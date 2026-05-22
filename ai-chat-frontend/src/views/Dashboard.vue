<template>
  <div class="page">
    <div class="page-header">
      <h1>今日总览</h1>
      <p class="date">{{ today }}</p>
    </div>

    <!-- Stats row -->
    <div class="stats-row">
      <StatCard label="连续打卡" :value="stats.streak" sub="天" />
      <StatCard label="本周学习" :value="stats.weekDays" sub="/ 7 天" />
      <StatCard label="复盘记录" :value="stats.reviewCount" sub="条" />
      <StatCard label="表达训练" :value="stats.expressionCount" sub="条" />
    </div>

    <!-- Quick actions -->
    <div class="quick-actions">
      <router-link to="/daily-review" class="action-card">
        <span class="action-emoji">/</span>
        <span class="action-label">写复盘</span>
      </router-link>
      <router-link to="/learning" class="action-card">
        <span class="action-emoji">+</span>
        <span class="action-label">打卡</span>
      </router-link>
      <router-link to="/expression" class="action-card">
        <span class="action-emoji">"</span>
        <span class="action-label">练表达</span>
      </router-link>
      <router-link to="/chat" class="action-card">
        <span class="action-emoji">#</span>
        <span class="action-label">问 AI</span>
      </router-link>
    </div>

    <!-- Recent records -->
    <div class="section">
      <h2>最近记录</h2>
      <div v-if="recent.length === 0" class="empty">今天还没有记录，开始你的成长之旅吧</div>
      <div v-for="item in recent" :key="item.id" class="record-item">
        <div class="record-left">
          <span :class="['record-dot', item.type]"></span>
          <span class="record-type">{{ typeLabel(item.type) }}</span>
        </div>
        <span class="record-text">{{ item.text }}</span>
        <span class="record-time">{{ item.time }}</span>
      </div>
    </div>

    <!-- Today's focus hint -->
    <div class="section focus-hint">
      <h2>今天可以做什么</h2>
      <ul class="focus-list">
        <li v-if="!hasReview">写今日复盘，反思今天的学习和成长</li>
        <li v-if="stats.streak === 0">开始第一次打卡，迈出学习追踪第一步</li>
        <li v-if="stats.expressionCount === 0">练习一次表达，提升逻辑沟通能力</li>
        <li v-if="!hasProject">更新你的项目进度，让 AI 帮你拆解下一步</li>
        <li v-if="recent.length >= 3">你今天已经很棒了，继续保持节奏</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import StatCard from '../components/StatCard.vue'

const today = new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' })

const stats = ref({ streak: 0, weekDays: 0, reviewCount: 0, expressionCount: 0 })
const recent = ref([])

const hasReview = computed(() => recent.value.some(r => r.type === 'review'))
const hasProject = computed(() => recent.value.some(r => r.type === 'project'))

function typeLabel(type) {
  return { review: '复盘', learning: '学习', expression: '表达', project: '项目', chat: '聊天' }[type] || type
}

onMounted(async () => {
  try {
    const res = await fetch('/api/dashboard', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    if (res.ok) {
      const json = await res.json()
      if (json.code === 200) {
        stats.value = json.data.stats
        recent.value = json.data.recent || []
      }
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page { max-width: 760px; margin: 0 auto; padding: 28px 20px 80px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: #1d1d1f; margin: 0; }
.date { color: #86868b; font-size: 14px; margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
@media (max-width: 600px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }

.quick-actions { display: flex; gap: 10px; margin-bottom: 28px; }
.action-card {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px;
  background: #fff; border-radius: 12px; padding: 16px 8px; text-decoration: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: background 0.12s;
}
.action-card:hover { background: #f5f5f7; }
.action-emoji { font-size: 22px; font-weight: 700; color: #1d1d1f; }
.action-label { font-size: 13px; color: #6e6e73; font-weight: 450; }

.section { background: #fff; border-radius: 14px; padding: 22px 24px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.section h2 { font-size: 15px; font-weight: 600; color: #1d1d1f; margin: 0 0 14px; }
.empty { color: #c7c7cc; font-size: 14px; text-align: center; padding: 24px 0; }

.record-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; }
.record-item + .record-item { border-top: 1px solid #f5f5f7; }
.record-left { display: flex; align-items: center; gap: 8px; min-width: 70px; }
.record-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.record-dot.review { background: #007aff; }
.record-dot.learning { background: #34c759; }
.record-dot.expression { background: #ff9500; }
.record-dot.project { background: #5856d6; }
.record-dot.chat { background: #aeaeb2; }
.record-type { font-size: 12px; color: #86868b; font-weight: 500; }
.record-text { flex: 1; font-size: 13px; color: #3a3a3c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-time { font-size: 12px; color: #c7c7cc; flex-shrink: 0; }

.focus-hint { background: #fafbfc; border: 1px solid #ebebed; }
.focus-list { margin: 0; padding-left: 18px; }
.focus-list li { font-size: 13px; color: #6e6e73; line-height: 1.8; }
</style>
