<template>
  <div class="dash">
    <div class="bg-orbs" aria-hidden="true">
      <div class="orb orb-1"></div><div class="orb orb-2"></div><div class="orb orb-3"></div>
    </div>

    <!-- ====== Hero — 紧凑 ====== -->
    <section class="hero">
      <div class="hero-left">
        <p class="hero-eyebrow">校园 AI 学习伙伴 · {{ todayStr }}</p>
        <h1 class="hero-title">
          {{ greeting }}，<span class="hero-name">{{ displayName }}</span>
          <span class="hero-emoji">{{ waveEmoji }}</span>
        </h1>
        <p class="hero-sub">{{ encourageText }}</p>
      </div>
      <div class="hero-widgets">
        <div class="hw-card">
          <span class="hw-val">{{ stats.streak }}</span><span class="hw-unit">天</span>
          <span class="hw-label">连续打卡</span>
        </div>
        <div class="hw-card">
          <span class="hw-val">{{ stats.weekDays }}</span><span class="hw-unit">/7</span>
          <span class="hw-label">本周学习</span>
        </div>
        <div class="hw-card">
          <span class="hw-val">{{ recent.length }}</span><span class="hw-unit">条</span>
          <span class="hw-label">今日记录</span>
        </div>
      </div>
    </section>

    <!-- ====== 主内容区 — 三栏 ====== -->
    <div class="main-grid">
      <!-- 左栏：统计 + 快捷 + 动态 -->
      <div class="col-main">
        <!-- 统计卡片 — 更紧凑 -->
        <div class="stats-row stagger">
          <StatCard label="连续打卡" :value="stats.streak" sub="天" color="blue">
            <template #icon><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></template>
          </StatCard>
          <StatCard label="本周学习" :value="stats.weekDays" sub="/7" color="cyan">
            <template #icon><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg></template>
          </StatCard>
          <StatCard label="复盘记录" :value="stats.reviewCount" sub="条" color="amber">
            <template #icon><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></template>
          </StatCard>
          <StatCard label="表达训练" :value="stats.expressionCount" sub="条" color="coral">
            <template #icon><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg></template>
          </StatCard>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-row">
          <router-link to="/daily-review" class="q-btn q-blue">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>写复盘
          </router-link>
          <router-link to="/learning" class="q-btn q-cyan">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>学习打卡
          </router-link>
          <router-link to="/expression" class="q-btn q-amber">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>练表达
          </router-link>
          <router-link to="/chat" class="q-btn q-coral">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>问 AI
          </router-link>
          <router-link to="/project" class="q-btn q-teal">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>项目
          </router-link>
        </div>

        <!-- 动态时间轴 -->
        <div class="card feed-card">
          <div class="card-head">
            <h3>最近动态</h3>
            <span class="badge" v-if="recent.length">{{ recent.length }}</span>
          </div>
          <div v-if="recent.length === 0" class="empty-mini">
            <p>还没有记录，开始你的第一次成长之旅 🚀</p>
          </div>
          <div v-else class="feed-list">
            <div v-for="(item, idx) in recent.slice(0, 6)" :key="item.id" class="feed-item anim-up" :style="{ animationDelay: (idx * 0.03) + 's' }">
              <div class="fi-dot" :class="'fi-' + item.type"></div>
              <div class="fi-body">
                <div class="fi-head"><span class="fi-type">{{ typeLabel(item.type) }}</span><span class="fi-time">{{ item.time }}</span></div>
                <p class="fi-text">{{ item.text }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：widgets -->
      <div class="col-side">
        <!-- AI 焦点 -->
        <div class="side-card focus-side">
          <div class="focus-badge">AI 推荐</div>
          <p class="focus-msg" v-if="!hasReview">今天还没写复盘，花5分钟回顾一下</p>
          <p class="focus-msg" v-else-if="stats.streak === 0">开始第一次打卡吧</p>
          <p class="focus-msg" v-else-if="stats.expressionCount === 0">来练一次表达</p>
          <p class="focus-msg" v-else>今天状态很好，继续保持</p>
          <router-link :to="focusLink" class="focus-link">{{ focusAction }} →</router-link>
        </div>

        <!-- Streak widget -->
        <div class="side-card streak-widget">
          <div class="sw-top">
            <span class="sw-fire">🔥</span>
            <span class="sw-num">{{ stats.streak }}</span>
            <span class="sw-label">天连续打卡</span>
          </div>
          <div class="sw-bar-wrap">
            <div class="sw-bar"><div class="sw-fill" :style="{ width: Math.min((stats.streak / 30) * 100, 100) + '%' }"></div></div>
            <span class="sw-target">目标 30 天</span>
          </div>
        </div>

        <!-- Week progress -->
        <div class="side-card">
          <div class="side-title">本周进度</div>
          <div class="week-dots-row">
            <span v-for="d in 7" :key="d" class="wd" :class="{ fill: d <= stats.weekDays }">{{ ['一','二','三','四','五','六','日'][d-1] }}</span>
          </div>
          <div class="side-stat-row">
            <span>学习天数</span><span class="side-stat-val">{{ stats.weekDays }}/7</span>
          </div>
          <div class="side-stat-row">
            <span>复盘次数</span><span class="side-stat-val">{{ stats.reviewCount }}</span>
          </div>
          <div class="side-stat-row">
            <span>表达训练</span><span class="side-stat-val">{{ stats.expressionCount }}</span>
          </div>
        </div>

        <!-- 模块入口 -->
        <div class="side-card">
          <div class="side-title">成长模块</div>
          <div class="mod-links">
            <router-link to="/daily-review" class="mod-link">📝 每日复盘</router-link>
            <router-link to="/learning" class="mod-link">📊 学习追踪</router-link>
            <router-link to="/expression" class="mod-link">🎤 表达训练</router-link>
            <router-link to="/project" class="mod-link">🚀 项目推进</router-link>
            <router-link to="/knowledge" class="mod-link">📚 知识库</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- DEBUG: 数据加载状态 -->
    <div style="position:fixed;bottom:0;left:210px;right:0;z-index:999;background:rgba(0,0,0,0.85);color:#0f0;padding:6px 16px;font-size:11px;font-family:monospace;display:flex;gap:24px">
      <span>🕐 {{ lastLoad }}</span>
      <span>{{ loadStatus }}</span>
      <span>👤 {{ displayName }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute } from 'vue-router'
import StatCard from '../components/StatCard.vue'

const route = useRoute()
const now = new Date()
const weekdays = ['日','一','二','三','四','五','六']
const todayStr = `${now.getMonth()+1}月${now.getDate()}日 周${weekdays[now.getDay()]}`

const greeting = computed(() => {
  const h = now.getHours()
  if (h < 6) return '夜深了'; if (h < 9) return '早上好'; if (h < 12) return '上午好'
  if (h < 14) return '中午好'; if (h < 18) return '下午好'; return '晚上好'
})
const waveEmoji = computed(() => {
  const h = now.getHours()
  if (h < 6) return '🌙'; if (h < 9) return '☀️'; if (h < 12) return '👋'
  if (h < 18) return '🌤️'; return '✨'
})

const userInfo = ref({})
const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '同学')

