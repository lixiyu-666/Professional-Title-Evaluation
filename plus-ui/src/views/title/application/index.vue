<template>
  <div class="app-container application-page">
    <el-card shadow="never" class="page-head">
      <div>
        <p class="eyebrow">APPLICATION CENTER</p>
        <h2>我的职称申报</h2>
        <p>五步完成填报，系统会保存草稿并在提交前执行资格预检。</p>
      </div>
      <el-button v-if="availableBatch" type="primary" size="large" @click="createApplication">开始申报</el-button>
    </el-card>

    <el-row :gutter="18" class="workspace">
      <el-col :xs="24" :lg="6">
        <el-card shadow="never" class="record-list">
          <template #header><strong>申报记录</strong></template>
          <el-empty v-if="!applications.length" description="尚未创建申报" />
          <button
            v-for="item in applications"
            :key="String(item.id)"
            type="button"
            class="record-item"
            :class="{ active: String(item.id) === String(current?.id) }"
            @click="openApplication(item.id)"
          >
            <span>{{ batchName(item.batchId) }}</span>
            <small>{{ statusLabel[item.status] || item.status }} · V{{ item.currentVersion }}</small>
            <small v-if="item.correctionDeadline" class="danger">补正截止：{{ formatTime(item.correctionDeadline) }}</small>
          </button>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="18">
        <el-empty v-if="!current" description="请选择或创建一条申报" />
        <el-card v-else shadow="never" class="editor-card">
          <template #header>
            <div class="editor-title">
              <div>
                <strong>{{ batchName(current.batchId) }}</strong
                ><el-tag class="status-tag">{{ statusLabel[current.status] || current.status }}</el-tag>
              </div>
              <span>当前版本 V{{ current.currentVersion }}</span>
            </div>
          </template>

          <el-alert v-if="current.status === 'CORRECTION_REQUIRED'" type="warning" :closable="false" show-icon>
            <template #title>申报已退回补正，请在 {{ formatTime(current.correctionDeadline) }} 前完成修改并重新提交。</template>
          </el-alert>

          <el-steps :active="activeStep" finish-status="success" align-center class="steps">
            <el-step v-for="step in steps" :key="step.title" :title="step.title" :description="`${step.count} 个字段`" />
          </el-steps>

          <div class="step-panel">
            <div class="step-heading">
              <div>
                <h3>{{ steps[activeStep].title }}</h3>
                <p>{{ steps[activeStep].description }}</p>
              </div>
              <span class="save-state">{{ saveState }}</span>
            </div>

            <el-form label-position="top" :disabled="!editable">
              <template v-for="(objectFields, objectName) in fieldsByObject" :key="objectName">
                <el-divider content-position="left">{{ objectLabel(String(objectName)) }}</el-divider>
                <el-row :gutter="18">
                  <el-col
                    v-for="field in objectFields"
                    :key="field.code"
                    :xs="24"
                    :md="field.control === 'textarea' || field.control === 'upload' ? 24 : 12"
                  >
                    <el-form-item>
                      <template #label>
                        <span>{{ field.name }}</span
                        ><span v-if="field.required" class="required"> *</span>
                        <el-tooltip :content="field.requiredRule" placement="top"
                          ><el-icon class="help"><QuestionFilled /></el-icon
                        ></el-tooltip>
                      </template>

                      <el-input
                        v-if="field.control === 'text' && !field.repeatable"
                        v-model="form.values[field.code]"
                        :disabled="field.readOnly"
                        :placeholder="field.readOnly ? '由系统或员工库带入' : `请输入${field.name}`"
                      />
                      <el-input
                        v-else-if="field.control === 'textarea'"
                        v-model="form.values[field.code]"
                        type="textarea"
                        :rows="4"
                        :disabled="field.readOnly"
                      />
                      <el-input-number
                        v-else-if="field.control === 'number'"
                        v-model="form.values[field.code]"
                        :min="0"
                        :disabled="field.readOnly"
                        style="width: 100%"
                      />
                      <el-date-picker
                        v-else-if="field.control === 'date' && !field.repeatable"
                        v-model="form.values[field.code]"
                        type="date"
                        value-format="YYYY-MM-DD"
                        :disabled="field.readOnly"
                        style="width: 100%"
                      />
                      <el-select
                        v-else-if="field.repeatable"
                        v-model="form.values[field.code]"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        :disabled="field.readOnly"
                        placeholder="可录入多条，回车确认"
                        style="width: 100%"
                      />
                      <el-select
                        v-else-if="field.control === 'select'"
                        v-model="form.values[field.code]"
                        filterable
                        allow-create
                        :disabled="field.readOnly"
                        style="width: 100%"
                      >
                        <el-option v-for="option in optionsFor(field)" :key="option" :label="option" :value="option" />
                      </el-select>
                      <FileUpload v-else-if="field.control === 'upload'" v-model="form.values[field.code]" :limit="5" />
                      <el-input v-else v-model="form.values[field.code]" :disabled="field.readOnly" />
                      <div class="field-code">{{ field.code }} · {{ field.source }}</div>
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>

              <div v-if="activeStep === 4" class="submit-panel">
                <el-checkbox v-model="form.declarationAccepted" size="large"
                  >本人承诺所填信息及提交材料真实、完整，并同意按流程接受审核。</el-checkbox
                >
                <div v-if="precheck" class="precheck-summary">
                  <el-tag type="success">通过 {{ precheck.passedCount }}</el-tag>
                  <el-tag type="danger">阻断 {{ precheck.blockCount }}</el-tag>
                  <el-tag type="warning">人工核验 {{ precheck.manualCount }}</el-tag>
                </div>
                <el-table v-if="precheck?.items.length" :data="precheck.items.filter((item) => item.status !== 'PASS')" size="small" max-height="260">
                  <el-table-column prop="fieldName" label="字段" min-width="160" />
                  <el-table-column prop="status" label="结果" width="100" />
                  <el-table-column prop="message" label="说明" min-width="220" />
                </el-table>
              </div>
            </el-form>
          </div>

          <div class="editor-actions">
            <el-button :disabled="activeStep === 0" @click="changeStep(-1)">上一步</el-button>
            <div>
              <el-button v-if="editable" :loading="saving" @click="saveDraft()">保存草稿</el-button>
              <el-button v-if="editable && activeStep === 4" @click="runPrecheck">资格预检</el-button>
              <el-button v-if="editable && activeStep === 4" type="primary" :disabled="!form.declarationAccepted" @click="submitApplication"
                >确认提交</el-button
              >
              <el-button v-if="activeStep < 4" type="primary" @click="changeStep(1)">下一步</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { QuestionFilled } from '@element-plus/icons-vue';
