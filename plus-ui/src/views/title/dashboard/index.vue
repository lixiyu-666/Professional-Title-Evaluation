<template>
  <div class="app-container title-dashboard">
    <section class="hero">
      <div>
        <p class="eyebrow">PROFESSIONAL TITLE EVALUATION</p>
        <h1>职称评审工作台</h1>
        <p>{{ welcomeText }}</p>
      </div>
      <el-tag size="large" effect="dark">工程系列 · 副高级 · 正常晋升</el-tag>
    </section>

    <el-row :gutter="18" class="metric-grid">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never"><el-statistic title="开放批次" :value="batches.length" /></el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never"
          ><el-statistic :title="isApplicant ? '我的申报' : '我的待办'" :value="isApplicant ? applications.length : reviewTasks.length"
        /></el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never"><el-statistic title="待补正" :value="correctionCount" /></el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="batch-card">
      <template #header><div class="card-title">当前批次</div></template>
      <el-empty v-if="!batches.length" description="暂无开放批次" />
      <div v-for="batch in batches" :key="String(batch.id)" class="batch-line">
        <div>
          <strong>{{ batch.name }}</strong>
          <p>{{ batch.titleSeries }} · {{ batch.titleLevel }} · {{ batch.applicationType }}</p>
        </div>
        <div class="deadline">首次提交截止：{{ formatTime(batch.firstSubmitDeadline) }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { listMyApplications, listReviewTasks, listTitleBatches } from '@/api/title';
import type { TitleApplication, TitleBatch } from '@/api/title/types';
import { useUserStore } from '@/store/modules/user';

const userStore = useUserStore();
const batches = ref<TitleBatch[]>([]);
const applications = ref<TitleApplication[]>([]);
const reviewTasks = ref<TitleApplication[]>([]);
const isApplicant = computed(() => userStore.roles.includes('title_applicant'));
const correctionCount = computed(() => applications.value.filter((item) => item.status === 'CORRECTION_REQUIRED').length);
const welcomeText = computed(() => (isApplicant.value ? '从这里查看申报进度、截止时间和补正提醒。' : '从这里查看当前审核节点与待处理任务。'));

const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-');

onMounted(async () => {
  const batchResponse = await listTitleBatches();
  batches.value = batchResponse.data;
  if (isApplicant.value) {
    applications.value = (await listMyApplications()).data;
  } else {
    reviewTasks.value = (await listReviewTasks()).data;
  }
});
</script>

<style scoped lang="scss">
.title-dashboard {
  background: #f4f7fb;
  min-height: calc(100vh - 84px);
}
.hero {
  display: flex;
  justify-content: space-between;
  align-items: end;
  padding: 32px;
  border-radius: 18px;
  color: white;
  background: linear-gradient(135deg, #103b66, #176b87 58%, #38a3a5);
  box-shadow: 0 16px 40px rgb(16 59 102 / 18%);
}
.hero h1 {
  margin: 8px 0;
  font-size: 30px;
}
.hero p {
  margin: 0;
  opacity: 0.86;
}
.eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
}
.metric-grid {
  margin-top: 18px;
}
.metric-grid :deep(.el-card) {
  border: 0;
  border-radius: 14px;
}
.batch-card {
  margin-top: 18px;
  border: 0;
  border-radius: 14px;
}
.card-title {
  font-weight: 700;
}
.batch-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 4px;
}
.batch-line p {
  margin: 6px 0 0;
  color: #718096;
}
.deadline {
  color: #b45309;
}
@media (max-width: 768px) {
  .hero,
  .batch-line {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }
}
</style>
