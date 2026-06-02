<template>
  <div class="page">
    <div class="page-hero">
      <div class="ph-left">
        <p class="page-eyebrow">表达训练 · {{ today }}</p>
        <h1 class="page-title">清晰表达，自信沟通</h1>
      </div>
      <div class="ph-pills">
        <span class="ph-pill">🎤 {{ records.length }} 次训练</span>
      </div>
    </div>

    <div class="page-grid">
      <div class="col-main">
        <!-- 今日话题 -->
        <div class="topic-card" v-if="topic">
          <span class="topic-chip">今日话题</span>
          <h2 class="topic-text">{{ topic }}</h2>
        </div>

        <!-- 表达输入 -->
        <div class="card">
          <div class="card-head"><h3>我的表达</h3><span class="badge">{{ content.length }}字</span></div>
          <textarea v-model="content" class="glass-input" rows="4" placeholder="围绕今天的话题，写下你的想法..."></textarea>
          <div class="form-foot">
            <span class="char-hint">可以打字，也可以模拟口头表达</span>
            <button class="submit-btn" :disabled="loading" @click="submit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              {{ loading ? '分析中...' : '提交分析' }}
            </button>
          </div>
        </div>

        <!-- AI 结果 -->
        <div v-if="result" class="card result-card anim-up">
          <div class="card-head"><h3>AI 分析</h3></div>
          <div class="scores-row">
            <div class="score-ring"><div class="ring blue">{{ result.aiLogicScore ?? '-' }}</div><span>逻辑评分</span></div>
            <div class="score-ring"><div class="ring green">{{ result.aiClarityScore ?? '-' }}</div><span>清晰度评分</span></div>
          </div>
          <div v-if="problems.length" class="fb"><h4>🔧 表达问题</h4><ul><li v-for="(p,i) in problems" :key="i">{{ p }}</li></ul></div>
          <div v-if="result.aiOptimizedExpression" class="fb"><h4 class="sug">✨ 优化示例</h4><p>{{ result.aiOptimizedExpression }}</p></div>
          <div v-if="suggestions.length" class="fb"><h4>💡 训练建议</h4><ul><li v-for="(s,i) in suggestions" :key="i">{{ s }}</li></ul></div>
        </div>

        <!-- 历史 -->
        <div class="card">
          <div class="card-head"><h3>历史记录</h3><span class="badge">{{ records.length }}</span></div>
          <div v-if="records.length === 0" class="empty-mini"><p>还没有表达记录</p></div>
          <div v-for="r in records.slice(0, 8)" :key="r.id" class="rec-item" @click="viewDetail(r)">
            <div class="rec-head"><span class="rec-topic">{{ r.topic }}</span><span v-if="r.aiLogicScore" class="rec-score">逻辑 {{ r.aiLogicScore }}</span></div>
            <p class="rec-text">{{ r.content?.slice(0, 100) }}{{ r.content?.length > 100 ? '...' : '' }}</p>
            <div class="rec-foot">
              <span class="rec-date">{{ r.createdAt?.slice(0,16)?.replace('T',' ') }}</span>
              <button class="rec-del" @click.stop="deleteRecord(r)">删除</button>
            </div>
          </div>
        </div>

        <!-- 详情弹窗遮罩 -->
        <div v-if="detailVisible" class="modal-mask" @click.self="detailVisible = false">
          <div class="modal-card" v-if="detail">
            <div class="modal-header">
              <h3>{{ detail.topic }}</h3>
              <button class="modal-close" @click="detailVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="detail-block"><h4>我的表达</h4><p>{{ detail.content }}</p></div>
              <div class="scores-row">
                <div class="score-ring"><div class="ring blue">{{ detail.aiLogicScore ?? '-' }}</div><span>逻辑评分</span></div>
                <div class="score-ring"><div class="ring green">{{ detail.aiClarityScore ?? '-' }}</div><span>清晰度评分</span></div>
              </div>
              <div v-if="detailProblems.length" class="fb"><h4>🔧 表达问题</h4><ul><li v-for="(p,i) in detailProblems" :key="i">{{ p }}</li></ul></div>
              <div v-if="detail.aiOptimizedExpression" class="fb"><h4 class="sug">✨ 优化示例</h4><p>{{ detail.aiOptimizedExpression }}</p></div>
              <div v-if="detailSuggestions.length" class="fb"><h4>💡 训练建议</h4><ul><li v-for="(s,i) in detailSuggestions" :key="i">{{ s }}</li></ul></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边 -->
      <div class="col-side">
        <div class="side-card focus-side">
          <span class="focus-badge">表达技巧</span>
          <p class="focus-msg">先组织逻辑再开口，用"第一、第二"让表达更有条理</p>
        </div>
        <div class="side-card">
          <div class="side-title">训练统计</div>
          <div class="side-stat-row"><span>总训练数</span><span class="side-stat-val">{{ records.length }}</span></div>
          <div class="side-stat-row"><span>最新逻辑分</span><span class="side-stat-val">{{ latestLogic || '-' }}</span></div>
          <div class="side-stat-row"><span>最新清晰分</span><span class="side-stat-val">{{ latestClarity || '-' }}</span></div>
        </div>
        <div class="side-card">
          <div class="side-title">评分说明</div>
          <ul class="tips-list">
            <li>逻辑: 论点是否清晰有层次</li>
            <li>清晰: 语言是否易懂不啰嗦</li>
            <li>80+ 已属优秀</li>
            <li>持续练习比分数更重要</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTodayTopic, submitExpression, getExpressionHistory, getExpressionDetail, deleteExpression } from '../api/index.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const today = new Date().toLocaleDateString('zh-CN', { month:'long', day:'numeric', weekday:'short' })
