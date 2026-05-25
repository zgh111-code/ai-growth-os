<template>
  <div class="page">
    <div class="page-hero">
      <div class="ph-left">
        <p class="page-eyebrow">学习追踪 · {{ today }}</p>
        <h1 class="page-title">持续行动，看见成长</h1>
      </div>
      <div class="ph-pills">
        <span class="ph-pill">🔥 {{ stats?.streak || 0 }} 天连续</span>
        <span class="ph-pill">📅 本周 {{ stats?.weekDays || 0 }}/7</span>
      </div>
    </div>

    <div class="page-grid">
      <div class="col-main">
        <!-- 3 stat cards -->
        <div class="mini-stats">
          <div class="ms-card"><span class="ms-icon">🔥</span><span class="ms-val">{{ stats?.streak || 0 }}</span><span class="ms-lbl">连续打卡</span></div>
          <div class="ms-card"><span class="ms-icon">📅</span><span class="ms-val">{{ stats?.weekDays || 0 }}</span><span class="ms-lbl">本周天数</span></div>
          <div class="ms-card"><span class="ms-icon">✅</span><span class="ms-val">{{ stats?.weekTotal || 0 }}</span><span class="ms-lbl">本周次数</span></div>
        </div>

        <!-- 今日打卡 -->
        <div class="card">
          <div class="card-head"><h3>今日打卡</h3></div>
          <div class="checkin-grid">
            <button v-for="cat in categories" :key="cat.key" :class="['cc-chip', { done: todayChecked[cat.key] }]" :disabled="todayChecked[cat.key]" @click="openCheckin(cat)">
              <span class="cc-icon">{{ cat.icon }}</span><span class="cc-label">{{ cat.label }}</span>
              <span class="cc-check" v-if="todayChecked[cat.key]"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg></span>
            </button>
          </div>
        </div>

        <!-- 本周统计 + 月度日历 并排 -->
        <div class="dual-cards">
          <div class="card card-half">
            <div class="card-head"><h3>本周分类</h3></div>
            <div class="cat-bars">
              <div v-for="cat in categories" :key="cat.key" class="cat-bar">
                <span class="cat-name">{{ cat.icon }} {{ cat.label }}</span>
                <div class="cat-track"><div class="cat-fill" :style="{ width: Math.min(((stats?.categoryMinutes?.[cat.key] || 0) / 120) * 100, 100) + '%' }"></div></div>
                <span class="cat-data">{{ stats?.categoryCount?.[cat.key] || 0 }}次</span>
              </div>
            </div>
          </div>
          <div class="card card-half">
            <div class="card-head cal-head-row">
              <button class="cal-nav" @click="prevMonth"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg></button>
              <span class="cal-month">{{ year }}.{{ month }}</span>
              <button class="cal-nav" @click="nextMonth"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button>
            </div>
            <div class="cal-grid">
              <div class="cal-h" v-for="d in ['一','二','三','四','五','六','日']" :key="d">{{ d }}</div>
              <div v-for="(day,i) in calendarDays" :key="i" :class="['cal-cell', { active: day.hasData, today: day.isToday, dim: !day.isCurrentMonth }]" :title="day.tooltip"><span v-if="day.day">{{ day.day }}</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边 -->
      <div class="col-side">
        <div class="side-card focus-side">
          <span class="focus-badge">学习建议</span>
          <p class="focus-msg">每天坚持 30 分钟比周末突击 3 小时更有效。保持节奏！</p>
        </div>
        <div class="side-card">
          <div class="side-title">打卡进度</div>
          <div class="side-stat-row"><span>今日已打卡</span><span class="side-stat-val">{{ todayCheckedCount }}/4</span></div>
          <div class="week-dots"><span v-for="d in 7" :key="d" class="wd" :class="{ fill: d <= (stats?.weekDays || 0) }"></span></div>
          <span class="wd-label">本周 {{ stats?.weekDays || 0 }}/7 天已学习</span>
        </div>
        <div class="side-card">
          <div class="side-title">学习贴士</div>
          <ul class="tips-list">
            <li>番茄钟 25+5 最有效</li>
            <li>学完立刻复盘记得更牢</li>
            <li>交叉学习减少疲劳</li>
            <li>每天固定时段形成习惯</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal-card anim-lift">
          <h3>{{ currentCat?.icon }} 打卡：{{ currentCat?.label }}</h3>
          <div class="mf"><label>时长（分钟）</label><input type="number" v-model="duration" min="0" max="600" class="glass-input" /></div>
          <div class="mf"><label>备注</label><input v-model="note" class="glass-input" placeholder="学了什么内容..." /></div>
          <div class="modal-btns">
            <button class="btn-ghost" @click="dialogVisible = false">取消</button>
            <button class="btn-primary" @click="doCheckin">确认打卡</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { learningCheckin, getLearningStats, getLearningCalendar } from '../api/index.js'