import {
  createTitleApplication,
  getTitleApplication,
  listMyApplications,
  listTitleBatches,
  precheckTitleApplication,
  saveTitleDraft,
  submitTitleApplication
} from '@/api/title';
import type { FieldDefinition, Id, PrecheckResult, TitleApplication, TitleBatch } from '@/api/title/types';
import rawDefinitions from './field-definitions.json';
import { editableStatuses, parseApplicationForm, statusLabel } from '../shared';

const definitions = rawDefinitions as FieldDefinition[];
const batches = ref<TitleBatch[]>([]);
const applications = ref<TitleApplication[]>([]);
const current = ref<TitleApplication>();
const activeStep = ref(0);
const saving = ref(false);
const saveState = ref('');
const hydrated = ref(false);
const precheck = ref<PrecheckResult>();
const form = reactive({
  values: {} as Record<string, any>,
  materials: [] as Array<{ fieldCode: string; ossIds: string; name?: string }>,
  declarationAccepted: false
});
let saveTimer: ReturnType<typeof setTimeout> | undefined;

const stepDescriptions = [
  '个人身份与申报基础信息',
  '学历、资历、培训及任职经历',
  '代表项目、任现职前后业绩与成果',
  '同行推荐与申报条件对照',
  '材料检查、承诺声明与提交'
];
const steps = computed(() =>
  [1, 2, 3, 4, 5].map((number, index) => ({
    title: ['基本信息', '学历资历与经历', '业绩与成果', '推荐与条件对照', '材料确认与提交'][index],
    description: stepDescriptions[index],
    count: definitions.filter((field) => field.visible && field.step === number).length
  }))
);
const editable = computed(() => Boolean(current.value && editableStatuses.has(current.value.status)));
const availableBatch = computed(() => batches.value.find((batch) => !applications.value.some((item) => String(item.batchId) === String(batch.id))));
const currentFields = computed(() => definitions.filter((field) => field.visible && field.step === activeStep.value + 1));
const fieldsByObject = computed(() =>
  currentFields.value.reduce<Record<string, FieldDefinition[]>>((groups, field) => {
    const key = field.object || 'application';
    (groups[key] ||= []).push(field);
    return groups;
  }, {})
);

const batchName = (batchId: Id) => batches.value.find((batch) => String(batch.id) === String(batchId))?.name || `批次 ${batchId}`;
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-');
const objectLabel = (name: string) =>
  ({
    application: '申报信息',
    review_form: '任职资格评审表',
    project_achievement: '代表性项目',
    peer_recommendation: '同行专家推荐',
    study_experience: '学习培训经历',
    pre_title_achievement: '任现职前业绩',
    exam_record: '考试与答辩',
    education_record: '学历信息',
    material: '申报材料'
  })[name] || name;
const optionsFor = (field: FieldDefinition) => {
  const match = field.editRule.match(/选项：([^/]+)/);
  return match
    ? match[1]
        .split(/[、/]/)
        .map((value) => value.trim())
        .filter(Boolean)
    : [];
};

