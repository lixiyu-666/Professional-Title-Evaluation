<template>
  <div class="app-container policy-page">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="context-card">
          <template #header><strong>问答范围</strong></template>
          <el-form label-position="top">
            <el-form-item label="当前批次">
              <el-select v-model="selectedBatchId" placeholder="请选择批次" style="width: 100%">
                <el-option v-for="batch in batches" :key="String(batch.id)" :label="batch.name" :value="batch.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-alert title="政策助手只负责查找公开依据，不参与资格判断，也不会修改申报和审核状态。" type="warning" :closable="false" show-icon />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="17">
        <el-card shadow="never" class="chat-card">
          <template #header
            ><div class="chat-title"><span>政策问答助手</span><el-tag>本地检索模式</el-tag></div></template
          >
          <div class="conversation">
            <div v-if="!answer" class="welcome">
              <h2>想了解哪项政策？</h2>
              <p>例如：本批次需要经过哪些审核节点？补正后从哪里重新审核？</p>
            </div>
            <div v-if="answer" class="answer-block">
              <div class="answer-text">{{ answer.answer }}</div>
              <el-empty v-if="!answer.citations.length" description="没有找到可靠政策依据" :image-size="80" />
              <el-card v-for="citation in answer.citations" :key="citation.documentName + citation.locator" shadow="never" class="citation">
                <div class="citation-head">
                  <strong>{{ citation.documentName }}</strong
                  ><el-tag size="small">{{ citation.locator }}</el-tag>
                </div>
                <p>{{ citation.excerpt }}</p>
                <el-link v-if="citation.sourceUrl" :href="citation.sourceUrl" target="_blank" type="primary">打开原文</el-link>
              </el-card>
            </div>
          </div>
          <div class="composer">
            <el-input
              v-model="question"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请输入政策、材料、时限或流程问题"
              @keydown.ctrl.enter="ask"
            />
            <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="ask">检索政策依据</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { askPolicy, listTitleBatches } from '@/api/title';
import type { Id, PolicyAnswer, TitleBatch } from '@/api/title/types';

const batches = ref<TitleBatch[]>([]);
const selectedBatchId = ref<Id>();
const question = ref('');
const answer = ref<PolicyAnswer>();
const loading = ref(false);
const selectedBatch = computed(() => batches.value.find((item) => String(item.id) === String(selectedBatchId.value)));

const ask = async () => {
  if (!question.value.trim()) return;
  loading.value = true;
  try {
    const batch = selectedBatch.value;
    answer.value = (
      await askPolicy(
        question.value.trim(),
        batch
          ? {
              evaluationYear: String(batch.evaluationYear),
              titleSeries: batch.titleSeries,
              titleLevel: batch.titleLevel,
              applicationType: batch.applicationType
            }
          : {}
      )
    ).data;
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  batches.value = (await listTitleBatches()).data;
  selectedBatchId.value = batches.value[0]?.id;
});
</script>

<style scoped lang="scss">
.policy-page {
  background: #f6f8fb;
  min-height: calc(100vh - 84px);
}
.context-card,
.chat-card {
  border: 0;
  border-radius: 16px;
}
.chat-title,
.citation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.conversation {
  min-height: 430px;
  max-height: 58vh;
  overflow: auto;
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
}
.welcome {
  padding: 80px 20px;
  text-align: center;
  color: #64748b;
}
.answer-text {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #e8f3f7;
  color: #164e63;
}
.citation {
  margin-bottom: 12px;
  border-color: #dce7ef;
}
.citation p {
  line-height: 1.75;
  color: #475569;
}
.composer {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  align-items: end;
}
.composer .el-button {
  height: 40px;
}
@media (max-width: 1200px) {
  .context-card {
    margin-bottom: 18px;
  }
}
</style>