const topic = ref(''); const content = ref(''); const loading = ref(false); const result = ref(null); const records = ref([])
const detailVisible = ref(false); const detail = ref(null); const detailProblems = ref([]); const detailSuggestions = ref([])

const problems = computed(() => {
  if (!result.value?.aiProblems) return []
  try { return typeof result.value.aiProblems === 'string' ? JSON.parse(result.value.aiProblems) : result.value.aiProblems } catch { return [] }
})
const suggestions = computed(() => {
  if (!result.value?.aiSuggestions) return []
  try { return typeof result.value.aiSuggestions === 'string' ? JSON.parse(result.value.aiSuggestions) : result.value.aiSuggestions } catch { return [] }
})
const latestLogic = computed(() => { const r = records.value.find(x => x.aiLogicScore); return r?.aiLogicScore })
const latestClarity = computed(() => { const r = records.value.find(x => x.aiClarityScore); return r?.aiClarityScore })

async function submit() {
  if (!content.value.trim()) { ElMessage.warning('请写下表达内容'); return }
  loading.value = true
  try { const res = await submitExpression({ content: content.value }); result.value = res.data; ElMessage.success('分析完成'); content.value = ''; loadRecords() } catch { /* */ }
  finally { loading.value = false }
}
async function loadData() { try { const t = await getTodayTopic(); topic.value = t.data?.topic || '' } catch { /* */ } loadRecords() }
async function loadRecords() { try { const r = await getExpressionHistory(); records.value = r.data || [] } catch { /* */ } }
async function viewDetail(rec) {
  detailVisible.value = true; detail.value = null
  try {
    const r = await getExpressionDetail(rec.id)
    const d = r.data
    detail.value = d
    try { detailProblems.value = typeof d.aiProblems === 'string' ? JSON.parse(d.aiProblems) : (d.aiProblems || []) } catch { detailProblems.value = [] }
    try { detailSuggestions.value = typeof d.aiSuggestions === 'string' ? JSON.parse(d.aiSuggestions) : (d.aiSuggestions || []) } catch { detailSuggestions.value = [] }
  } catch { /* */ }
}
async function deleteRecord(rec) {
  try {
    await ElMessageBox.confirm(`确定删除「${rec.topic}」这条记录吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteExpression(rec.id)
    ElMessage.success('已删除')
    records.value = records.value.filter(r => r.id !== rec.id)
  } catch { /* 用户取消或删除失败 */ }
}
onMounted(loadData)
</script>

<style scoped>
.page { max-width: 1140px; margin: 0 auto; padding: 24px 20px 80px; }
.page-hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 16px; }
.page-eyebrow { font-size: 11px; font-weight: 650; color: #4F8CFF; letter-spacing: 0.06em; margin: 0 0 4px; }
.page-title { font-size: 24px; font-weight: 750; color: #1C2640; margin: 0; letter-spacing: -0.02em; }
.ph-pills { display: flex; gap: 6px; flex-shrink: 0; }
.ph-pill { font-size: 11.5px; color: #5B6780; padding: 4px 12px; border-radius: 16px; background: rgba(255,255,255,0.6); border: 1px solid #E4EAF4; font-weight: 500; }

.page-grid { display: grid; grid-template-columns: 1fr 270px; gap: 16px; align-items: start; }

.card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 18px 20px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 14px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-head h3 { font-size: 14px; font-weight: 650; color: #1C2640; margin: 0; }
.badge { font-size: 11px; color: #909BB5; font-weight: 550; padding: 2px 8px; border-radius: 10px; background: rgba(79,140,255,0.05); }

.topic-card { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border: 1px solid #BDD3FF; border-radius: 14px; padding: 16px 20px; margin-bottom: 14px; }
.topic-chip { font-size: 10.5px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; letter-spacing: 0.04em; }
.topic-text { font-size: 17px; font-weight: 650; color: #1C2640; margin: 6px 0 0; line-height: 1.5; }

.glass-input { width: 100%; border: 1px solid #E4EAF4; border-radius: 12px; padding: 10px 14px; font-size: 13.5px; line-height: 1.5; font-family: inherit; color: #1C2640; background: #F8FAFD; resize: vertical; outline: none; box-sizing: border-box; }
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.07); }
.form-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.char-hint { font-size: 11.5px; color: #A0ACC5; }
.submit-btn { display: flex; align-items: center; gap: 6px; padding: 8px 18px; border-radius: 10px; border: none; background: linear-gradient(135deg, #4F8CFF, #3B6FDF); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; box-shadow: 0 2px 8px rgba(79,140,255,0.15); transition: all 0.15s; font-family: inherit; }
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(79,140,255,0.22); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.result-card { background: linear-gradient(155deg, #F8FAFD, #F0F5FF); border-color: #BDD3FF; }
.scores-row { display: flex; gap: 32px; justify-content: center; margin-bottom: 16px; }
.score-ring { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.ring { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 750; color: #fff; }
.ring.blue { background: linear-gradient(135deg, #4F8CFF, #3B6FDF); box-shadow: 0 3px 12px rgba(79,140,255,0.2); }
.ring.green { background: linear-gradient(135deg, #10B981, #059669); box-shadow: 0 3px 12px rgba(16,185,129,0.2); }
.score-ring span { font-size: 11px; color: #909BB5; font-weight: 550; }
.fb { padding: 12px 14px; background: #fff; border: 1px solid #E4EAF4; border-radius: 12px; margin-bottom: 10px; }
.fb:last-child { margin-bottom: 0; }
.fb h4 { font-size: 11.5px; font-weight: 650; color: #5B6780; margin: 0 0 5px; } .fb h4.sug { color: #4F8CFF; }
.fb ul { margin: 0; padding-left: 14px; } .fb li, .fb p { font-size: 13px; line-height: 1.5; color: #3C4A6E; margin: 0; }

.empty-mini { text-align: center; padding: 20px 0; } .empty-mini p { font-size: 13px; color: #A0ACC5; margin: 0; }
.rec-item { padding: 10px 0; border-bottom: 1px solid #F0F4FA; cursor: pointer; border-radius: 8px; padding: 10px 8px; margin: 0 -8px; transition: background 0.12s; }
.rec-item:hover { background: rgba(79,140,255,0.04); }
.rec-item:last-child { border: none; }
.rec-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px; }
.rec-topic { font-size: 13px; font-weight: 600; color: #1C2640; } .rec-score { font-size: 11.5px; font-weight: 600; color: #4F8CFF; }
.rec-text { font-size: 12.5px; color: #5B6780; margin: 3px 0; line-height: 1.4; }
.rec-foot { display: flex; justify-content: space-between; align-items: center; }
.rec-date { font-size: 11px; color: #A0ACC5; }
.rec-del { font-size: 11px; color: #E55555; background: none; border: 1px solid #F5D0D0; border-radius: 6px; padding: 2px 10px; cursor: pointer; font-family: inherit; transition: all 0.12s; }
.rec-del:hover { background: #FFF0F0; border-color: #E55555; }

/* 详情弹窗 */
.modal-mask { position: fixed; inset: 0; background: rgba(28,38,64,0.35); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal-card { background: #fff; border-radius: 18px; max-width: 640px; width: 100%; max-height: 85vh; overflow-y: auto; box-shadow: 0 16px 48px rgba(28,38,64,0.18); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px 0; }
.modal-header h3 { font-size: 16px; font-weight: 650; color: #1C2640; margin: 0; line-height: 1.4; }
.modal-close { background: none; border: none; font-size: 22px; color: #909BB5; cursor: pointer; padding: 0 4px; line-height: 1; transition: color 0.12s; }
.modal-close:hover { color: #1C2640; }
.modal-body { padding: 16px 22px 22px; }
.detail-block { background: #F8FAFD; border: 1px solid #E4EAF4; border-radius: 12px; padding: 14px; margin-bottom: 16px; }
.detail-block h4 { font-size: 11.5px; font-weight: 650; color: #909BB5; margin: 0 0 6px; text-transform: uppercase; letter-spacing: 0.04em; }
.detail-block p { font-size: 13.5px; color: #3C4A6E; margin: 0; line-height: 1.6; white-space: pre-wrap; }

.col-side { position: sticky; top: 24px; }
.side-card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 16px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 12px; }
.side-title { font-size: 11.5px; font-weight: 650; color: #909BB5; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 10px; }
.focus-side { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border-color: #BDD3FF; }
.focus-badge { font-size: 10px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 6px; }
.focus-msg { font-size: 12.5px; color: #3C4A6E; margin: 0; line-height: 1.5; }
.side-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 12.5px; color: #5B6780; }
.side-stat-val { font-weight: 650; color: #4F8CFF; }
.tips-list { margin: 0; padding-left: 14px; display: flex; flex-direction: column; gap: 5px; }
.tips-list li { font-size: 12px; color: #5B6780; line-height: 1.4; }

@media (max-width: 800px) { .page-grid { grid-template-columns: 1fr; } .col-side { position: static; } }
</style>
