package org.dromara.title.domain;

/** Business states deliberately kept independent from the workflow engine. */
public enum ApplicationStatus {
    DRAFT, PENDING_DEPARTMENT_REVIEW, PENDING_TECHNICAL_REVIEW,
    PENDING_DEPARTMENT_LEADER_REVIEW, PENDING_HR_REVIEW,
    CORRECTION_REQUIRED, EXPIRED_UNCORRECTED, REJECTED, APPROVED, ARCHIVED
}
