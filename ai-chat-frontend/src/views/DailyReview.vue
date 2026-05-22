<template>
  <div class="page">
    <div class="page-header">
      <h1>每日复盘</h1>
      <p>回顾今天，让每一天都有意义</p>
    </div>

    <div class="card form-card">
      <div class="form-grid">
        <div class="field">
          <label>今天做了什么</label>
          <el-input v-model="form.content" type="textarea" :rows="4"
            placeholder="列出你今天完成的主要事情..." />
        </div>
        <div class="field">
          <label>情绪状态</label>
          <div class="mood-select">
            <button v-for="m in moods" :key="m.key"
              :class="['mood-btn', { active: form.mood === m.key }]"
              @click="form.mood = m.key">
              <span class="mood-emoji">{{ m.emoji }}</span>
              <span class="mood-label">{{ m.label }}</span>
            </button>
          </div>
        </div>
      </div>
      <el-button type="primary" :loading="loading" @click="submit" class="submit-btn">
        开始复盘分析
      </el-button>
    </div>

    <div v-if="result" class="card result-card">
      <h2>AI 分析结果</h2>
      <div class="result-section">
        <h3>今日总结</h3>
        <p>{{ result.aiSummary }}</p>
      </div>
      <div class="result-grid">
        <div class="result-section">
          <h3 class="positive">做得好的地方</h3>
          <ul v-if="parsedPositives.length">
            <li v-for="(item, i) in parsedPositives" :key="i">{{ item }}</li>
          </ul>
          <p v-else class="empty-hint">{{ result.aiPositives }}</p>
        </div>
        <div class="result-section">
          <h3 class="problem">需要改进</h3>
          <ul v-if="parsedProblems.length">
            <li v-for="(item, i) in parsedProblems" :key="i">{{ item }}</li>
          </ul>
          <p v-else class="empty-hint">{{ result.aiProblems }}</p>
        </div>
      </div>
      <div class="result-section">
        <h3 class="suggestion">明日建议</h3>
        <ul v-if="parsedSuggestions.length">
          <li v-for="(item, i) in parsedSuggestions" :key="i">{{ item }}</li>
        </ul>
        <p v-else class="empty-hint">{{ result.aiSuggestions }}</p>
      </div>
    </div>

    <div class="card history-card">
      <h2>历史复盘</h2>
      <div v-if="reviews.length === 0" class="empty">还没有复盘记录，开始第一次吧</div>
      <div v-for="r in reviews" :key="r.id" class="timeline-item"
        @click="loadDetail(r)">
        <div class="timeline-date">{{ r.reviewDate }}</div>
        <div class="timeline-content">
          <div class="timeline-title">{{ r.mood ? moodLabel(r.mood) : '' }} {{ r.aiSummary || r.content?.slice(0, 80) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submitReview, getReviewList, getReviewDetail } from '../api/index.js'
import { ElMessage } from 'element-plus'

const moods = [
  { key: 'happy', emoji: '😊', label: '开心' },
  { key: 'neutral', emoji: '😐', label: '平静' },
  { key: 'sad', emoji: '😔', label: '低落' },
  { key: 'anxious', emoji: '😰', label: '焦虑' }
]

const loading = ref(false)
const form = ref({ content: '', mood: '' })
const result = ref(null)
const reviews = ref([])

const parsedPositives = computed(() => parseJson(result.value?.aiPositives))
const parsedProblems = computed(() => parseJson(result.value?.aiProblems))
const parsedSuggestions = computed(() => parseJson(result.value?.aiSuggestions))

function parseJson(str) {
  try { const v = JSON.parse(str); return Array.isArray(v) ? v : [] } catch { return [] }
}

function moodLabel(key) {
  const m = moods.find(x => x.key === key)
  return m ? m.emoji : ''
}

async function submit() {
  if (!form.value.content.trim()) {
    ElMessage.warning('请填写今天做了什么')
    return
  }
  loading.value = true
  try {
    const res = await submitReview({ content: form.value.content, mood: form.value.mood })
    result.value = res.data
    ElMessage.success('复盘分析完成')
    form.value = { content: '', mood: '' }
    loadList()
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

async function loadList() {
  try {
    const res = await getReviewList()
    reviews.value = res.data || []
  } catch { /* ignore */ }
}

async function loadDetail(r) {
  try {
    const res = await getReviewDetail(r.id)
    result.value = res.data
  } catch { /* ignore */ }
}

onMounted(loadList)
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px 80px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0; }
.page-header p { color: #86868b; margin: 4px 0 0; font-size: 14px; }

.card { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card h2 { font-size: 16px; font-weight: 600; margin: 0 0 16px; color: #1d1d1f; }

.form-grid { display: grid; gap: 20px; margin-bottom: 20px; }
.field label { display: block; font-size: 13px; font-weight: 500; color: #86868b; margin-bottom: 6px; }

.mood-select { display: flex; gap: 8px; flex-wrap: wrap; }
.mood-btn { display: flex; align-items: center; gap: 4px; padding: 8px 14px; border: 1px solid #e5e5ea; border-radius: 10px; background: #fff; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.mood-btn:hover { border-color: #007aff; }
.mood-btn.active { border-color: #007aff; background: #e8f4fd; }
.mood-emoji { font-size: 18px; }
.submit-btn { width: 100%; }

.result-section { margin-bottom: 16px; }
.result-section h3 { font-size: 13px; font-weight: 600; margin: 0 0 8px; color: #86868b; text-transform: uppercase; letter-spacing: 0.5px; }
.result-section h3.positive { color: #34c759; }
.result-section h3.problem { color: #ff9500; }
.result-section h3.suggestion { color: #007aff; }
.result-section p, .result-section li { font-size: 14px; line-height: 1.7; color: #3a3a3c; }
.result-section ul { padding-left: 18px; }
.empty-hint { color: #c7c7cc !important; font-style: italic; }
.result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 500px) { .result-grid { grid-template-columns: 1fr; } }

.history-card { padding: 20px 24px; }
.empty { text-align: center; color: #c7c7cc; padding: 32px 0; font-size: 14px; }
.timeline-item { display: flex; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f5f5f7; cursor: pointer; transition: background 0.15s; }
.timeline-item:hover { background: #fafafa; margin: 0 -24px; padding-left: 24px; padding-right: 24px; border-radius: 8px; }
.timeline-date { font-size: 13px; color: #86868b; white-space: nowrap; min-width: 80px; }
.timeline-content { flex: 1; min-width: 0; }
.timeline-title { font-size: 14px; color: #1d1d1f; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
