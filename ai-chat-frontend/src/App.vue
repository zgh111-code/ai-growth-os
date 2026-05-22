<template>
  <div class="app-layout" :class="{ 'has-sidenav': showNav }">
    <SideNav v-if="showNav" />
    <main :class="showNav ? 'main-with-nav' : 'main-full'">
      <router-view />
    </main>
    <nav v-if="showNav" class="bottom-nav">
      <router-link to="/dashboard" class="nav-item">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
        </span>
        <span class="nav-label">总览</span>
      </router-link>
      <router-link to="/chat" class="nav-item">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
        </span>
        <span class="nav-label">聊天</span>
      </router-link>
      <router-link to="/daily-review" class="nav-item">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        </span>
        <span class="nav-label">复盘</span>
      </router-link>
      <router-link to="/learning" class="nav-item">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </span>
        <span class="nav-label">学习</span>
      </router-link>
      <router-link to="/project" class="nav-item">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
        </span>
        <span class="nav-label">项目</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideNav from './components/SideNav.vue'

const route = useRoute()
const showNav = computed(() => {
  const token = localStorage.getItem('token')
  if (!token) return false
  const path = route.path
  return !['/login', '/register'].includes(path)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #f5f5f7;
  color: #1d1d1f;
}

#app { height: 100%; }

.app-layout { min-height: 100%; display: flex; }

.main-full {
  flex: 1;
  min-height: 100%;
}

.main-with-nav {
  flex: 1;
  margin-left: 220px;
  min-height: 100%;
  padding-bottom: 70px; /* room for mobile bottom nav */
}

@media (max-width: 768px) {
  .main-with-nav { margin-left: 0; }
}

/* Bottom nav — mobile only */
.bottom-nav {
  display: none;
}
@media (max-width: 768px) {
  .bottom-nav {
    position: fixed;
    bottom: 0; left: 0; right: 0;
    display: flex;
    justify-content: space-around;
    background: rgba(255,255,255,0.92);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-top: 1px solid #e5e5ea;
    padding: 6px 0 env(safe-area-inset-bottom, 6px);
    z-index: 100;
  }
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.15s;
}
.nav-item.router-link-active .nav-icon,
.nav-item.router-link-active .nav-label {
  color: #007aff;
}
.nav-icon { display: flex; color: #86868b; }
.nav-label { font-size: 10px; font-weight: 500; color: #86868b; }

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #d0d5dd; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #b0b5bd; }
::selection { background: rgba(64, 158, 255, 0.2); color: #1d1d1f; }

.el-button { --el-border-radius-base: 8px; }
.el-input__wrapper, .el-textarea__inner { border-radius: 10px !important; }
.el-dialog { --el-dialog-border-radius: 14px; }
.el-message { --el-message-border-radius: 10px; border-radius: 10px !important; }
</style>
