<template>
  <aside class="sidenav dot-bg">
    <!-- ==========================================
         学生证卡片
         ========================================== -->
    <div class="student-card">
      <div class="sc-top">
        <div class="sc-avatar">{{ initial }}</div>
        <div class="sc-info">
          <span class="sc-name">{{ displayName }}</span>
          <span class="sc-role">AI 成长学员</span>
        </div>
      </div>
      <div class="sc-divider"></div>
      <div class="sc-meta">
        <span class="sc-greeting">{{ greeting }}</span>
        <span class="sc-vibe">{{ vibeText }}</span>
      </div>
    </div>

    <!-- ==========================================
         导航菜单
         ========================================== -->
    <nav class="nav-list">
      <router-link to="/dashboard" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg></span>
        <span class="nav-label">总览</span>
      </router-link>

      <router-link to="/chat" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg></span>
        <span class="nav-label">AI 聊天</span>
        <span class="nav-badge">AI</span>
      </router-link>

      <!-- 分隔 — 细线带间距 -->
      <div class="nav-sep">
        <span class="nav-sep-label">成长模块</span>
      </div>

      <router-link to="/daily-review" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
        <span class="nav-label">每日复盘</span>
      </router-link>

      <router-link to="/learning" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></span>
        <span class="nav-label">学习追踪</span>
      </router-link>

      <router-link to="/expression" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg></span>
        <span class="nav-label">表达训练</span>
      </router-link>

      <router-link to="/project" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg></span>
        <span class="nav-label">项目推进</span>
      </router-link>

      <router-link to="/knowledge" class="nav-item">
        <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg></span>
        <span class="nav-label">知识库</span>
      </router-link>
    </nav>

    <!-- ==========================================
         底部 — 学习状态
         ========================================== -->
    <div class="nav-footer">
      <div class="streak-card">
        <div class="streak-row">
          <span class="streak-fire">🔥</span>
          <span class="streak-num">{{ streak }}</span>
          <span class="streak-unit">天连续</span>
        </div>
        <div class="week-track">
          <div class="week-dots">
            <span v-for="d in 7" :key="d" class="wd" :class="{ fill: d <= weekDays }"></span>
          </div>
          <span class="week-label">本周 {{ weekDays }}/7</span>
        </div>
      </div>
      <p class="footer-quote">每一天，都在成为更好的自己</p>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const streak = ref(0)
const weekDays = ref(0)
const userInfo = ref({})

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '同学')
const initial = computed(() => displayName.value.charAt(0).toUpperCase())

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了 🌙'
  if (h < 9)  return '早上好 ☀️'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好 🌤️'
  return '晚上好 ✨'
})

const vibeText = computed(() => {
  const h = new Date().getHours()
  if (h >= 6 && h < 9)  return '适合晨间学习'
  if (h >= 9 && h < 12) return '黄金专注时段'
  if (h >= 14 && h < 17) return '适合深度学习'
  if (h >= 19 && h < 22) return '晚间复盘时间'
  return '保持学习节奏'
})

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
      }
    }
  } catch { /* ignore */ }
}

onMounted(loadStats)
onActivated(loadStats)

// 每次路由变化时刷新数据
watch(() => route.path, () => loadStats())
</script>

<style scoped>
/* ==========================================
   Sidebar base
   ========================================== */
.sidenav {
  position: fixed;
  left: 0; top: 0; bottom: 0;
  width: 210px;
  display: flex;
  flex-direction: column;
  z-index: 50;
  background: linear-gradient(175deg, #F6F9FE 0%, #F0F5FF 50%, #F4F8FD 100%);
  border-right: 1px solid #E4EAF4;
}

/* ==========================================
   学生证卡片
   ========================================== */
.student-card {
  margin: 14px 12px 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.70);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(79,140,255,0.10);
  box-shadow: 0 2px 12px rgba(79,140,255,0.04);
}
.sc-top {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sc-avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F8CFF 0%, #7CCBFF 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(79,140,255,0.25);
}
.sc-info { display: flex; flex-direction: column; min-width: 0; }
.sc-name {
  font-size: 14px; font-weight: 650; color: #1C2640;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sc-role {
  font-size: 11px; color: #7C8BA8; font-weight: 480;
}
.sc-divider {
  height: 1px;
  margin: 10px 0;
  background: linear-gradient(90deg, transparent, #BDD3FF, transparent);
}
.sc-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sc-greeting {
  font-size: 11.5px; color: #5B6780; font-weight: 520;
}
.sc-vibe {
  font-size: 10.5px; color: #4F8CFF; font-weight: 550;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(79,140,255,0.07);
}

/* ==========================================
   导航列表
   ========================================== */
.nav-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 12px;
  overflow-y: auto;
}

/* 分隔标签 */
.nav-sep {
  padding: 12px 12px 4px;
  margin-top: 4px;
}
.nav-sep-label {
  font-size: 10px;
  font-weight: 600;
  color: #909BB5;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* 导航项 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border-radius: 12px;
  text-decoration: none;
  color: #5B6780;
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.22s cubic-bezier(0.4,0,0.2,1);
  position: relative;
}
.nav-item:hover {
  background: rgba(79,140,255,0.05);
  color: #3B6FDF;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(79,140,255,0.06);
}

/* 激活态 */
.nav-item.router-link-active {
  background: linear-gradient(135deg, rgba(79,140,255,0.10), rgba(124,203,255,0.08));
  color: #2D52B5;
  font-weight: 600;
  box-shadow: 0 0 0 1px rgba(79,140,255,0.16), 0 4px 14px rgba(79,140,255,0.10);
}
.nav-item.router-link-active::before {
  content: '';
  position: absolute;
  left: -12px; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px;
  border-radius: 0 3px 3px 0;
  background: #4F8CFF;
}
.nav-item.router-link-active .nav-icon {
  color: #3B6FDF;
}

.nav-icon {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px;
  border-radius: 9px;
  flex-shrink: 0;
  transition: color 0.2s;
}
.nav-label { flex: 1; }

.nav-badge {
  font-size: 9.5px; font-weight: 750;
  padding: 1.5px 6px; border-radius: 5px;
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; letter-spacing: 0.04em;
}

/* ==========================================
   底部
   ========================================== */
.nav-footer { padding: 10px 12px 14px; }

.streak-card {
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(79,140,255,0.08);
  border-radius: 14px;
  padding: 12px 14px;
  margin-bottom: 8px;
}
.streak-row {
  display: flex; align-items: baseline; gap: 3px; margin-bottom: 8px;
}
.streak-fire { font-size: 14px; }
.streak-num {
  font-size: 22px; font-weight: 750; color: #1C2640; line-height: 1;
}
.streak-unit {
  font-size: 11px; color: #7C8BA8; font-weight: 480;
}
.week-track {
  display: flex; align-items: center; justify-content: space-between;
}
.week-dots { display: flex; gap: 4px; }
.wd {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: rgba(79,140,255,0.10);
  transition: all 0.3s;
}
.wd.fill {
  background: #4F8CFF;
  box-shadow: 0 0 6px rgba(79,140,255,0.25);
}
.week-label {
  font-size: 10.5px; color: #909BB5; font-weight: 500;
}

.footer-quote {
  margin: 0;
  font-size: 10.5px;
  color: #A0ACC5;
  font-weight: 480;
  text-align: center;
}
</style>
