<template>
  <aside class="sidenav">
    <!-- 噪点纹理层 -->
    <div class="noise-grain" aria-hidden="true"></div>
    <!-- 顶部光晕 -->
    <div class="glow-top" aria-hidden="true"></div>
    <!-- 底部光晕 -->
    <div class="glow-bottom" aria-hidden="true"></div>

    <!-- ===== 用户卡片 ===== -->
    <div
      v-motion
      :initial="{ opacity: 0, y: -6, scale: 0.97 }"
      :enter="{ opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 300, damping: 28, delay: 0.02 } }"
      class="user-card"
    >
      <div class="uc-top">
        <div class="uc-avatar" :class="avatarGradient">
          <span class="uc-initial">{{ initial }}</span>
          <span class="uc-dot"></span>
        </div>
        <div class="uc-info">
          <span class="uc-name">{{ displayName }}</span>
          <span class="uc-badge">
            <Sparkles :size="10" />
            AI 学员
          </span>
        </div>
      </div>
      <p class="uc-motto">{{ encouragement }}</p>
    </div>

    <!-- ===== 导航 ===== -->
    <nav class="nav-list">
      <template v-for="(item, i) in navItems" :key="item.to || item.sep">
        <!-- 分隔线 -->
        <div v-if="item.sep" class="nav-sep">
          <span class="nav-sep-label">{{ item.sep }}</span>
        </div>

        <router-link
          v-else
          :to="item.to"
          class="nav-item"
          v-motion
          :initial="{ opacity: 0, x: -12 }"
          :enter="{ opacity: 1, x: 0, transition: { type: 'spring', stiffness: 280, damping: 26, delay: 0.06 + i * 0.035 } }"
        >
          <component :is="item.icon" class="nav-icon" :size="18" :stroke-width="1.8" />
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </router-link>
      </template>
    </nav>

    <!-- ===== 底部 ===== -->
    <div
      v-motion
      :initial="{ opacity: 0, y: 6 }"
      :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 28, delay: 0.35 } }"
      class="nav-footer"
    >
      <div class="footer-card">
        <div class="fc-ring">
          <svg width="46" height="46" viewBox="0 0 46 46">
            <circle cx="23" cy="23" r="19" fill="none" stroke="rgba(79,140,255,0.08)" stroke-width="3" />
            <circle
              cx="23" cy="23" r="19"
              fill="none"
              stroke="url(#ringGrad)"
              stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="ringLen"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 23 23)"
              class="ring-progress"
            />
            <!-- 小发光点 -->
            <circle
              cx="23" cy="4"
              r="2.5"
              fill="#7CCBFF"
              class="ring-dot"
              :style="{ transform: `rotate(${ringAngle}deg)`, transformOrigin: '23px 23px' }"
            />
            <defs>
              <linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#4F8CFF" />
                <stop offset="100%" stop-color="#7CCBFF" />
              </linearGradient>
            </defs>
          </svg>
          <div class="ring-center">
            <span class="ring-num">{{ streak }}</span>
            <span class="ring-unit">天</span>
          </div>
        </div>
        <div class="fc-stats">
          <div class="fcs-item">
            <span class="fcs-val">{{ weekDays }}<span class="fcs-suffix">/7</span></span>
            <span class="fcs-label">本周学习</span>
          </div>
          <div class="fcs-item">
            <span class="fcs-val">{{ todayCount }}</span>
            <span class="fcs-label">今日记录</span>
          </div>
        </div>
      </div>

      <!-- 周追踪 -->
      <div class="week-track">
        <span
          v-for="(active, i) in weekActivity"
          :key="i"
          class="wd"
          :class="{
            fill: active,
            today: (i + 1) === todayDot
          }"
        ></span>
      </div>

      <p class="footer-quote">每一天，都在成为更好的自己</p>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  LayoutDashboard, MessageSquare, PenLine, TrendingUp,
  Smile, FolderGit2, BookOpen, Sparkles
} from '@lucide/vue'

