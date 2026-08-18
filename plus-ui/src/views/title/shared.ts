import type { ApplicationFormData, TitleApplication } from '@/api/title/types';

export const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_DEPARTMENT_REVIEW: '待部门初审',
  PENDING_TECHNICAL_REVIEW: '待技术审核',
  PENDING_DEPARTMENT_LEADER_REVIEW: '待部门领导审核',
  PENDING_HR_REVIEW: '待人事终审',
  CORRECTION_REQUIRED: '待补正',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  OVERDUE_CORRECTION: '逾期未补正'
};

export const editableStatuses = new Set(['DRAFT', 'CORRECTION_REQUIRED']);

export function parseApplicationForm(application?: TitleApplication): ApplicationFormData {
  if (!application?.formJson) return { values: {}, materials: [], declarationAccepted: false };
  try {
    const parsed = JSON.parse(application.formJson);
    return {
      values: parsed.values || {},
      materials: parsed.materials || [],
      declarationAccepted: Boolean(parsed.declarationAccepted)
    };
  } catch {
    return { values: {}, materials: [], declarationAccepted: false };
  }
}
