package org.dromara.title.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.title.domain.TitleApplication;
import org.dromara.title.domain.TitleBatch;
import org.dromara.title.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.MockedStatic;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("dev")
class TitleEvaluationServiceTest {

    private final TitleBatchMapper batches = mock(TitleBatchMapper.class);
    private final TitleApplicationMapper applications = mock(TitleApplicationMapper.class);
    private final TitleVersionMapper versions = mock(TitleVersionMapper.class);
    private final TitleReviewMapper reviews = mock(TitleReviewMapper.class);
    private final TitleAuditMapper audits = mock(TitleAuditMapper.class);
    private final TitleFieldCatalogService fields = mock(TitleFieldCatalogService.class);
    private final TitleEvaluationService service = new TitleEvaluationService(
        batches, applications, versions, reviews, audits, new ObjectMapper(), fields);

    @Test
    void applicantCanReadOwnApplication() {
        TitleApplication application = application(100L, 9101L, "DRAFT");
        when(applications.selectById(100L)).thenReturn(application);

        try (MockedStatic<LoginHelper> login = loginAs(9101L, TitleEvaluationService.ROLE_APPLICANT)) {
            assertDoesNotThrow(() -> service.getAccessible(100L));
        }
    }

    @Test
    void applicantCannotReadAnotherApplicantsApplication() {
        TitleApplication application = application(100L, 9999L, "DRAFT");
        when(applications.selectById(100L)).thenReturn(application);

        try (MockedStatic<LoginHelper> login = loginAs(9101L, TitleEvaluationService.ROLE_APPLICANT)) {
            assertThrows(ServiceException.class, () -> service.getAccessible(100L));
        }
    }

    @Test
    void onlyReviewerForCurrentNodeCanReadApplication() {
        TitleApplication application = application(100L, 9101L, "PENDING_TECHNICAL_REVIEW");
        when(applications.selectById(100L)).thenReturn(application);

        try (MockedStatic<LoginHelper> login = loginAs(9103L, TitleEvaluationService.ROLE_TECHNICAL_REVIEWER)) {
            assertDoesNotThrow(() -> service.getAccessible(100L));
        }
        try (MockedStatic<LoginHelper> login = loginAs(9102L, TitleEvaluationService.ROLE_DEPARTMENT_REVIEWER)) {
            assertThrows(ServiceException.class, () -> service.getAccessible(100L));
        }
    }

    @Test
    void hrAdminCanPublishValidDraftBatch() {
        TitleBatch batch = new TitleBatch();
        batch.setId(9002L);
        batch.setName("测试批次");
        batch.setEvaluationYear(2026);
        batch.setTitleSeries("工程系列");
        batch.setTitleLevel("副高级");
        batch.setApplicationType("正常晋升");
        batch.setOpenAt(java.time.LocalDateTime.of(2026, 8, 1, 9, 0));
        batch.setFirstSubmitDeadline(java.time.LocalDateTime.of(2026, 12, 1, 18, 0));
        batch.setDefaultCorrectionHours(72);
        batch.setRuleVersion("MVP-1.0");
        batch.setPublished(false);
        when(batches.selectById(9002L)).thenReturn(batch);

        try (MockedStatic<LoginHelper> login = loginAs(9105L, TitleEvaluationService.ROLE_HR_ADMIN)) {
            service.publishBatch(9002L);
        }
        org.junit.jupiter.api.Assertions.assertTrue(batch.getPublished());
        verify(batches).updateById(batch);
    }

    @Test
    void nonHrRoleCannotCreateBatch() {
        try (MockedStatic<LoginHelper> login = loginAs(9101L, TitleEvaluationService.ROLE_APPLICANT)) {
            assertThrows(ServiceException.class, () -> service.createBatch(Map.of()));
        }
    }

    private static TitleApplication application(Long id, Long applicantId, String status) {
        TitleApplication application = new TitleApplication();
        application.setId(id);
        application.setApplicantUserId(applicantId);
        application.setStatus(status);
        application.setCurrentVersion(1);
        return application;
    }

    private static MockedStatic<LoginHelper> loginAs(Long userId, String role) {
        LoginUser loginUser = new LoginUser();
        loginUser.setRolePermission(Set.of(role));
        MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
        login.when(LoginHelper::getUserId).thenReturn(userId);
        login.when(LoginHelper::isSuperAdmin).thenReturn(false);
        login.when(LoginHelper::getLoginUser).thenReturn(loginUser);
        return login;
    }
}