const route = useRoute()

// ===== 响应式数据 =====
const streak = ref(0)
const weekDays = ref(0)
const weekActivity = ref([false,false,false,false,false,false,false])
const todayCount = ref(0)
const userInfo = ref({})

// ===== 进度环计算 =====
const ringLen = 2 * Math.PI * 19  // ≈ 119.38
const ringOffset = computed(() => {
  const pct = Math.min(weekDays.value / 7, 1)
  return ringLen * (1 - pct)
})
const ringAngle = computed(() => {
  const pct = Math.min(weekDays.value / 7, 1)
  return pct * 360
})

// ===== 用户相关 =====
const todayDot = computed(() => new Date().getDay() || 7)

const displayName = computed(() =>
  userInfo.value?.nickname || userInfo.value?.username || '同学'
)
const initial = computed(() => displayName.value.charAt(0).toUpperCase())

const avatarGradient = computed(() => {
  const list = ['ag-iris', 'ag-ocean', 'ag-amber', 'ag-mint']
  return list[(displayName.value.charCodeAt(0) || 0) % list.length]
})

const encouragement = computed(() => {
  const pool = [
    '今天也在成为更好的自己 ✨',
    '每一次努力都不会被浪费 🌱',
    '你离理想又近了一步 🚀',
    '保持好奇，持续成长 💡',
    '学习是最高级的复利 📈',
  ]
  return pool[new Date().getDate() % pool.length]
})

// ===== 导航配置 =====
const navItems = [
  { to: '/dashboard',     label: '总览',       icon: LayoutDashboard },
  { to: '/chat',          label: 'AI 聊天',    icon: MessageSquare,  badge: 'AI' },
  { sep: '成长模块' },
  { to: '/daily-review',  label: '每日复盘',   icon: PenLine },
  { to: '/learning',      label: '学习追踪',   icon: TrendingUp },
  { to: '/expression',    label: '表达训练',   icon: Smile },
  { to: '/project',       label: '项目推进',   icon: FolderGit2 },
  { to: '/knowledge',     label: '知识库',     icon: BookOpen },
]

