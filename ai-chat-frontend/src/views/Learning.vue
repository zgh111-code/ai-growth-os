<template>
  <div class="page">
    <div class="page-header">
      <h1>学习追踪</h1>
      <p>持续行动，看见成长</p>
    </div>

    <!-- Stats cards -->
    <div class="stats-row" v-if="stats">
      <div class="stat-card">
        <span class="stat-num">{{ stats.streak }}</span>
        <span class="stat-label">连续打卡天数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ stats.weekDays }}</span>
        <span class="stat-label">本周打卡天数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ stats.weekTotal }}</span>
        <span class="stat-label">本周打卡次数</span>
      </div>
    </div>

    <!-- Checkin buttons -->
    <div class="card checkin-card">
      <h2>今日打卡</h2>
      <div class="checkin-grid">
        <button v-for="cat in categories" :key="cat.key"
          :class="['checkin-btn', { done: todayChecked[cat.key] }]"
          :disabled="todayChecked[cat.key]"
          @click="openCheckin(cat)">
          <span class="cat-icon">{{ cat.icon }}</span>
          <span class="cat-label">{{ cat.label }}</span>
          <span class="cat-check" v-if="todayChecked[cat.key]">✓</span>
        </button>
      </div>
      <el-dialog v-model="dialogVisible" :title="'打卡: ' + currentCat?.label" width="360px">
        <div class="dialog-field">
          <label>时长（分钟）</label>
          <el-input-number v-model="duration" :min="0" :max="600" />
        </div>
        <div class="dialog-field">
          <label>备注</label>
          <el-input v-model="note" placeholder="学了什么内容..." />
        </div>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="doCheckin">确认打卡</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- Weekly stats -->
    <div class="card" v-if="stats">
      <h2>本周分类统计</h2>
      <div class="category-stats">
        <div v-for="cat in categories" :key="cat.key" class="cat-row">
          <span class="cat-name">{{ cat.icon }} {{ cat.label }}</span>
          <span class="cat-count">{{ stats.categoryCount?.[cat.key] || 0 }} 次</span>
          <span class="cat-minutes">{{ stats.categoryMinutes?.[cat.key] || 0 }} min</span>
        </div>
      </div>
    </div>

    <!-- Monthly calendar -->
    <div class="card calendar-card">
      <h2>
        <button class="month-nav" @click="prevMonth">&lt;</button>
        {{ year }}年{{ month }}月
        <button class="month-nav" @click="nextMonth">&gt;</button>
      </h2>
      <div class="calendar-grid">
        <div class="cal-header" v-for="d in ['一','二','三','四','五','六','日']" :key="d">{{ d }}</div>
        <div v-for="(day, i) in calendarDays" :key="i"
          :class="['cal-day', {
            'has-data': day.hasData,
            'is-today': day.isToday,
            'dimmed': !day.isCurrentMonth
          }]"
          :title="day.tooltip">
          {{ day.day }}
        </div>
      </div>
    </div>
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

const todayChecked = reactive({})
const stats = ref(null)
const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const calendar = ref([])

const dialogVisible = ref(false)
const currentCat = ref(null)
const duration = ref(0)
const note = ref('')