const encourageTexts = [
  '每一次复盘，都是对未来的自己最好的投资',
  '成长的路上，每一步都算数',
  '今天比昨天进步 1%，一年后就是 37 倍的差距',
  '学习不是为了考试，是为了成为更好的自己',
  '小步快跑，持续精进，你已经很棒了',
]
const encourageText = ref(encourageTexts[0])
const stats = ref({ streak: 0, weekDays: 0, reviewCount: 0, expressionCount: 0 })
const recent = ref([])
const hasReview = computed(() => recent.value.some(r => r.type === 'review'))
const hasProject = computed(() => recent.value.some(r => r.type === 'project'))

function typeLabel(type) {
  return { review:'复盘', learning:'学习', expression:'表达', project:'项目', chat:'聊天' }[type] || type
}
const focusLink = computed(() => {
  if (!hasReview.value) return '/daily-review'; if (stats.value.streak === 0) return '/learning'
  if (stats.value.expressionCount === 0) return '/expression'; return '/chat'
})
const focusAction = computed(() => {
  if (!hasReview.value) return '去写复盘'; if (stats.value.streak === 0) return '去打卡'
  if (stats.value.expressionCount === 0) return '去练习'; return '找 AI 聊聊'
})

const lastLoad = ref('未加载')
const loadStatus = ref('')
async function loadDashboard() {
  try { const s = localStorage.getItem('user'); if (s) userInfo.value = JSON.parse(s) } catch { /* ignore */ }
  try {
    const res = await fetch(`/api/dashboard?_t=${Date.now()}`, { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })
    if (res.ok) {
      const j = await res.json()
      if (j.code === 200) {
        stats.value = j.data.stats; recent.value = j.data.recent || []
        lastLoad.value = new Date().toLocaleTimeString()
        loadStatus.value = `✅ streak=${j.data.stats.streak} weekDays=${j.data.stats.weekDays} recent=${j.data.recent?.length || 0}条`
      } else {
        loadStatus.value = `❌ code=${j.code} msg=${j.msg}`
      }
    } else {
      loadStatus.value = `❌ HTTP ${res.status}`
    }
  } catch (e) { loadStatus.value = '❌ ' + e.message }
}

