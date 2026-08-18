<template>
  <div class="app-container title-batch-manage">
    <el-card shadow="never">
      <template #header>
        <div class="header"><span>批次管理</span><el-button type="primary" :icon="Plus" @click="openCreate">新建批次草稿</el-button></div>
      </template>
      <el-alert title="发布后批次配置将锁定；如需调整，请新建新的草稿批次。" type="warning" :closable="false" show-icon class="mb16" />
      <el-table :data="batches" v-loading="loading" border>
        <el-table-column prop="name" label="批次名称" min-width="250" />
        <el-table-column label="申报范围" min-width="190"
          ><template #default="{ row }">{{ row.titleSeries }} · {{ row.titleLevel }} · {{ row.applicationType }}</template></el-table-column
        >
        <el-table-column prop="firstSubmitDeadline" label="首次截止时间" width="180"
          ><template #default="{ row }">{{ formatTime(row.firstSubmitDeadline) }}</template></el-table-column
        >
        <el-table-column prop="ruleVersion" label="规则版本" width="120" />
        <el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.published ? 'success' : 'info'">{{ row.published ? '已发布' : '草稿' }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="操作" width="180" fixed="right"
          ><template #default="{ row }"
            ><el-button v-if="!row.published" link type="primary" @click="openEdit(row)">编辑</el-button
            ><el-button v-if="!row.published" link type="success" @click="publish(row)">发布</el-button
            ><span v-else class="muted">配置已锁定</span></template
          ></el-table-column
        >
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="editingId ? '编辑批次草稿' : '新建批次草稿'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16"
          ><el-col :span="16"
            ><el-form-item label="批次名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col
          ><el-col :span="8"
            ><el-form-item label="评审年度" prop="evaluationYear"
              ><el-input-number v-model="form.evaluationYear" :min="2020" :max="2100" class="full" /></el-form-item></el-col
        ></el-row>
        <el-row :gutter="16"
          ><el-col :span="8"
            ><el-form-item label="职称系列" prop="titleSeries"><el-input v-model="form.titleSeries" /></el-form-item></el-col
          ><el-col :span="8"
            ><el-form-item label="申报级别" prop="titleLevel"><el-input v-model="form.titleLevel" /></el-form-item></el-col
          ><el-col :span="8"
            ><el-form-item label="申报类型" prop="applicationType"><el-input v-model="form.applicationType" /></el-form-item></el-col
        ></el-row>
        <el-row :gutter="16"
          ><el-col :span="12"
            ><el-form-item label="开放时间" prop="openAt"
              ><el-date-picker v-model="form.openAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="full" /></el-form-item></el-col
          ><el-col :span="12"
            ><el-form-item label="首次截止" prop="firstSubmitDeadline"
              ><el-date-picker
                v-model="form.firstSubmitDeadline"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="full" /></el-form-item></el-col
        ></el-row>
        <el-row :gutter="16"
          ><el-col :span="12"
            ><el-form-item label="补正时长(小时)" prop="defaultCorrectionHours"
              ><el-input-number v-model="form.defaultCorrectionHours" :min="1" :max="720" class="full" /></el-form-item></el-col
          ><el-col :span="12"
            ><el-form-item label="规则版本" prop="ruleVersion"><el-input v-model="form.ruleVersion" /></el-form-item></el-col
        ></el-row>
        <el-form-item label="材料清单"
          ><el-select
            v-model="form.config!.materialChecklist"
            multiple
            filterable
            allow-create
            default-first-option
            class="full"
            placeholder="输入材料名称后按回车添加"
        /></el-form-item>
        <el-form-item label="资格说明"
          ><el-input
            v-model="form.config!.qualificationNotes"
            type="textarea"
            :rows="3"
            placeholder="例如：申报人须满足本年度工程系列副高级正常晋升要求。"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
      >
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { createTitleBatch, listManagedTitleBatches, publishTitleBatch, updateTitleBatch } from '@/api/title';
import type { BatchConfigInput, TitleBatch } from '@/api/title/types';

const loading = ref(false);
const saving = ref(false);
const visible = ref(false);
const editingId = ref<string | number>();
const batches = ref<TitleBatch[]>([]);
const formRef = ref<FormInstance>();
const blank = (): BatchConfigInput => ({
  name: '',
  evaluationYear: new Date().getFullYear(),
  titleSeries: '工程系列',
  titleLevel: '副高级',
  applicationType: '正常晋升',
  openAt: '',
  firstSubmitDeadline: '',
  defaultCorrectionHours: 72,
  ruleVersion: 'MVP-1.0',
  config: { materialChecklist: [], policyDocumentIds: [], qualificationNotes: '' }
});
const form = reactive<BatchConfigInput>(blank());
const rules: FormRules = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  openAt: [{ required: true, message: '请选择开放时间', trigger: 'change' }],
  firstSubmitDeadline: [{ required: true, message: '请选择首次截止时间', trigger: 'change' }],
  ruleVersion: [{ required: true, message: '请输入规则版本', trigger: 'blur' }]
};
const formatTime = (value: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-');
const load = async () => {
  loading.value = true;
  try {
    batches.value = (await listManagedTitleBatches()).data;
  } finally {
    loading.value = false;
  }
};
const assign = (source: BatchConfigInput) =>
  Object.assign(form, blank(), source, {
    config: { materialChecklist: [], policyDocumentIds: [], qualificationNotes: '', ...(source.config || {}) }
  });
const openCreate = () => {
  editingId.value = undefined;
  assign(blank());
  visible.value = true;
};
const openEdit = (row: TitleBatch) => {
  editingId.value = row.id;
  let config = {};
  try {
    config = row.configJson ? JSON.parse(row.configJson) : {};
  } catch {
    /* legacy blank config */
  }
  assign({ ...row, config } as BatchConfigInput);
  visible.value = true;
};
const save = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (editingId.value) await updateTitleBatch(editingId.value, form);
    else await createTitleBatch(form);
    ElMessage.success('批次草稿已保存');
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
};
const publish = async (row: TitleBatch) => {
  await ElMessageBox.confirm(`发布“${row.name}”后将不能修改配置，确认发布？`, '发布确认', { type: 'warning' });
  await publishTitleBatch(row.id);
  ElMessage.success('批次已发布并锁定');
  await load();
};
onMounted(load);
</script>

<style scoped lang="scss">
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
}
.mb16 {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.muted {
  color: #909399;
  font-size: 13px;
}
</style>