const hydrate = (application: TitleApplication) => {
  hydrated.value = false;
  current.value = application;
  const data = parseApplicationForm(application);
  form.values = data.values;
  form.materials = data.materials;
  form.declarationAccepted = Boolean(data.declarationAccepted);
  precheck.value = undefined;
  queueMicrotask(() => {
    hydrated.value = true;
  });
};

const refresh = async () => {
  applications.value = (await listMyApplications()).data;
  if (current.value) {
    const refreshed = applications.value.find((item) => String(item.id) === String(current.value?.id));
    if (refreshed) current.value = refreshed;
  }
};

const openApplication = async (id: Id) => hydrate((await getTitleApplication(id)).data);
const createApplication = async () => {
  if (!availableBatch.value) return;
  const application = (await createTitleApplication(availableBatch.value.id)).data;
  await refresh();
  hydrate(application);
  ElMessage.success('草稿创建成功');
};

const syncMaterials = () => {
  form.materials = definitions
    .filter((field) => field.control === 'upload' && form.values[field.code])
    .map((field) => ({ fieldCode: field.code, ossIds: String(form.values[field.code]), name: field.name }));
};
const saveDraft = async (silent = false) => {
  if (!current.value || !editable.value) return;
  saving.value = true;
  syncMaterials();
  try {
    const saved = (
      await saveTitleDraft(current.value.id, { values: form.values, materials: form.materials, declarationAccepted: form.declarationAccepted })
    ).data;
    current.value = saved;
    saveState.value = `已保存 ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`;
    if (!silent) ElMessage.success('草稿已保存');
  } finally {
    saving.value = false;
  }
};
const changeStep = async (offset: number) => {
  if (editable.value) await saveDraft(true);
  activeStep.value = Math.min(4, Math.max(0, activeStep.value + offset));
};
const runPrecheck = async () => {
  await saveDraft(true);
  precheck.value = (await precheckTitleApplication(current.value!.id)).data;
  precheck.value.passed ? ElMessage.success('预检完成，没有阻断项') : ElMessage.warning(`发现 ${precheck.value.blockCount} 个阻断项`);
};
const submitApplication = async () => {
  await runPrecheck();
  if (!precheck.value?.passed) return;
  await ElMessageBox.confirm('提交后当前版本将锁定并进入部门人事初审，是否继续？', '确认提交', { type: 'warning' });
  hydrate((await submitTitleApplication(current.value!.id)).data);
  await refresh();
  ElMessage.success('提交成功，已进入部门人事初审');
};

watch(
  () => form.values,
  () => {
    if (!hydrated.value || !editable.value) return;
    saveState.value = '有未保存修改';
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => saveDraft(true), 1500);
  },
  { deep: true }
);
onBeforeUnmount(() => clearTimeout(saveTimer));

onMounted(async () => {
  if (definitions.length !== 149 || new Set(definitions.map((field) => field.code)).size !== 149) throw new Error('字段配置必须包含149个唯一字段');
  batches.value = (await listTitleBatches()).data;
  await refresh();
  if (applications.value[0]) await openApplication(applications.value[0].id);
});
</script>

<style scoped lang="scss">
.application-page {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}
.page-head {
  border: 0;
  border-radius: 16px;
}
.page-head :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.page-head h2 {
  margin: 4px 0 8px;
}
.page-head p {
  margin: 0;
  color: #64748b;
}
.eyebrow {
  color: #0f766e !important;
  letter-spacing: 1.8px;
  font-size: 11px;
}
.workspace {
  margin-top: 18px;
}
.record-list,
.editor-card {
  border: 0;
  border-radius: 16px;
}
.record-item {
  width: 100%;
  margin-bottom: 10px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: white;
  text-align: left;
  cursor: pointer;
}
.record-item span,
.record-item small {
  display: block;
}
.record-item small {
  margin-top: 6px;
  color: #64748b;
}
.record-item.active {
  border-color: #0f766e;
  background: #effaf8;
}
.danger {
  color: #b45309 !important;
}
.editor-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.status-tag {
  margin-left: 10px;
}
.steps {
  margin: 26px 0 30px;
}
.step-panel {
  min-height: 460px;
}
.step-heading {
  display: flex;
  justify-content: space-between;
  align-items: start;
}
.step-heading h3 {
  margin: 0 0 6px;
}
.step-heading p,
.save-state {
  color: #64748b;
}
.required {
  color: #dc2626;
}
.help {
  margin-left: 5px;
  color: #94a3b8;
}
.field-code {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.35;
}
.submit-panel {
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
}
.precheck-summary {
  display: flex;
  gap: 8px;
  margin: 14px 0;
}
.editor-actions {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}
@media (max-width: 1200px) {
  .record-list {
    margin-bottom: 18px;
  }
}
@media (max-width: 768px) {
  .page-head :deep(.el-card__body),
  .editor-title,
  .editor-actions {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .steps {
    overflow-x: auto;
  }
}
</style>
