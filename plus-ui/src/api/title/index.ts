import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type { ApplicationFormData, AuditEvent, FieldDefinition, Id, PolicyAnswer, PrecheckResult, TitleApplication, TitleBatch } from './types';

export const listTitleBatches = (): AxiosPromise<TitleBatch[]> => request({ url: '/title/batches', method: 'get' });
export const getTitleBatch = (id: Id): AxiosPromise<TitleBatch> => request({ url: `/title/batches/${id}`, method: 'get' });
export const listMyApplications = (): AxiosPromise<TitleApplication[]> => request({ url: '/title/applications/mine', method: 'get' });
export const listReviewTasks = (): AxiosPromise<TitleApplication[]> => request({ url: '/title/applications/review-tasks', method: 'get' });
export const getTitleApplication = (id: Id): AxiosPromise<TitleApplication> => request({ url: `/title/applications/${id}`, method: 'get' });
export const createTitleApplication = (batchId: Id): AxiosPromise<TitleApplication> =>
  request({ url: '/title/applications', method: 'post', data: { batchId, values: {}, materials: [] } });
export const saveTitleDraft = (id: Id, data: ApplicationFormData): AxiosPromise<TitleApplication> =>
  request({ url: `/title/applications/${id}/draft`, method: 'put', data });
export const precheckTitleApplication = (id: Id): AxiosPromise<PrecheckResult> =>
  request({ url: `/title/applications/${id}/precheck`, method: 'post' });
export const submitTitleApplication = (id: Id): AxiosPromise<TitleApplication> =>
  request({ url: `/title/applications/${id}/submit`, method: 'post' });
export const reviewTitleApplication = (id: Id, data: { action: string; reason?: string; correctionHours?: number }): AxiosPromise<TitleApplication> =>
  request({ url: `/title/applications/${id}/review`, method: 'post', data });
export const listAuditEvents = (id: Id): AxiosPromise<AuditEvent[]> => request({ url: `/title/applications/${id}/audit-events`, method: 'get' });
export const listFieldDefinitions = (): AxiosPromise<FieldDefinition[]> => request({ url: '/title/applications/field-definitions', method: 'get' });
export const askPolicy = (question: string, context: Record<string, string>): AxiosPromise<PolicyAnswer> =>
  request({ url: '/title/policies/ask', method: 'post', data: { question, context } });
