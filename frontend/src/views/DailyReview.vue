<template>
  <div class="page">
    <!-- ====== Header ====== -->
    <div class="page-hero">
      <div class="ph-left">
        <p class="page-eyebrow">每日复盘 · {{ today }}</p>
        <h1 class="page-title">回顾今天，让每一天都有意义</h1>
      </div>
      <div class="ph-pills">
        <span class="ph-pill">📝 {{ reviewCount }} 条复盘</span>
        <span class="ph-pill">🔥 {{ streak }} 天连续</span>
      </div>
    </div>

    <!-- ====== 双栏 ====== -->
    <div class="page-grid">
      <!-- 左栏：主内容 -->
      <div class="col-main">
        <!-- 表单 -->
        <div class="card">
          <div class="card-head"><h3>写复盘</h3><span class="badge" v-if="form.content.length">{{ form.content.length }}字</span></div>
          <div class="form-row">
            <textarea v-model="form.content" class="glass-input" rows="3" placeholder="列出你今天完成的主要事情..."></textarea>
            <div class="mood-row">
              <button v-for="m in moods" :key="m.key" :class="['mood-chip', { active: form.mood === m.key }]" @click="form.mood = m.key">
                <span class="moji">{{ m.emoji }}</span><span>{{ m.label }}</span>
              </button>
            </div>
          </div>
          <button class="submit-btn" :disabled="loading" @click="submit">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            {{ loading ? 'AI 正在分析...' : 'AI 复盘分析' }}
          </button>
        </div>

        <!-- AI 结果 -->
        <div v-if="result" class="card result-card anim-up">
          <div class="card-head"><h3>AI 分析结果</h3></div>
          <div class="result-block"><h4>📝 今日总结</h4><p>{{ result.aiSummary }}</p></div>
          <div class="result-2col">
            <div class="result-block"><h4 class="good">✅ 做得好</h4>
              <ul v-if="parsedPositives.length"><li v-for="(item,i) in parsedPositives" :key="i">{{ item }}</li></ul>
              <p v-else class="muted">{{ result.aiPositives }}</p>
            </div>
            <div class="result-block"><h4 class="warn">🔧 需改进</h4>
              <ul v-if="parsedProblems.length"><li v-for="(item,i) in parsedProblems" :key="i">{{ item }}</li></ul>
              <p v-else class="muted">{{ result.aiProblems }}</p>
            </div>
          </div>
          <div class="result-block"><h4 class="next">💡 明日建议</h4>
            <ul v-if="parsedSuggestions.length"><li v-for="(item,i) in parsedSuggestions" :key="i">{{ item }}</li></ul>
            <p v-else class="muted">{{ result.aiSuggestions }}</p>
          </div>
        </div>

        <!-- 历史列表 -->
        <div class="card">
          <div class="card-head"><h3>历史复盘</h3><span class="badge">{{ reviews.length }}</span></div>
          <div v-if="reviews.length === 0" class="empty-mini"><p>还没有复盘记录</p></div>
          <div v-for="r in reviews.slice(0, 8)" :key="r.id" class="ri-item" @click="loadDetail(r)">
            <span class="ri-date">{{ r.reviewDate }}</span>
            <span class="ri-mood">{{ r.mood ? moodLabel(r.mood) : '' }}</span>
            <span class="ri-text">{{ r.aiSummary || r.content?.slice(0, 60) }}</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
        </div>
      </div>

      <!-- 右栏：侧边面板 -->
      <div class="col-side">
        <div class="side-card focus-side">
          <span class="focus-badge">AI 小提示</span>
          <p class="focus-msg">复盘时尽量具体描述，AI 能给你更精准的分析和建议</p>
        </div>
        <div class="side-card">
          <div class="side-title">复盘统计</div>
          <div class="side-stat-row"><span>总复盘数</span><span class="side-stat-val">{{ reviewCount }}</span></div>
          <div class="side-stat-row"><span>本周复盘</span><span class="side-stat-val">{{ weekReviewCount }}</span></div>
          <div class="side-stat-row"><span>最近情绪</span><span class="side-stat-val">{{ lastMood || '暂无' }}</span></div>
        </div>
        <div class="side-card">
          <div class="side-title">复盘技巧</div>
          <ul class="tips-list">
            <li>记录具体事件，而非感受</li>
            <li>关注"学到了什么"</li>
            <li>设定明天的小目标</li>
            <li>诚实地面对不足</li>
          </ul>
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

const today = new Date().toLocaleDateString('zh-CN', { month:'long', day:'numeric', weekday:'short' })
const loading = ref(false)
const form = ref({ content: '', mood: '' })
const result = ref(null)
const reviews = ref([])

