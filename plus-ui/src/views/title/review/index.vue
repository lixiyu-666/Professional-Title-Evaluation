<template>
  <div class="app-container review-page">
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div>
          <p class="eyebrow">UNIFIED REVIEW DESK</p>
          <h2>统一审核工作台</h2>
        </div>
        <el-input v-model="keyword" clearable placeholder="按申请人ID或版本筛选" style="width: 260px" />
      </div>
    </el-card>

    <el-row :gutter="18" class="workspace">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="task-card">
          <template #header
            ><div class="card-head">
              <strong>我的待办</strong><el-tag>{{ filteredTasks.length }}</el-tag>
            </div></template
          >
          <el-empty v-if="!filteredTasks.length" description="当前没有待审核任务" />
          <button
            v-for="task in filteredTasks"
            :key="String(task.id)"
            class="task-item"
            :class="{ active: String(task.id) === String(current?.id) }"
            @click="openTask(task.id)"
          >
            <div>
              <strong>申报人 {{ task.applicantUserId }}</strong
              ><el-tag size="small" type="warning">V{{ task.currentVersion }}</el-tag>
            </div>
            <p>{{ batchName(task.batchId) }}</p>
            <small>{{ statusLabel[task.status] || task.status }}</small>
          </button>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-empty v-if="!current" description="请选择一条审核待办" />
        <el-card v-else shadow="never" class="detail-card">
          <template #header>
            <div class="card-head">
              <div>
                <strong>申报详情</strong><el-tag class="status">{{ statusLabel[current.status] }}</el-tag>
              </div>
              <span>V{{ current.currentVersion }}</span>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="申请人ID">{{ current.applicantUserId }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ batchName(current.batchId) }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ statusLabel[current.status] }}</el-descriptions-item>
            <el-descriptions-item label="提交版本">V{{ current.currentVersion }}</el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="申报内容" name="form">
              <el-collapse accordion>
                <el-collapse-item v-for="step in [1, 2, 3, 4, 5]" :key="step" :name="step" :title="`${step}. ${stepNames[step - 1]}`">
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item v-for="field in populatedFields(step)" :key="field.code" :label="field.name">
                      {{ displayValue(form.values[field.code]) }}
                    </el-descriptions-item>
                  </el-descriptions>
                  <el-empty v-if="!populatedFields(step).length" description="本步骤尚未填写" :image-size="60" />
                </el-collapse-item>
              </el-collapse>
            </el-tab-pane>
            <el-tab-pane label="材料" name="materials">
              <el-table :data="form.materials">
                <el-table-column prop="name" label="材料名称" />
                <el-table-column prop="fieldCode" label="字段编码" />
                <el-table-column prop="ossIds" label="文件ID" />
              </el-table>
              <el-empty v-if="!form.materials.length" description="未上传材料" />
            </el-tab-pane>
            <el-tab-pane label="审计时间线" name="audit">
              <el-timeline>
                <el-timeline-item v-for="event in audits" :key="String(event.id)" :timestamp="formatTime(event.occurredAt)" placement="top">
                  <strong>{{ eventLabel(event.eventType) }}</strong>
                  <p>
                    {{ statusLabel[event.beforeStatus || ''] || event.beforeStatus || '开始' }} →
                    {{ statusLabel[event.afterStatus || ''] || event.afterStatus }}
                  </p>
                  <p v-if="event.reason">{{ event.reason }}</p>
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>
          </el-tabs>

          <div class="review-actions">
            <el-button type="success" @click="openAction('approve')">通过</el-button>
            <el-button type="warning" @click="openAction('return')">退回补正</el-button>
            <el-button type="danger" plain @click="openAction('reject')">驳回</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialog.visible" :title="actionTitle" width="520px" append-to-body>
      <el-form label-position="top">
        <el-form-item v-if="dialog.action !== 'approve'" :label="dialog.action === 'reject' ? '政策依据与具体原因' : '退回原因'" required>
          <el-input v-model="dialog.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item v-if="dialog.action === 'return'" label="补正时长（小时）" required>
          <el-input-number v-model="dialog.correctionHours" :min="1" :max="720" />
        </el-form-item>
        <el-alert
          v-if="dialog.action === 'reject'"
          title="驳回将终止本批次流程，提交前请再次核对政策依据。"
          type="error"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer
        ><el-button @click="dialog.visible = false">取消</el-button
        ><el-button type="primary" :loading="submitting" @click="submitReview">确认</el-button></template
      >
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { getTitleApplication, listAuditEvents, listReviewTasks, listTitleBatches, reviewTitleApplication } from '@/api/title';
import type { AuditEvent, FieldDefinition, Id, TitleApplication, TitleBatch } from '@/api/title/types';
import rawDefinitions from '../application/field-definitions.json';
import { parseApplicationForm, statusLabel } from '../shared';