import { ElMessage } from 'element-plus'

const categories = [
  { key: 'english', icon: 'ab', label: '英语' },
  { key: 'coding', icon: '<>', label: '编程' },
  { key: 'project', icon: '##', label: '项目' },
  { key: 'exercise', icon: '>>', label: '锻炼' }
]
const today = new Date().toLocaleDateString('zh-CN', { month:'long', day:'numeric', weekday:'short' })
const todayChecked = reactive({})
const stats = ref(null)
const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const calendar = ref([])
const dialogVisible = ref(false); const currentCat = ref(null); const duration = ref(0); const note = ref('')

const todayCheckedCount = computed(() => Object.values(todayChecked).filter(Boolean).length)

const calendarDays = computed(() => {
  const today = new Date().toISOString().slice(0,10); const days = []
  const firstDay = new Date(year.value, month.value - 1, 1); const lastDay = new Date(year.value, month.value, 0)
  const startDow = firstDay.getDay() || 7
  for (let i = 1; i < startDow; i++) days.push({ day: '', isCurrentMonth: false, hasData: false, isToday: false })
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = `${year.value}-${String(month.value).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    const calEntry = calendar.value.find(c => c.date === dateStr)
    days.push({ day: d, isCurrentMonth: true, hasData: calEntry && calEntry.categories?.length > 0, isToday: dateStr === today, tooltip: calEntry?.categories ? Array.from(calEntry.categories).join(', ') : '' })
  }
  return days
})

function openCheckin(cat) { currentCat.value = cat; duration.value = 0; note.value = ''; dialogVisible.value = true }
async function doCheckin() {
  try { await learningCheckin({ category: currentCat.value.key, durationMin: duration.value, note: note.value }); todayChecked[currentCat.value.key] = true; dialogVisible.value = false; ElMessage.success('打卡成功'); loadData() } catch { /* */ }
}
async function loadData() {
  try { const s = await getLearningStats(); stats.value = s.data } catch { /* */ }
  try { const c = await getLearningCalendar(year.value, month.value); calendar.value = c.data || [] } catch { /* */ }
  const today = new Date().toISOString().slice(0,10)
  const todayEntry = calendar.value.find(c => c.date === today)
  if (todayEntry?.categories) { for (const cat of todayEntry.categories) todayChecked[cat] = true }
}
function prevMonth() { if (month.value === 1) { month.value = 12; year.value-- } else month.value--; loadMonth() }
function nextMonth() { const now = new Date(); if (year.value === now.getFullYear() && month.value === now.getMonth() + 1) return; if (month.value === 12) { month.value = 1; year.value++ } else month.value++; loadMonth() }
async function loadMonth() { try { const c = await getLearningCalendar(year.value, month.value); calendar.value = c.data || [] } catch { /* */ } }
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

.mini-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.ms-card { background: #fff; border-radius: 14px; padding: 14px; display: flex; flex-direction: column; align-items: center; gap: 3px; border: 1px solid #E4EAF4; box-shadow: 0 1px 4px rgba(79,140,255,0.03); }
.ms-icon { font-size: 20px; } .ms-val { font-size: 26px; font-weight: 750; color: #4F8CFF; line-height: 1; } .ms-lbl { font-size: 11px; color: #909BB5; font-weight: 550; }

.checkin-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.cc-chip { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 14px 8px; border-radius: 12px; border: 1px solid #E4EAF4; background: #fff; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.cc-chip:hover:not(:disabled) { border-color: #BDD3FF; background: #F0F5FF; transform: translateY(-1px); box-shadow: 0 3px 10px rgba(79,140,255,0.06); }
.cc-chip.done { background: #F0F5FF; border-color: #BDD3FF; opacity: 0.8; cursor: default; }
.cc-icon { font-size: 20px; font-weight: 700; color: #4F8CFF; } .cc-label { font-size: 12px; color: #3C4A6E; font-weight: 530; }
.cc-check { color: #10B981; }

.dual-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.card-half { min-width: 0; }

.cat-bars { display: flex; flex-direction: column; gap: 10px; }
.cat-bar { display: flex; align-items: center; gap: 8px; }
.cat-name { font-size: 12.5px; font-weight: 550; color: #3C4A6E; min-width: 60px; }
.cat-track { flex: 1; height: 6px; background: #F0F4FA; border-radius: 3px; overflow: hidden; }
.cat-fill { height: 100%; background: linear-gradient(90deg, #4F8CFF, #7CCBFF); border-radius: 3px; transition: width 0.5s ease; }
.cat-data { font-size: 11.5px; color: #909BB5; min-width: 30px; text-align: right; }

.cal-head-row { justify-content: center; gap: 8px; }
.cal-month { font-size: 13px; font-weight: 650; color: #1C2640; }
.cal-nav { background: none; border: none; cursor: pointer; color: #909BB5; padding: 3px; border-radius: 4px; display: flex; }
.cal-nav:hover { color: #4F8CFF; background: rgba(79,140,255,0.06); }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; }
.cal-h { text-align: center; font-size: 10px; color: #A0ACC5; padding: 4px 0; font-weight: 550; }
.cal-cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 6px; font-size: 11.5px; color: #3C4A6E; }
.cal-cell.dim { color: #D0D8E8; } .cal-cell.active { background: #4F8CFF; color: #fff; font-weight: 600; }
.cal-cell.today { font-weight: 700; box-shadow: inset 0 0 0 2px #4F8CFF; color: #4F8CFF; }
.cal-cell.active.today { background: #4F8CFF; color: #fff; box-shadow: 0 0 0 2px rgba(79,140,255,0.25); }

/* Sidebar */
.col-side { position: sticky; top: 24px; }
.side-card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 16px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 12px; }
.side-title { font-size: 11.5px; font-weight: 650; color: #909BB5; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 10px; }
.focus-side { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border-color: #BDD3FF; }
.focus-badge { font-size: 10px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 6px; }
.focus-msg { font-size: 12.5px; color: #3C4A6E; margin: 0; line-height: 1.5; }
.side-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 12.5px; color: #5B6780; }
.side-stat-val { font-weight: 650; color: #4F8CFF; }
.week-dots { display: flex; gap: 4px; margin: 8px 0 5px; }
.wd { width: 10px; height: 10px; border-radius: 50%; background: rgba(79,140,255,0.10); }
.wd.fill { background: #4F8CFF; }
.wd-label { font-size: 10.5px; color: #909BB5; }
.tips-list { margin: 0; padding-left: 14px; display: flex; flex-direction: column; gap: 5px; }
.tips-list li { font-size: 12px; color: #5B6780; line-height: 1.4; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.2); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 20px; padding: 24px; width: 360px; max-width: 90vw; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-card h3 { font-size: 15px; font-weight: 650; margin: 0 0 16px; color: #1C2640; }
.mf { margin-bottom: 12px; } .mf label { display: block; font-size: 12px; font-weight: 550; color: #5B6780; margin-bottom: 5px; }
.glass-input { width: 100%; border: 1px solid #E4EAF4; border-radius: 10px; padding: 8px 12px; font-size: 13px; font-family: inherit; color: #1C2640; background: #F8FAFD; outline: none; box-sizing: border-box; }
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.07); }
.modal-btns { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.btn-ghost { padding: 8px 18px; border-radius: 10px; border: 1px solid #E4EAF4; background: #fff; color: #5B6780; cursor: pointer; font-size: 13px; font-family: inherit; }
.btn-primary { padding: 8px 18px; border-radius: 10px; border: none; background: linear-gradient(135deg, #4F8CFF, #3B6FDF); color: #fff; cursor: pointer; font-size: 13px; font-weight: 600; font-family: inherit; box-shadow: 0 2px 8px rgba(79,140,255,0.18); }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(79,140,255,0.25); }

@media (max-width: 800px) { .page-grid { grid-template-columns: 1fr; } .col-side { position: static; } .dual-cards { grid-template-columns: 1fr; } }
</style>