onMounted(() => {
  encourageText.value = encourageTexts[Math.floor(Math.random() * encourageTexts.length)]
  loadDashboard()
})

// keep-alive 激活时也刷新
onActivated(() => loadDashboard())

// 路由变化时刷新（含首次 immediate）
watch(() => route.path, () => loadDashboard(), { immediate: false })
</script>

<style scoped>
.dash { position: relative; max-width: 1240px; margin: 0 auto; padding: 28px 24px 80px; }

.bg-orbs { position: fixed; inset: 0; pointer-events: none; z-index: 0; overflow: hidden; }
.orb { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.13; }
.orb-1 { width: 350px; height: 350px; background: #7CCBFF; top: -80px; right: -80px; animation: float 8s ease-in-out infinite; }
.orb-2 { width: 260px; height: 260px; background: #BDD3FF; bottom: 10%; left: -80px; animation: float 10s ease-in-out infinite reverse; }
.orb-3 { width: 220px; height: 220px; background: #4F8CFF; top: 50%; right: 25%; animation: float 7s ease-in-out infinite 2s; }

/* ====== Hero ====== */
.hero {
  position: relative; z-index: 1;
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 20px; gap: 24px;
}
.hero-eyebrow { font-size: 11.5px; font-weight: 650; color: #4F8CFF; letter-spacing: 0.06em; margin: 0 0 6px; }
.hero-title { font-size: 28px; font-weight: 750; color: #1C2640; margin: 0 0 4px; letter-spacing: -0.02em; }
.hero-name { background: linear-gradient(135deg, #4F8CFF, #7CCBFF); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.hero-emoji { display: inline-block; animation: waveA 0.6s ease-in-out 0.4s; }
@keyframes waveA { 0%,100%{transform:rotate(0)} 25%{transform:rotate(14deg)} 75%{transform:rotate(-8deg)} }
.hero-sub { font-size: 13.5px; color: #5B6780; margin: 0; font-weight: 450; }
.hero-widgets { display: flex; gap: 12px; flex-shrink: 0; }
.hw-card {
  background: rgba(255,255,255,0.6); backdrop-filter: blur(8px);
  border: 1px solid #E4EAF4; border-radius: 14px;
  padding: 12px 18px; text-align: center; min-width: 70px;
}
.hw-val { font-size: 24px; font-weight: 750; color: #4F8CFF; }
.hw-unit { font-size: 12px; color: #909BB5; margin-left: 2px; }
.hw-label { display: block; font-size: 11px; color: #909BB5; font-weight: 500; margin-top: 2px; }

/* ====== Main Grid ====== */
.main-grid {
  position: relative; z-index: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 18px; align-items: start;
}

/* ====== Left Column ====== */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 14px; }

.quick-row { display: flex; gap: 8px; margin-bottom: 16px; }
.q-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 5px;
  padding: 9px 12px; border-radius: 12px; text-decoration: none;
  font-size: 12.5px; font-weight: 560; transition: all 0.18s;
}
.q-btn:hover { transform: translateY(-1px); }
.q-blue { background: #F0F5FF; color: #3B6FDF; border: 1px solid #BDD3FF; }
.q-blue:hover { background: #E0ECFF; box-shadow: 0 3px 12px rgba(79,140,255,0.1); }
.q-cyan { background: #ECFEFF; color: #0891B2; border: 1px solid #A5F3FC; }
.q-cyan:hover { background: #CFFAFE; box-shadow: 0 3px 12px rgba(6,182,212,0.1); }
.q-amber { background: #FFFBEB; color: #B45309; border: 1px solid #FDE68A; }
.q-amber:hover { background: #FEF3C7; box-shadow: 0 3px 12px rgba(245,158,11,0.1); }
.q-coral { background: #FFF5F3; color: #D14536; border: 1px solid #FFD5CC; }
.q-coral:hover { background: #FFEAE6; box-shadow: 0 3px 12px rgba(255,107,91,0.1); }
.q-teal { background: #F0FDFA; color: #0F766E; border: 1px solid #99F6E4; }
.q-teal:hover { background: #CCFBF1; box-shadow: 0 3px 12px rgba(20,184,166,0.1); }

/* Feed card */
.card {
  background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px);
  border-radius: 16px; padding: 18px 20px;
  border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03);
  margin-bottom: 14px;
}
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-head h3 { font-size: 14px; font-weight: 650; color: #1C2640; margin: 0; }
.badge { font-size: 11px; color: #909BB5; font-weight: 550; padding: 2px 8px; border-radius: 10px; background: rgba(79,140,255,0.05); }
.empty-mini { text-align: center; padding: 20px 0; }
.empty-mini p { font-size: 13px; color: #A0ACC5; margin: 0; }

.feed-item { display: flex; gap: 10px; padding: 9px 12px; border-radius: 10px; transition: background 0.15s; }
.feed-item:hover { background: rgba(79,140,255,0.02); }
.fi-dot { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.fi-review     { background: #E0ECFF; color: #3B6FDF; }
.fi-learning   { background: #CFFAFE; color: #0891B2; }
.fi-expression { background: #FEF3C7; color: #B45309; }
.fi-project    { background: #CCFBF1; color: #0F766E; }
.fi-chat        { background: #F0F4FA; color: #5B6780; }
.fi-body { flex: 1; min-width: 0; }
.fi-head { display: flex; align-items: center; gap: 8px; margin-bottom: 1px; }
.fi-type { font-size: 10.5px; font-weight: 650; color: #4F8CFF; letter-spacing: 0.03em; }
.fi-time { font-size: 10.5px; color: #A0ACC5; }
.fi-text { font-size: 12.5px; color: #3C4A6E; margin: 0; line-height: 1.4; }

/* ====== Right Sidebar ====== */
.col-side { position: sticky; top: 24px; }

.side-card {
  background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px);
  border-radius: 16px; padding: 16px 18px;
  border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03);
  margin-bottom: 12px;
}
.side-title { font-size: 12px; font-weight: 650; color: #909BB5; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 12px; }

/* Focus */
.focus-side { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border-color: #BDD3FF; }
.focus-badge { font-size: 10px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 8px; }
.focus-msg { font-size: 13px; color: #3C4A6E; margin: 0 0 8px; line-height: 1.5; font-weight: 500; }
.focus-link { font-size: 12.5px; color: #4F8CFF; font-weight: 600; text-decoration: none; }
.focus-link:hover { text-decoration: underline; }

/* Streak */
.streak-widget { text-align: center; }
.sw-top { display: flex; align-items: baseline; justify-content: center; gap: 4px; margin-bottom: 10px; }
.sw-fire { font-size: 18px; }
.sw-num { font-size: 32px; font-weight: 750; color: #1C2640; line-height: 1; }
.sw-label { font-size: 12px; color: #909BB5; }
.sw-bar-wrap { display: flex; align-items: center; gap: 8px; }
.sw-bar { flex: 1; height: 6px; background: #F0F4FA; border-radius: 3px; overflow: hidden; }
.sw-fill { height: 100%; background: linear-gradient(90deg, #4F8CFF, #7CCBFF); border-radius: 3px; transition: width 0.5s ease; }
.sw-target { font-size: 10.5px; color: #909BB5; white-space: nowrap; }

/* Week dots */
.week-dots-row { display: flex; gap: 4px; margin-bottom: 12px; }
.wd {
  flex: 1; text-align: center; padding: 5px 0; border-radius: 8px;
  font-size: 10.5px; color: #A0ACC5; background: #F8FAFD; font-weight: 550;
}
.wd.fill { background: #4F8CFF; color: #fff; }

/* Stat rows */
.side-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 5px 0; font-size: 12.5px; color: #5B6780; }
.side-stat-val { font-weight: 650; color: #4F8CFF; }

/* Module links */
.mod-links { display: flex; flex-direction: column; gap: 4px; }
.mod-link {
  display: block; padding: 7px 10px; border-radius: 9px;
  font-size: 12.5px; color: #5B6780; text-decoration: none;
  transition: all 0.15s; font-weight: 500;
}
.mod-link:hover { background: rgba(79,140,255,0.05); color: #3B6FDF; }

@media (max-width: 900px) {
  .main-grid { grid-template-columns: 1fr; }
  .col-side { position: static; }
  .hero { flex-direction: column; }
  .hero-widgets { width: 100%; }
  .hw-card { flex: 1; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .quick-row { flex-wrap: wrap; }
  .q-btn { min-width: calc(33% - 6px); flex: none; }
}
</style>
