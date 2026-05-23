<template>
  <div class="page">
    <div class="page-header">
      <h1>表达训练</h1>
      <p>每天练习，清晰表达你的想法</p>
    </div>

    <!-- Today's topic -->
    <div class="card topic-card" v-if="topic">
      <span class="topic-label">今日话题</span>
      <h2 class="topic-text">{{ topic }}</h2>
    </div>

    <!-- Expression input -->
    <div class="card form-card">
      <h2>我的表达</h2>
      <el-input v-model="content" type="textarea" :rows="6"
        placeholder="围绕今天的话题，写下你的想法。可以是文字，也可以模拟一段口头表达..." />
      <div class="form-footer">
        <span class="char-count">{{ content.length }} 字</span>
        <el-button type="primary" :loading="loading" @click="submit">
          提交分析
        </el-button>
      </div>
    </div>

    <!-- AI Analysis result -->
    <div v-if="result" class="card result-card">
      <h2>AI 分析</h2>
      <div class="scores-row">
        <div class="score-item">
          <div class="ring">{{ result.aiLogicScore ?? '-' }}</div>
          <span class="ring-label">逻辑评分</span>
        </div>
        <div class="score-item">
          <div class="ring green">{{ result.aiClarityScore ?? '-' }}</div>
          <span class="ring-label">清晰度评分</span>
        </div>
      </div>
      <div v-if="problems.length" class="feedback-block">
        <h3 class="fb-title">表达问题</h3>
        <ul class="problem-list">
          <li v-for="(p, i) in problems" :key="i">{{ p }}</li>
        </ul>
      </div>
      <div v-if="result.aiOptimizedExpression" class="feedback-block">
        <h3 class="fb-title suggest">优化示例</h3>
        <p>{{ result.aiOptimizedExpression }}</p>
      </div>
      <div v-if="suggestions.length" class="feedback-block">
        <h3 class="fb-title">训练建议</h3>
        <ul class="problem-list">
          <li v-for="(s, i) in suggestions" :key="i">{{ s }}</li>
        </ul>
      </div>
    </div>

    <!-- History -->
    <div class="card history-card">
      <h2>历史记录</h2>
      <div v-if="records.length === 0" class="empty">还没有表达记录</div>
      <div v-for="r in records" :key="r.id" class="record-item">
        <div class="record-header">
          <span class="record-topic">{{ r.topic }}</span>
          <span v-if="r.aiLogicScore" class="record-score">逻辑 {{ r.aiLogicScore }}</span>
        </div>
        <p class="record-content">{{ r.content?.slice(0, 120) }}{{ r.content?.length > 120 ? '...' : '' }}</p>
        <div class="record-date">{{ r.createdAt?.slice(0, 16)?.replace('T', ' ') }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTodayTopic, submitExpression, getExpressionHistory } from '../api/index.js'
import { ElMessage } from 'element-plus'

const topic = ref('')
const content = ref('')
const loading = ref(false)
const result = ref(null)
const records = ref([])

const problems = computed(() => {
  if (!result.value?.aiProblems) return []
  try { return typeof result.value.aiProblems === 'string' ? JSON.parse(result.value.aiProblems) : result.value.aiProblems }
  catch { return [] }
})

const suggestions = computed(() => {
  if (!result.value?.aiSuggestions) return []
  try { return typeof result.value.aiSuggestions === 'string' ? JSON.parse(result.value.aiSuggestions) : result.value.aiSuggestions }
  catch { return [] }
})

async function submit() {
  if (!content.value.trim()) {
    ElMessage.warning('请写下你的表达内容')
    return
  }
  loading.value = true
  try {
    const res = await submitExpression({ content: content.value })
    result.value = res.data
    ElMessage.success('分析完成')
    content.value = ''
    loadRecords()
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function loadData() {
  try {
    const t = await getTodayTopic()
    topic.value = t.data?.topic || ''
  } catch { /* ignore */ }
  loadRecords()
}

async function loadRecords() {
  try {
    const r = await getExpressionHistory()
    records.value = r.data || []
  } catch { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px 80px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0; }
.page-header p { color: #86868b; margin: 4px 0 0; font-size: 14px; }

.card { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card h2 { font-size: 16px; font-weight: 600; margin: 0 0 16px; color: #1d1d1f; }

.topic-card { background: linear-gradient(135deg, #f5f9ff 0%, #e8f4fd 100%); border: 1px solid #d6e8fb; }
.topic-label { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #007aff; font-weight: 600; }
.topic-text { font-size: 18px; font-weight: 600; color: #1d1d1f; margin-top: 8px; line-height: 1.5; }

.form-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.char-count { font-size: 13px; color: #c7c7cc; }

.scores-row { display: flex; gap: 32px; justify-content: center; margin-bottom: 20px; }
.score-item { display: flex; flex-direction: column; align-items: center; }
.ring { width: 64px; height: 64px; border-radius: 50%; background: #007aff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 700; }
.ring.green { background: #34c759; }
.ring-label { font-size: 12px; color: #86868b; margin-top: 6px; }

.problem-list { margin: 4px 0 0; padding-left: 18px; }
.problem-list li { font-size: 14px; line-height: 1.8; color: #3a3a3c; }

.feedback-block { margin-bottom: 14px; padding: 14px; background: #fafafa; border-radius: 10px; }
.fb-title { font-size: 12px; font-weight: 600; color: #86868b; margin: 0 0 4px; text-transform: uppercase; letter-spacing: 0.5px; }
.fb-title.suggest { color: #007aff; }
.feedback-block p { font-size: 14px; line-height: 1.7; color: #3a3a3c; margin: 0; }

.empty { text-align: center; color: #c7c7cc; padding: 32px 0; font-size: 14px; }
.record-item { padding: 14px 0; border-bottom: 1px solid #f5f5f7; }
.record-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.record-topic { font-size: 14px; font-weight: 600; color: #1d1d1f; }
.record-score { font-size: 13px; font-weight: 600; color: #007aff; }
.record-content { font-size: 13px; color: #6e6e73; margin: 4px 0; line-height: 1.5; }
.record-date { font-size: 12px; color: #c7c7cc; }
</style>
