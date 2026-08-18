export type Id = string | number;

export interface TitleBatch {
  id: Id;
  name: string;
  evaluationYear: number;
  titleSeries: string;
  titleLevel: string;
  applicationType: string;
  openAt: string;
  firstSubmitDeadline: string;
  defaultCorrectionHours: number;
  ruleVersion: string;
  published: boolean;
}

export interface TitleApplication {
  id: Id;
  batchId: Id;
  applicantUserId: Id;
  status: string;
  currentVersion: number;
  formJson: string;
  correctionDeadline?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ApplicationFormData {
  values: Record<string, unknown>;
  materials: Array<{ fieldCode: string; ossIds: string; name?: string }>;
  declarationAccepted?: boolean;
}

export interface AuditEvent {
  id: Id;
  actorUserId: Id;
  actorRole: string;
  eventType: string;
  beforeStatus?: string;
  afterStatus?: string;
  versionNo: number;
  reason?: string;
  occurredAt: string;
}

export interface FieldDefinition {
  code: string;
  name: string;
  step: number;
  object: string;
  control: 'text' | 'textarea' | 'date' | 'number' | 'select' | 'upload' | 'readonly';
  format: string;
  required: boolean;
  requiredRule: string;
  source: string;
  editRule: string;
  visibility: string;
  mapping: string;
  scope: string;
  visible: boolean;
  readOnly: boolean;
  repeatable: boolean;
}

export interface PrecheckItem {
  fieldCode: string;
  fieldName: string;
  step: number;
  status: 'PASS' | 'BLOCK' | 'MANUAL';
  message: string;
  basis: string;
}

export interface PrecheckResult {
  passed: boolean;
  items: PrecheckItem[];
  passedCount: number;
  blockCount: number;
  manualCount: number;
}

export interface PolicyCitation {
  documentName: string;
  locator: string;
  excerpt: string;
  sourceUrl?: string;
}

export interface PolicyAnswer {
  answer: string;
  citations: PolicyCitation[];
  modelConfigured: boolean;
}