// ===== 数据加载 =====
async function loadStats() {
  try {
    const stored = localStorage.getItem('user')
    if (stored) userInfo.value = JSON.parse(stored)
  } catch { /* ignore */ }

  try {
    const res = await fetch(`/api/dashboard?_t=${Date.now()}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    if (res.ok) {
      const json = await res.json()
      if (json.code === 200 && json.data?.stats) {
        streak.value = json.data.stats.streak || 0
        weekDays.value = json.data.stats.weekDays || 0
        if (json.data.stats.weekActivity) {
          weekActivity.value = json.data.stats.weekActivity
        }
        todayCount.value = json.data.stats.todayCount || 0
      }
    }
  } catch { /* ignore */ }
}

onMounted(loadStats)
watch(() => route.path, () => loadStats())
</script>

<style scoped>
/* ==========================================
   Design Tokens
   ========================================== */
:root {
  --s-color-accent: #4F8CFF;
  --s-color-accent-soft: #7CCBFF;
  --s-color-accent-cyan: #7CCBFF;
  --s-color-text-primary: #1f2937;
  --s-color-text-secondary: #6B7280;
  --s-color-text-muted: #9CA3AF;
  --s-color-glass-bg: rgba(248, 250, 254, 0.75);
  --s-color-glass-border: rgba(79, 140, 255, 0.07);
  --s-radius-md: 14px;
  --s-radius-lg: 18px;
  --s-shadow-card: 0 1px 2px rgba(79,140,255,0.02), 0 4px 12px rgba(79,140,255,0.04);
  --s-shadow-card-hover: 0 2px 4px rgba(79,140,255,0.04), 0 8px 20px rgba(79,140,255,0.06);
}

/* ==========================================
   Container
   ========================================== */
.sidenav {
  --nav-width: 190px;

  position: fixed;
  inset: 0 auto 0 0;
  width: var(--nav-width);
  display: flex;
  flex-direction: column;
  z-index: 50;
  isolation: isolate;
  border-right: 1px solid var(--s-color-glass-border);
  box-shadow:
    2px 0 32px rgba(79, 140, 255, 0.03),
    8px 0 64px rgba(79, 140, 255, 0.02);
  overflow: hidden;
}

/* 玻璃效果独立层 — blur 在 ::before 上，不影响文字清晰度 */
.sidenav::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background: rgba(248, 250, 254, 0.97);
  backdrop-filter: blur(10px) saturate(1.05);
  -webkit-backdrop-filter: blur(10px) saturate(1.05);
}

/* ---- 噪点纹理 ---- */
.noise-grain {
  position: absolute; inset: 0;
  opacity: 0.028;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.82' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 200px 200px;
  pointer-events: none;
  z-index: 0;
}

/* ---- 光晕 ---- */
.glow-top {
  position: absolute;
  top: -60px; right: -40px;
  width: 160px; height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124,203,255,0.12) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}
.glow-bottom {
  position: absolute;
  bottom: -40px; left: -30px;
  width: 140px; height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124,203,255,0.08) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

/* ==========================================
   User Card
   ========================================== */
.user-card {
  position: relative; z-index: 1;
  margin: 14px 12px 6px;
  padding: 15px;
  border-radius: var(--s-radius-lg);
  background: rgba(255,255,255,0.52);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(79,140,255,0.07);
  box-shadow: var(--s-shadow-card);
  transition: box-shadow 0.3s ease;
}
.user-card:hover {
  box-shadow: var(--s-shadow-card-hover);
}

.uc-top {
  display: flex;
  align-items: center;
  gap: 11px;
}

/* 头像 */
.uc-avatar {
  position: relative;
  width: 38px; height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(79,140,255,0.18);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.user-card:hover .uc-avatar {
  transform: scale(1.04);
  box-shadow: 0 6px 18px rgba(79,140,255,0.24);
}

.uc-avatar.ag-iris  { background: linear-gradient(135deg, #4F8CFF, #93C5FD); }
.uc-avatar.ag-ocean { background: linear-gradient(135deg, #2563EB, #67E8F9); }
.uc-avatar.ag-amber { background: linear-gradient(135deg, #F59E0B, #FCD34D); }
.uc-avatar.ag-mint  { background: linear-gradient(135deg, #059669, #6EE7B7); }

.uc-initial {
  font-size: 15px; font-weight: 700; color: #fff;
  line-height: 1;
}

/* 在线状态点 */
.uc-dot {
  position: absolute;
  bottom: 1px; right: 1px;
  width: 9px; height: 9px;
  border-radius: 50%;
  background: #22C55E;
  border: 2px solid rgba(255,255,255,0.92);
  box-shadow: 0 0 6px rgba(34,197,94,0.35);
}

.uc-info {
  display: flex; flex-direction: column; gap: 3px;
  min-width: 0;
}
.uc-name {
  font-size: 13.5px; font-weight: 650; color: var(--s-color-text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  line-height: 1.2;
}
.uc-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 10.5px; font-weight: 600; color: #4F8CFF;
  padding: 1.5px 8px; border-radius: 20px;
  background: rgba(79,140,255,0.07);
  width: fit-content;
  letter-spacing: 0.01em;
}

/* 鼓励语 */
.uc-motto {
  margin: 10px 0 0;
  font-size: 11px; color: var(--s-color-text-secondary);
  font-weight: 460; text-align: center;
  line-height: 1.4;
}

/* ==========================================
   Navigation
   ========================================== */
.nav-list {
  position: relative; z-index: 1;
  flex: 1;
  display: flex; flex-direction: column; gap: 2px;
  padding: 8px 12px;
  overflow-y: auto;
}

/* 分隔 */
.nav-sep {
  padding: 16px 14px 6px;
}
.nav-sep-label {
  font-size: 9.5px; font-weight: 650;
  color: var(--s-color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.09em;
}

/* 导航项 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 13px;
  border-radius: var(--s-radius-md);
  text-decoration: none;
  color: var(--s-color-text-secondary);
  font-size: 13.5px; font-weight: 500;
  transition: all 0.24s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.nav-item:hover {
  background: rgba(79,140,255,0.05);
  color: #3B6FDF;
  transform: translateY(-1px);
  box-shadow: 0 2px 10px rgba(79,140,255,0.05);
}

/* Active 态 */
.nav-item.router-link-active {
  background: linear-gradient(135deg, rgba(79,140,255,0.10), rgba(124,203,255,0.06));
  color: #2563EB;
  font-weight: 630;
  box-shadow:
    0 0 0 1px rgba(79,140,255,0.12),
    0 3px 12px rgba(79,140,255,0.09);
}
.nav-item.router-link-active::before {
  content: '';
  position: absolute;
  left: -12px; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px;
  border-radius: 0 4px 4px 0;
  background: linear-gradient(180deg, #4F8CFF, #7CCBFF);
  box-shadow: 0 0 8px rgba(79,140,255,0.3);
}

.nav-icon {
  flex-shrink: 0;
  transition: color 0.2s, filter 0.2s;
}
.nav-item.router-link-active .nav-icon {
  filter: drop-shadow(0 1px 4px rgba(79,140,255,0.25));
}

.nav-label { flex: 1; }

.nav-badge {
  font-size: 9px; font-weight: 750;
  padding: 2px 7px; border-radius: 5px;
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; letter-spacing: 0.03em;
  line-height: 1.3;
  box-shadow: 0 1px 4px rgba(79,140,255,0.25);
}

/* ==========================================
   Footer
   ========================================== */
.nav-footer {
  position: relative; z-index: 1;
  padding: 8px 12px 16px;
}

.footer-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 11px 13px;
  border-radius: var(--s-radius-lg);
  background: rgba(255,255,255,0.48);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(79,140,255,0.06);
  box-shadow: var(--s-shadow-card);
  margin-bottom: 10px;
}

/* 进度环 */
.fc-ring {
  position: relative;
  width: 46px; height: 46px;
  flex-shrink: 0;
}
.ring-progress {
  transition: stroke-dashoffset 0.9s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.ring-dot {
  transition: transform 0.9s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
.ring-num {
  font-size: 16px; font-weight: 750; color: var(--s-color-text-primary);
  line-height: 1;
}
.ring-unit {
  font-size: 9px; color: var(--s-color-text-secondary);
  font-weight: 500; margin-top: 1px;
}

/* 小数据 */
.fc-stats {
  display: flex; flex-direction: column; gap: 5px;
}
.fcs-item {
  display: flex; align-items: baseline; gap: 5px;
}
.fcs-val {
  font-size: 15px; font-weight: 700; color: var(--s-color-text-primary);
  line-height: 1.2;
}
.fcs-suffix {
  font-size: 11px; font-weight: 500; color: var(--s-color-text-muted);
  margin-left: 1px;
}
.fcs-label {
  font-size: 11px; color: var(--s-color-text-secondary);
  font-weight: 460;
}

/* 周点 */
.week-track {
  display: flex; align-items: center; justify-content: center;
  gap: 6px; margin-bottom: 8px;
}
.wd {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: rgba(79,140,255,0.07);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.wd.fill {
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  box-shadow: 0 0 5px rgba(79,140,255,0.2);
}
.wd.today {
  box-shadow: 0 0 8px rgba(79,140,255,0.4);
  transform: scale(1.2);
}

.footer-quote {
  margin: 0;
  font-size: 10px; color: var(--s-color-text-muted);
  font-weight: 460; text-align: center;
}
</style>