const reviewCount = computed(() => reviews.value.length)
const weekReviewCount = computed(() => {
  const weekAgo = new Date(); weekAgo.setDate(weekAgo.getDate() - 7)
  return reviews.value.filter(r => new Date(r.reviewDate) >= weekAgo).length
})
const lastMood = computed(() => {
  const recent = reviews.value.find(r => r.mood)
  if (!recent) return null
  const m = moods.find(x => x.key === recent.mood)
  return m ? m.emoji : null
})
const streak = computed(() => {
  let s = 0; const today = new Date().toISOString().slice(0,10)
  for (let i = 0; i < 365; i++) {
    const d = new Date(); d.setDate(d.getDate() - i)
    const ds = d.toISOString().slice(0,10)
    if (reviews.value.some(r => r.reviewDate === ds)) s++; else break
  }
  return s
})

const parsedPositives = computed(() => parseJson(result.value?.aiPositives))
const parsedProblems = computed(() => parseJson(result.value?.aiProblems))
const parsedSuggestions = computed(() => parseJson(result.value?.aiSuggestions))
function parseJson(str) { try { const v = JSON.parse(str); return Array.isArray(v) ? v : [] } catch { return [] } }
function moodLabel(key) { const m = moods.find(x => x.key === key); return m ? m.emoji : '' }

async function submit() {
  if (!form.value.content.trim()) { ElMessage.warning('请填写内容'); return }
  loading.value = true
  try {
    const res = await submitReview({ content: form.value.content, mood: form.value.mood })
    result.value = res.data; ElMessage.success('分析完成')
    form.value = { content: '', mood: '' }; loadList()
  } catch { /* handled */ }
  finally { loading.value = false }
}
async function loadList() { try { const r = await getReviewList(); reviews.value = r.data || [] } catch { /* ignore */ } }
async function loadDetail(r) { try { const res = await getReviewDetail(r.id); result.value = res.data } catch { /* ignore */ } }
onMounted(loadList)
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
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.card-head h3 { font-size: 14px; font-weight: 650; color: #1C2640; margin: 0; }
.badge { font-size: 11px; color: #909BB5; font-weight: 550; padding: 2px 8px; border-radius: 10px; background: rgba(79,140,255,0.05); }

.form-row { display: flex; flex-direction: column; gap: 12px; margin-bottom: 14px; }
.glass-input {
  width: 100%; border: 1px solid #E4EAF4; border-radius: 12px;
  padding: 10px 14px; font-size: 13.5px; line-height: 1.5;
  font-family: inherit; color: #1C2640; background: #F8FAFD;
  resize: vertical; outline: none; transition: all 0.2s; box-sizing: border-box;
}
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.07); }
.mood-row { display: flex; gap: 6px; flex-wrap: wrap; }
.mood-chip {
  display: flex; align-items: center; gap: 4px; padding: 6px 14px; border-radius: 10px;
  border: 1px solid #E4EAF4; background: #fff; cursor: pointer;
  font-size: 12.5px; font-family: inherit; transition: all 0.15s; color: #5B6780;
}
.mood-chip:hover { border-color: #BDD3FF; background: #F0F5FF; }
.mood-chip.active { border-color: #4F8CFF; background: #E0ECFF; color: #3B6FDF; font-weight: 600; }
.moji { font-size: 15px; }
.submit-btn {
  width: 100%; display: flex; align-items: center; justify-content: center; gap: 7px;
  padding: 10px 20px; border-radius: 12px; border: none;
  background: linear-gradient(135deg, #4F8CFF, #3B6FDF);
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
  box-shadow: 0 3px 12px rgba(79,140,255,0.18); transition: all 0.2s; font-family: inherit;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 5px 18px rgba(79,140,255,0.25); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.result-card { background: linear-gradient(155deg, #F8FAFD, #F0F5FF); border-color: #BDD3FF; }
.result-block { margin-bottom: 12px; }
.result-block h4 { font-size: 11.5px; font-weight: 650; color: #5B6780; margin: 0 0 6px; }
.result-block h4.good { color: #10B981; } .result-block h4.warn { color: #F59E0B; } .result-block h4.next { color: #4F8CFF; }
.result-block p, .result-block li { font-size: 13px; line-height: 1.6; color: #3C4A6E; } .result-block ul { padding-left: 16px; margin: 0; }
.muted { color: #A0ACC5 !important; font-style: italic; }
.result-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.empty-mini { text-align: center; padding: 20px 0; } .empty-mini p { font-size: 13px; color: #A0ACC5; margin: 0; }
.ri-item { display: flex; align-items: center; gap: 10px; padding: 9px 10px; border-radius: 10px; cursor: pointer; transition: background 0.15s; }
.ri-item:hover { background: rgba(79,140,255,0.02); }
.ri-date { font-size: 11.5px; color: #4F8CFF; font-weight: 550; min-width: 60px; flex-shrink: 0; }
.ri-mood { font-size: 14px; flex-shrink: 0; }
.ri-text { flex: 1; font-size: 13px; color: #3C4A6E; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ri-item svg { color: #BDD3FF; flex-shrink: 0; }

/* Sidebar */
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

@media (max-width: 800px) { .page-grid { grid-template-columns: 1fr; } .col-side { position: static; } .result-2col { grid-template-columns: 1fr; } }
</style>
