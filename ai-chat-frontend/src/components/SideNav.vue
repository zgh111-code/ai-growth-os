<template>
  <aside class="sidenav">
    <div class="sidenav-brand">
      <div class="brand-icon">G</div>
      <span class="brand-text">Growth OS</span>
    </div>

    <nav class="sidenav-items">
      <router-link to="/dashboard" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
        </span>
        <span class="item-label">总览</span>
      </router-link>

      <router-link to="/chat" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
        </span>
        <span class="item-label">AI 聊天</span>
      </router-link>

      <router-link to="/daily-review" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        </span>
        <span class="item-label">每日复盘</span>
      </router-link>

      <router-link to="/learning" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </span>
        <span class="item-label">学习追踪</span>
      </router-link>

      <router-link to="/expression" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        </span>
        <span class="item-label">表达训练</span>
      </router-link>

      <router-link to="/project" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
        </span>
        <span class="item-label">项目推进</span>
      </router-link>

      <router-link to="/knowledge" class="sidenav-item">
        <span class="item-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
        </span>
        <span class="item-label">知识库</span>
      </router-link>
    </nav>

    <div class="sidenav-footer">
      <div class="user-info" v-if="username">
        <span class="user-avatar">{{ username.charAt(0) }}</span>
        <span class="user-name">{{ username }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const username = ref('')

onMounted(() => {
  const stored = localStorage.getItem('username')
  if (stored) username.value = stored
})
</script>

<style scoped>
.sidenav {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 220px;
  background: #f8f8f9;
  border-right: 1px solid #ebebed;
  display: flex;
  flex-direction: column;
  z-index: 50;
  overflow-y: auto;
}

.sidenav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px 16px;
}

.brand-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #1d1d1f;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.brand-text {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
}

.sidenav-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 10px;
}

.sidenav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  text-decoration: none;
  color: #6e6e73;
  font-size: 14px;
  font-weight: 450;
  transition: all 0.12s;
}

.sidenav-item:hover {
  background: #ececee;
  color: #1d1d1f;
}

.sidenav-item.router-link-active {
  background: #e8e8ea;
  color: #1d1d1f;
  font-weight: 550;
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.sidenav-footer {
  padding: 14px 18px;
  border-top: 1px solid #ebebed;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
}

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #d6d6db;
  color: #1d1d1f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  color: #6e6e73;
  font-weight: 450;
}
</style>