const calendarDays = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const days = []
  const firstDay = new Date(year.value, month.value - 1, 1)
  const lastDay = new Date(year.value, month.value, 0)
  const startDow = firstDay.getDay() || 7

  for (let i = 1; i < startDow; i++) {
    days.push({ day: '', isCurrentMonth: false, hasData: false, isToday: false })
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = `${year.value}-${String(month.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const calEntry = calendar.value.find(c => c.date === dateStr)
    days.push({
      day: d,
      isCurrentMonth: true,
      hasData: calEntry && calEntry.categories && calEntry.categories.length > 0,
      isToday: dateStr === today,
      tooltip: calEntry && calEntry.categories ? Array.from(calEntry.categories).join(', ') : ''
    })
  }
  return days
})

function openCheckin(cat) {
  currentCat.value = cat
  duration.value = 0
  note.value = ''
  dialogVisible.value = true
}

async function doCheckin() {
  try {
    await learningCheckin({
      category: currentCat.value.key,
      durationMin: duration.value,
      note: note.value
    })
    todayChecked[currentCat.value.key] = true
    dialogVisible.value = false
    ElMessage.success('打卡成功')
    loadData()
  } catch { /* handled */ }
}

async function loadData() {
  try {
    const s = await getLearningStats()
    stats.value = s.data
  } catch { /* ignore */ }
  try {
    const c = await getLearningCalendar(year.value, month.value)
    calendar.value = c.data || []
  } catch { /* ignore */ }
  // Mark today's checked categories
  const today = new Date().toISOString().slice(0, 10)
  const todayEntry = calendar.value.find(c => c.date === today)
  if (todayEntry?.categories) {
    for (const cat of todayEntry.categories) {
      todayChecked[cat] = true
    }
  }
}

function prevMonth() {
  if (month.value === 1) { month.value = 12; year.value-- }
  else month.value--
  loadMonth()
}
function nextMonth() {
  const now = new Date()
  if (year.value === now.getFullYear() && month.value === now.getMonth() + 1) return
  if (month.value === 12) { month.value = 1; year.value++ }
  else month.value++
  loadMonth()
}
async function loadMonth() {
  try {
    const c = await getLearningCalendar(year.value, month.value)
    calendar.value = c.data || []
  } catch { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px 80px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0; }
.page-header p { color: #86868b; margin: 4px 0 0; font-size: 14px; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 14px; padding: 20px 16px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-num { display: block; font-size: 36px; font-weight: 700; color: #007aff; }
.stat-label { font-size: 12px; color: #86868b; margin-top: 4px; display: block; }

.card { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card h2 { font-size: 16px; font-weight: 600; margin: 0 0 16px; color: #1d1d1f; display: flex; align-items: center; gap: 12px; }

.checkin-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.checkin-btn { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px 8px; border: 1px solid #e5e5ea; border-radius: 12px; background: #fff; cursor: pointer; transition: all 0.2s; }
.checkin-btn:hover:not(:disabled) { border-color: #007aff; background: #f5f9ff; }
.checkin-btn.done { background: #f5f9ff; border-color: #c7e0ff; opacity: 0.7; }
.checkin-btn:disabled { cursor: default; }
.cat-icon { font-size: 22px; font-weight: 700; color: #007aff; }
.cat-label { font-size: 13px; color: #3a3a3c; }
.cat-check { color: #34c759; font-weight: 700; font-size: 14px; }
@media (max-width: 400px) { .checkin-grid { grid-template-columns: repeat(2, 1fr); } }

.dialog-field { margin-bottom: 14px; }
.dialog-field label { display: block; font-size: 13px; font-weight: 500; color: #86868b; margin-bottom: 6px; }

.category-stats { display: flex; flex-direction: column; gap: 10px; }
.cat-row { display: flex; align-items: center; gap: 12px; padding: 10px 14px; background: #fafafa; border-radius: 10px; }
.cat-name { flex: 1; font-size: 14px; font-weight: 500; }
.cat-count { font-size: 13px; color: #007aff; font-weight: 600; }
.cat-minutes { font-size: 13px; color: #86868b; }

.month-nav { background: none; border: none; font-size: 16px; cursor: pointer; color: #86868b; padding: 0 8px; }
.month-nav:hover { color: #007aff; }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.cal-header { text-align: center; font-size: 11px; color: #86868b; padding: 8px 0; font-weight: 500; }
.cal-day { text-align: center; padding: 10px 0; font-size: 13px; border-radius: 8px; color: #c7c7cc; }
.cal-day.is-today { font-weight: 700; color: #007aff; background: #e8f4fd; }
.cal-day.has-data { background: #007aff; color: #fff; font-weight: 600; border-radius: 8px; }
.cal-day.is-today.has-data { background: #007aff; color: #fff; box-shadow: 0 0 0 2px #007aff33; }
.cal-day.dimmed:not(.has-data) { color: #e5e5ea; }
</style>
