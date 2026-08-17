package org.dromara.title.service;

import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.title.domain.*;
import org.dromara.title.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TitleEvaluationService {
    public static final String ROLE_APPLICANT = "title_applicant";
    public static final String ROLE_DEPARTMENT_REVIEWER = "title_dept_reviewer";
    public static final String ROLE_TECHNICAL_REVIEWER = "title_technical_reviewer";
    public static final String ROLE_DEPARTMENT_LEADER = "title_dept_leader";
    public static final String ROLE_HR_ADMIN = "title_hr_admin";

    private static final Map<String, String> REVIEW_ROLE_BY_STATUS = Map.of(
        "PENDING_DEPARTMENT_REVIEW", ROLE_DEPARTMENT_REVIEWER,
        "PENDING_TECHNICAL_REVIEW", ROLE_TECHNICAL_REVIEWER,
        "PENDING_DEPARTMENT_LEADER_REVIEW", ROLE_DEPARTMENT_LEADER,
        "PENDING_HR_REVIEW", ROLE_HR_ADMIN
    );

    private final TitleBatchMapper batches;
    private final TitleApplicationMapper applications;
    private final TitleVersionMapper versions;
    private final TitleReviewMapper reviews;
    private final TitleAuditMapper audits;
    private final ObjectMapper json;

    public List<TitleBatch> listPublishedBatches() {
        actor();
        return batches.selectList(Wrappers.<TitleBatch>lambdaQuery()
            .eq(TitleBatch::getPublished, true)
            .orderByDesc(TitleBatch::getEvaluationYear)
            .orderByDesc(TitleBatch::getOpenAt));
    }

    public TitleBatch getPublishedBatch(Long id) {
        actor();
        TitleBatch batch = batches.selectOne(Wrappers.<TitleBatch>lambdaQuery()
            .eq(TitleBatch::getId, id)
            .eq(TitleBatch::getPublished, true));
        if (batch == null) {
            throw new NoSuchElementException("批次不存在或未发布");
        }
        return batch;
    }

    public List<TitleApplication> listMine() {
        Long userId = actor();
        return applications.selectList(Wrappers.<TitleApplication>lambdaQuery()
            .eq(TitleApplication::getApplicantUserId, userId)
            .orderByDesc(TitleApplication::getCreatedAt));
    }

    public List<TitleApplication> listReviewTasks() {
        actor();
        Collection<String> statuses;
        if (LoginHelper.isSuperAdmin()) {
            statuses = REVIEW_ROLE_BY_STATUS.keySet();
        } else {
            Set<String> roleSet = roles();
            statuses = REVIEW_ROLE_BY_STATUS.entrySet().stream()
                .filter(entry -> roleSet.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        }
        if (statuses.isEmpty()) {
            return List.of();
        }
        return applications.selectList(Wrappers.<TitleApplication>lambdaQuery()
            .in(TitleApplication::getStatus, statuses)
            .orderByAsc(TitleApplication::getCreatedAt));
    }

    @Transactional
    public TitleApplication createDraft(Long batchId, Map<String, Object> form) {
        Long userId = actor();
        TitleBatch batch = getPublishedBatch(batchId);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(batch.getOpenAt()) || now.isAfter(batch.getFirstSubmitDeadline())) {
            throw new IllegalStateException("当前不在批次申报开放期内");
        }
        if (applications.exists(Wrappers.<TitleApplication>lambdaQuery()
            .eq(TitleApplication::getBatchId, batchId)
            .eq(TitleApplication::getApplicantUserId, userId))) {
            throw new IllegalStateException("同一人员同一批次只允许一条申报");
        }
        TitleApplication application = new TitleApplication();
        application.setBatchId(batchId);
        application.setApplicantUserId(userId);
        application.setStatus("DRAFT");
        application.setCurrentVersion(0);
        application.setFormJson(write(form));
        application.setCreatedAt(now);
        applications.insert(application);
        audit(application, "CREATE_DRAFT", null, "DRAFT", "创建草稿");
        return application;
    }

    @Transactional
    public TitleApplication saveDraft(Long id, Map<String, Object> form) {
        TitleApplication application = getOwned(id);
        allow(application, "DRAFT", "CORRECTION_REQUIRED");
        Map<String, Object> merged = read(application.getFormJson());
        merged.putAll(form);
        application.setFormJson(write(merged));
        applications.updateById(application);
        audit(application, "SAVE_DRAFT", application.getStatus(), application.getStatus(), "保存草稿");
        return application;
    }

    @Transactional
    public TitleApplication submit(Long id) {
        TitleApplication application = getOwned(id);
        allow(application, "DRAFT", "CORRECTION_REQUIRED");
        String before = application.getStatus();
        application.setCurrentVersion(application.getCurrentVersion() + 1);
        application.setStatus("PENDING_DEPARTMENT_REVIEW");
        application.setCorrectionDeadline(null);

        TitleApplicationVersion version = new TitleApplicationVersion();
        version.setApplicationId(id);
        version.setVersionNo(application.getCurrentVersion());
        version.setFormSnapshot(application.getFormJson());
        version.setMaterialSnapshot("[]");
        version.setPrecheckSnapshot("{}");
        version.setSubmittedAt(LocalDateTime.now());
        versions.insert(version);
        applications.updateById(application);
        audit(application, "SUBMIT_V" + application.getCurrentVersion(), before, application.getStatus(), "提交版本快照");
        return application;
    }

    @Transactional
    public TitleApplication review(Long id, String action, String reason, Long correctionHours) {
        TitleApplication application = getRaw(id);
        Long userId = actor();
        if (application.getApplicantUserId().equals(userId)) {
            throw new ServiceException("申请人不得审核本人申报", HttpStatus.HTTP_FORBIDDEN);
        }
        requireCurrentReviewer(application);
        String before = application.getStatus();
        String normalizedAction = action == null ? "" : action.toLowerCase(Locale.ROOT);
        if ("approve".equals(normalizedAction)) {
            application.setStatus(next(before));
        } else if ("return".equals(normalizedAction)) {
            application.setStatus("CORRECTION_REQUIRED");
            application.setCorrectionDeadline(LocalDateTime.now().plusHours(correctionHours == null ? 72 : correctionHours));
        } else if ("reject".equals(normalizedAction)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalStateException("驳回必须填写依据");
            }
            application.setStatus("REJECTED");
        } else {
            throw new IllegalArgumentException("未知审核动作");
        }

        TitleReviewRecord record = new TitleReviewRecord();
        record.setApplicationId(id);
        record.setVersionNo(application.getCurrentVersion());
        record.setReviewerUserId(userId);
        record.setReviewNode(before);
        record.setAction(normalizedAction);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        reviews.insert(record);
        applications.updateById(application);
        audit(application, "REVIEW_" + normalizedAction.toUpperCase(Locale.ROOT), before, application.getStatus(), reason);
        return application;
    }

    public TitleApplication getAccessible(Long id) {
        TitleApplication application = getRaw(id);
        requireCanAccess(application);
        return application;
    }

    public List<TitleAuditEvent> auditEvents(Long id) {
        TitleApplication application = getRaw(id);
        requireCanAccess(application);
        return audits.selectList(Wrappers.<TitleAuditEvent>lambdaQuery()
            .eq(TitleAuditEvent::getApplicationId, id)
            .orderByAsc(TitleAuditEvent::getOccurredAt));
    }

    private TitleApplication getOwned(Long id) {
        TitleApplication application = getRaw(id);
        if (!application.getApplicantUserId().equals(actor())) {
            throw new ServiceException("只能操作本人申报", HttpStatus.HTTP_FORBIDDEN);
        }
        return application;
    }

    private TitleApplication getRaw(Long id) {
        TitleApplication application = applications.selectById(id);
        if (application == null) {
            throw new NoSuchElementException("申报不存在");
        }
        return application;
    }

    private void requireCanAccess(TitleApplication application) {
        Long userId = actor();
        if (LoginHelper.isSuperAdmin() || application.getApplicantUserId().equals(userId)) {
            return;
        }
        String requiredRole = REVIEW_ROLE_BY_STATUS.get(application.getStatus());
        if (requiredRole != null && roles().contains(requiredRole)) {
            return;
        }
        if (("APPROVED".equals(application.getStatus()) || "REJECTED".equals(application.getStatus()))
            && roles().contains(ROLE_HR_ADMIN)) {
            return;
        }
        throw new ServiceException("无权查看该申报", HttpStatus.HTTP_FORBIDDEN);
    }

    private void requireCurrentReviewer(TitleApplication application) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        String requiredRole = REVIEW_ROLE_BY_STATUS.get(application.getStatus());
        if (requiredRole == null) {
            throw new IllegalStateException("当前状态不是审核节点");
        }
        if (!roles().contains(requiredRole)) {
            throw new ServiceException("当前节点不属于该审核角色", HttpStatus.HTTP_FORBIDDEN);
        }
    }

    private Set<String> roles() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getRolePermission() == null) {
            return Set.of();
        }
        return loginUser.getRolePermission();
    }

    private void audit(TitleApplication application, String event, String before, String after, String reason) {
        TitleAuditEvent auditEvent = new TitleAuditEvent();
        auditEvent.setApplicationId(application.getId());
        auditEvent.setActorUserId(actor());
        auditEvent.setActorRole(String.join(",", roles()));
        auditEvent.setEventType(event);
        auditEvent.setBeforeStatus(before);
        auditEvent.setAfterStatus(after);
        auditEvent.setVersionNo(application.getCurrentVersion());
        auditEvent.setReason(reason);
        auditEvent.setOccurredAt(LocalDateTime.now());
        audits.insert(auditEvent);
    }

    private Long actor() {
        Long id = LoginHelper.getUserId();
        if (id == null) {
            throw new IllegalStateException("请先登录");
        }
        return id;
    }

    private static void allow(TitleApplication application, String... statuses) {
        if (!List.of(statuses).contains(application.getStatus())) {
            throw new IllegalStateException("当前状态不允许操作");
        }
    }

    private static String next(String status) {
        return switch (status) {
            case "PENDING_DEPARTMENT_REVIEW" -> "PENDING_TECHNICAL_REVIEW";
            case "PENDING_TECHNICAL_REVIEW" -> "PENDING_DEPARTMENT_LEADER_REVIEW";
            case "PENDING_DEPARTMENT_LEADER_REVIEW" -> "PENDING_HR_REVIEW";
            case "PENDING_HR_REVIEW" -> "APPROVED";
            default -> throw new IllegalStateException("当前状态不能通过");
        };
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("申报JSON解析失败", exception);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON序列化失败", exception);
        }
    }
}