const definitions = rawDefinitions as FieldDefinition[];
const stepNames = ['基本信息', '学历资历与经历', '业绩与成果', '推荐与条件对照', '材料确认与提交'];
const batches = ref<TitleBatch[]>([]);
const tasks = ref<TitleApplication[]>([]);
const current = ref<TitleApplication>();
const audits = ref<AuditEvent[]>([]);
const form = reactive({ values: {} as Record<string, unknown>, materials: [] as Array<{ fieldCode: string; ossIds: string; name?: string }> });
const keyword = ref('');
const activeTab = ref('form');
const submitting = ref(false);
const dialog = reactive({ visible: false, action: 'approve', reason: '', correctionHours: 72 });

const filteredTasks = computed(() =>
  tasks.value.filter((task) => !keyword.value || `${task.applicantUserId} ${task.currentVersion}`.includes(keyword.value))
);
const actionTitle = computed(() => ({ approve: '审核通过', return: '退回补正', reject: '驳回申报' })[dialog.action]);
const batchName = (id: Id) => batches.value.find((batch) => String(batch.id) === String(id))?.name || `批次 ${id}`;
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-');
const displayValue = (value: unknown) =>
  Array.isArray(value) ? value.join('；') : value === undefined || value === null || value === '' ? '-' : String(value);
const populatedFields = (step: number) =>
  definitions.filter((field) => field.visible && field.step === step && displayValue(form.values[field.code]) !== '-');
const eventLabel = (type: string) =>
  type
    .replace('CREATE_DRAFT', '创建草稿')
    .replace('SAVE_DRAFT', '保存草稿')
    .replace('SUBMIT_V', '提交版本 V')
    .replace('REVIEW_APPROVE', '审核通过')
    .replace('REVIEW_RETURN', '退回补正')
    .replace('REVIEW_REJECT', '驳回');

const loadTasks = async () => {
  tasks.value = (await listReviewTasks()).data;
};
const openTask = async (id: Id) => {
  const application = (await getTitleApplication(id)).data;
  current.value = application;
  const data = parseApplicationForm(application);
  form.values = data.values;
  form.materials = data.materials;
  audits.value = (await listAuditEvents(id)).data;
};
const openAction = (action: string) => {
  dialog.action = action;
  dialog.reason = '';
  dialog.correctionHours = 72;
  dialog.visible = true;
};
const submitReview = async () => {
  if (!current.value) return;
  if (dialog.action !== 'approve' && !dialog.reason.trim()) return ElMessage.warning('请填写审核原因');
  if (dialog.action === 'reject') await ElMessageBox.confirm('驳回后流程将终止，确认继续？', '二次确认', { type: 'error' });
  submitting.value = true;
  try {
    await reviewTitleApplication(current.value.id, {
      action: dialog.action,
      reason: dialog.reason || undefined,
      correctionHours: dialog.action === 'return' ? dialog.correctionHours : undefined
    });
    dialog.visible = false;
    current.value = undefined;
    audits.value = [];
    await loadTasks();
    ElMessage.success('审核操作已完成');
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  batches.value = (await listTitleBatches()).data;
  await loadTasks();
  if (tasks.value[0]) await openTask(tasks.value[0].id);
});
</script>

<style scoped lang="scss">
.review-page {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}
.filter-card,
.task-card,
.detail-card {
  border: 0;
  border-radius: 16px;
}
.filter-row,
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}
.filter-row h2 {
  margin: 4px 0;
}
.eyebrow {
  margin: 0;
  color: #7c3aed;
  font-size: 11px;
  letter-spacing: 1.8px;
}
.workspace {
  margin-top: 18px;
}
.task-item {
  width: 100%;
  margin-bottom: 10px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: white;
  text-align: left;
  cursor: pointer;
}
.task-item.active {
  border-color: #7c3aed;
  background: #f5f3ff;
}
.task-item div {
  display: flex;
  justify-content: space-between;
}
.task-item p {
  margin: 8px 0;
  color: #475569;
}
.task-item small {
  color: #a16207;
}
.status {
  margin-left: 10px;
}
.detail-tabs {
  margin-top: 20px;
}
.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}
@media (max-width: 1200px) {
  .task-card {
    margin-bottom: 18px;
  }
}
@media (max-width: 768px) {
  .filter-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
