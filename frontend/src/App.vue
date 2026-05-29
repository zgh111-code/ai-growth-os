<template>
  <div class="app-layout" :class="{ 'has-sidenav': showNav }">
    <!-- 全局氛围光晕 -->
    <div class="ambient-orb ambient-orb-top" aria-hidden="true"></div>
    <div class="ambient-orb ambient-orb-bottom" aria-hidden="true"></div>
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
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1C2640;
  background: linear-gradient(170deg, #F4F7FD 0%, #FAF5FE 50%, #F0F7FF 100%);
}

#app { height: 100%; }

.app-layout { min-height: 100%; display: flex; }

.main-full {
  flex: 1;
  min-height: 100vh;
}

.main-with-nav {
  flex: 1;
  margin-left: 190px;
  min-height: 100vh;
  padding-bottom: 70px;
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
    background: rgba(255,255,255,0.88);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border-top: 1px solid #E4EAF4;
    padding: 6px 0 env(safe-area-inset-bottom, 6px);
    z-index: 100;
  }
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 6px 14px;
  text-decoration: none;
  border-radius: 10px;
  transition: all 0.2s;
}
.nav-item.router-link-active .nav-icon,
.nav-item.router-link-active .nav-label {
  color: #5B9BD5;
}
.nav-icon { display: flex; color: #B5B1AC; transition: color 0.2s; }
.nav-label { font-size: 10px; font-weight: 550; color: #B5B1AC; transition: color 0.2s; }

::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #C4D6F5; border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: #A8C0E8; }
::selection { background: rgba(79,140,255,0.12); color: #2D52B5; }
</style>
