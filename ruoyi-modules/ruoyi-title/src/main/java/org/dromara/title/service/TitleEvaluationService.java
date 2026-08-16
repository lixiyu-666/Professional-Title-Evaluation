package org.dromara.title.service;

import org.dromara.title.domain.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MVP domain facade. Persistence adapters map these aggregates to the title_* tables
 * supplied in script/sql/title_evaluation_mvp.sql; keeping the rules here makes a
 * later WarmFlow adapter or repository implementation replaceable.
 */
@Service
public class TitleEvaluationService {
    private final AtomicLong ids = new AtomicLong(1000);
    private final Map<Long, Application> applications = new ConcurrentHashMap<>();
    private final List<AuditEvent> audits = Collections.synchronizedList(new ArrayList<>());

    public Application createDraft(Long applicantId, Long batchId, Map<String, Object> form) {
        if (applications.values().stream().anyMatch(a -> a.applicantId.equals(applicantId) && a.batchId.equals(batchId) && !Set.of(ApplicationStatus.REJECTED, ApplicationStatus.ARCHIVED).contains(a.status))) {
            throw new IllegalStateException("同一人员在同一批次只能有一条有效申报");
        }
        Application application = new Application(ids.incrementAndGet(), applicantId, batchId, 0, ApplicationStatus.DRAFT, new LinkedHashMap<>(form), LocalDateTime.now());
        applications.put(application.id, application);
        audit("CREATE_DRAFT", application, null, ApplicationStatus.DRAFT, "创建草稿");
        return application.copy();
    }

    public Application saveDraft(Long id, Map<String, Object> form) {
        Application a = required(id); require(a, ApplicationStatus.DRAFT, ApplicationStatus.CORRECTION_REQUIRED);
        a.form.putAll(form); audit("SAVE_DRAFT", a, a.status, a.status, "保存草稿"); return a.copy();
    }

    public Application submit(Long id) {
        Application a = required(id); require(a, ApplicationStatus.DRAFT, ApplicationStatus.CORRECTION_REQUIRED);
        if (a.form.isEmpty()) throw new IllegalStateException("申报表不能为空");
        ApplicationStatus before = a.status; a.version++; a.status = ApplicationStatus.PENDING_DEPARTMENT_REVIEW;
        audit("SUBMIT_V" + a.version, a, before, a.status, "生成不可变提交快照"); return a.copy();
    }

    public Application review(Long id, String action, Long reviewerId, String reason, Long correctionHours) {
        Application a = required(id); if (a.applicantId.equals(reviewerId)) throw new IllegalStateException("申请人不得审核本人申报");
        ApplicationStatus before = a.status;
        if ("approve".equals(action)) a.status = next(a.status);
        else if ("return".equals(action)) { a.status = ApplicationStatus.CORRECTION_REQUIRED; a.correctionDeadline = LocalDateTime.now().plusHours(correctionHours == null ? 72 : correctionHours); }
        else if ("reject".equals(action)) { if (reason == null || reason.isBlank()) throw new IllegalStateException("驳回必须填写政策依据与具体原因"); a.status = ApplicationStatus.REJECTED; }
        else throw new IllegalArgumentException("未知审核动作");
        audit("REVIEW_" + action.toUpperCase(), a, before, a.status, reason); return a.copy();
    }

    public List<AuditEvent> auditEvents(Long applicationId) { return audits.stream().filter(e -> e.applicationId.equals(applicationId)).toList(); }
    public Application get(Long id) { return required(id).copy(); }
    private Application required(Long id) { Application a = applications.get(id); if (a == null) throw new NoSuchElementException("申报不存在"); return a; }
    private static void require(Application a, ApplicationStatus... allowed) { if (!Set.of(allowed).contains(a.status)) throw new IllegalStateException("当前状态不允许此操作: " + a.status); }
    private static ApplicationStatus next(ApplicationStatus current) { return switch (current) { case PENDING_DEPARTMENT_REVIEW -> ApplicationStatus.PENDING_TECHNICAL_REVIEW; case PENDING_TECHNICAL_REVIEW -> ApplicationStatus.PENDING_DEPARTMENT_LEADER_REVIEW; case PENDING_DEPARTMENT_LEADER_REVIEW -> ApplicationStatus.PENDING_HR_REVIEW; case PENDING_HR_REVIEW -> ApplicationStatus.APPROVED; default -> throw new IllegalStateException("当前状态不能通过"); }; }
    private void audit(String event, Application a, ApplicationStatus before, ApplicationStatus after, String reason) { audits.add(new AuditEvent(event, a.id, a.version, before, after, reason, LocalDateTime.now())); }

    public static final class Application { public final Long id, applicantId, batchId; public int version; public ApplicationStatus status; public final Map<String,Object> form; public final LocalDateTime createdAt; public LocalDateTime correctionDeadline; Application(Long id, Long applicantId, Long batchId, int version, ApplicationStatus status, Map<String,Object> form, LocalDateTime createdAt) { this.id=id;this.applicantId=applicantId;this.batchId=batchId;this.version=version;this.status=status;this.form=form;this.createdAt=createdAt; } Application copy(){ Application c=new Application(id,applicantId,batchId,version,status,new LinkedHashMap<>(form),createdAt);c.correctionDeadline=correctionDeadline;return c;} }
    public record AuditEvent(String event, Long applicationId, int version, ApplicationStatus beforeStatus, ApplicationStatus afterStatus, String reason, LocalDateTime occurredAt) { }
}
