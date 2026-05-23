<template>
  <div class="history-container">
    <!-- 顶部导航栏 -->
    <header class="history-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon :size="18"><ArrowLeft /></el-icon>
        返回聊天
      </el-button>
      <h1 class="history-title">聊天历史</h1>
      <div class="header-right">
        <span class="user-info">
          <img v-if="user?.avatar" :src="avatarUrl" class="user-avatar" />
          <el-icon v-else :size="16"><User /></el-icon>
          {{ user?.nickname || user?.username }}
        </span>
        <el-button size="small" text @click="handleLogout" class="logout-btn">
          退出登录
        </el-button>
      </div>
    </header>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索聊天记录..."
        :prefix-icon="Search"
        clearable
        size="large"
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="clearSearch"
      />
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchMode" class="search-results">
      <div class="search-info">
        搜索「{{ searchKeyword }}」找到 {{ searchTotal }} 条结果
        <el-button text size="small" @click="clearSearch">清除</el-button>
      </div>
      <div v-if="searchLoading" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div v-else-if="searchRecords.length === 0" class="empty-state">
        <p>未找到匹配的消息</p>
      </div>
      <div v-else class="search-list">
        <div
          v-for="msg in searchRecords"
          :key="msg.id"
          class="search-item"
        >
          <div class="search-item-header">
            <span class="search-role" :class="msg.role">{{ msg.role === 'user' ? '你' : 'AI' }}</span>
            <span class="search-time">{{ formatTime(msg.createdAt) }}</span>
          </div>
          <div class="search-content">{{ msg.content }}</div>
        </div>
        <div class="search-pager" v-if="searchPages > 1">
          <el-pagination
            small
            background
            layout="prev, pager, next"
            :total="searchTotal"
            :page-size="searchSize"
            v-model:current-page="searchPage"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="sessions.length === 0" class="empty-state">
      <el-icon :size="48" color="#909399"><ChatLineSquare /></el-icon>
      <p>暂无聊天记录</p>
      <el-button type="primary" @click="goToChat">开始聊天</el-button>
    </div>

    <!-- 历史记录列表（按会话展示） -->
    <div v-else class="history-list">
      <div class="total-info">共 {{ total }} 条消息，{{ sessions.length }} 个对话</div>
      <div
        v-for="session in sessions"
        :key="session.sessionId"
        class="session-card"
        @click="viewSession(session)"
      >
        <div class="session-header">
          <span class="session-title-text">{{ session.title }}</span>
          <span class="session-count">{{ session.count }} 条消息</span>
        </div>
        <div class="session-footer">
          <span class="session-time">{{ formatTime(session.lastTime) }}</span>
          <el-button text size="small" class="export-btn" @click.stop="handleExport(session.sessionId)">
            <el-icon :size="14"><Download /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 查看某个会话详细记录的对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="'对话记录 - ' + currentSessionTitle"
      width="800px"
      top="5vh"
      class="detail-dialog"
      destroy-on-close
    >
      <div v-if="detailLoading" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
      <div v-else class="detail-messages">
        <div
          v-for="(msg, i) in detailMessages"
          :key="i"
          class="detail-message"
          :class="msg.role"
        >
          <div class="detail-avatar">
            <el-avatar :size="32" :icon="msg.role === 'user' ? User : Avatar" />
          </div>
          <div class="detail-content">
            <div class="detail-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
            <div class="detail-bubble">
              <MarkdownRenderer :content="msg.content" />
            </div>
            <div class="detail-time">{{ formatDetailTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, User, Avatar, ChatLineSquare, Loading, Search, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHistory, getSessionHistory, searchMessages, exportSession } from '../api/index.js'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'

const router = useRouter()
const loading = ref(true)
const sessions = ref([])
const total = ref(0)

// 搜索
const searchKeyword = ref('')
const searchMode = ref(false)
const searchLoading = ref(false)
const searchRecords = ref([])
const searchTotal = ref(0)
const searchPage = ref(1)
const searchSize = ref(10)
const searchPages = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentSessionTitle = ref('')
const detailMessages = ref([])

const user = computed(() => {
  try {
    const userStr = localStorage.getItem('user')
    return userStr ? JSON.parse(userStr) : null
  } catch {
    return null
  }
})

const avatarUrl = computed(() => {
  const u = user.value
  if (!u?.avatar) return ''
  if (u.avatar.startsWith('http')) return u.avatar
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  return base.replace('/api', '') + u.avatar
})

onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const res = await getHistory()
    sessions.value = res.data.sessions
    total.value = res.data.total
  } catch (error) {
    console.error('获取历史记录失败:', error)
  } finally {
    loading.value = false
  }
})

// 搜索
const handleSearch = async () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchMode.value = true
  searchLoading.value = true
  try {
    const res = await searchMessages({ keyword: kw, page: searchPage.value, size: searchSize.value })
    searchRecords.value = res.data.records
    searchTotal.value = res.data.total
    searchPages.value = res.data.pages
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    searchLoading.value = false
  }
}

const clearSearch = () => {
  searchMode.value = false
  searchKeyword.value = ''
  searchRecords.value = []
  searchTotal.value = 0
  searchPage.value = 1
}

const handleExport = async (sessionId, format = 'md') => {
  try {
    await exportSession(sessionId, format)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// 返回聊天页面
const goBack = () => {
  router.push('/chat')
}

// 去聊天
const goToChat = () => {
  router.push('/chat')
}

// 查看某个会话的详细记录
const viewSession = async (session) => {
  currentSessionTitle.value = session.title
  detailVisible.value = true
  detailLoading.value = true

  try {
    const res = await getSessionHistory(session.sessionId)
    detailMessages.value = res.data.messages
  } catch (error) {
    console.error('获取会话详情失败:', error)
    ElMessage.error('获取会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {
    // 用户取消退出
  })
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = date.toDateString() === yesterday.toDateString()

  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')

  if (isToday) return `今天 ${hours}:${minutes}`
  if (isYesterday) return `昨天 ${hours}:${minutes}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${hours}:${minutes}`
}

// 格式化详细时间
const formatDetailTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}
</script>

<style scoped>
.history-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg, #faf9f6);
}

/* 搜索栏 */
.search-bar {
  padding: 16px 28px;
  background: var(--surface, #fff);
  border-bottom: 1px solid var(--border, #e8e4dd);
}
.search-input :deep(.el-input__wrapper) {
  background: var(--bg, #faf9f6);
  border-radius: 10px;
  box-shadow: none !important;
}
.search-results {
  flex: 1;
  overflow-y: auto;
  padding: 20px 28px;
  max-width: 700px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
.search-info {
  font-size: 13px;
  color: var(--text-secondary, #787570);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-list { display: flex; flex-direction: column; gap: 10px; }
.search-item {
  background: var(--surface, #fff);
  border: 1px solid var(--border, #e8e4dd);
  border-radius: 10px;
  padding: 14px 18px;
}
.search-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.search-role {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.search-role.user { background: var(--accent-bg, rgba(74,106,138,0.08)); color: var(--accent, #4a6a8a); }
.search-role.assistant { background: #f0ece6; color: var(--text-secondary, #787570); }
.search-content {
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--text, #3c3a37);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-pager { margin-top: 16px; display: flex; justify-content: center; }

.history-header {
  display: flex;
  align-items: center;
  padding: 14px 28px;
  background: var(--surface, #fff);
  border-bottom: 1px solid var(--border, #e8e4dd);
}

.back-btn { font-size: 14px; color: var(--accent, #4a6a8a); }
.back-btn:hover { color: var(--accent-hover, #4a4ac4); }

.history-title {
  flex: 1; margin: 0; font-size: 16px; font-weight: 650;
  color: var(--text, #3c3a37); text-align: center;
}

.header-right { display: flex; align-items: center; gap: 14px; }

.user-info {
  display: flex; align-items: center; gap: 6px;
  color: var(--text-secondary, #787570); font-size: 13px; font-weight: 500;
}
.user-avatar { width: 24px; height: 24px; border-radius: 50%; object-fit: cover; }
.logout-btn { color: var(--text-muted, #a09c95); font-size: 13px; }
.logout-btn:hover { color: #e08585 !important; }

.loading-state, .empty-state {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px; color: var(--text-muted, #a09c95);
}
.empty-state p { margin: 0; font-size: 14px; }

.history-list {
  flex: 1; overflow-y: auto; padding: 28px;
  max-width: 700px; width: 100%; margin: 0 auto; box-sizing: border-box;
}
.total-info { font-size: 13px; color: var(--text-muted, #a09c95); margin-bottom: 16px; }

.session-card {
  background: var(--surface, #fff);
  border-radius: 12px;
  padding: 18px 22px;
  margin-bottom: 10px;
  cursor: pointer;
  border: 1px solid var(--border, #e8e4dd);
  transition: box-shadow 0.15s, transform 0.15s;
}
.session-card:hover {
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
  transform: translateY(-1px);
}

.session-header {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 8px;
}
.session-title-text {
  font-size: 14.5px; font-weight: 600; color: var(--text, #3c3a37);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; margin-right: 12px;
}
.session-count {
  font-size: 12px; color: var(--accent, #4a6a8a); font-weight: 550;
  background: var(--accent-bg, rgba(91,91,214,0.06)); padding: 3px 10px; border-radius: 12px;
}
.session-time { font-size: 12px; color: var(--text-muted, #a09c95); }

.detail-messages { max-height: 60vh; overflow-y: auto; padding: 8px 0; }
.detail-messages::-webkit-scrollbar { width: 4px; }
.detail-messages::-webkit-scrollbar-thumb { background: #d5d0c8; border-radius: 4px; }

.detail-message {
  display: flex; gap: 12px; margin-bottom: 24px;
}
.detail-message.user { flex-direction: row-reverse; }
.detail-avatar { flex-shrink: 0; }
.detail-avatar .el-avatar { background: #f0ece6; border: 1px solid var(--border, #e8e4dd); }
.detail-message.user .detail-avatar .el-avatar { background: #4a6a8a; border: none; }

.detail-content { max-width: 70%; }
.detail-message.user .detail-content { text-align: right; }

.detail-role { font-size: 11.5px; color: var(--text-muted, #a09c95); margin-bottom: 4px; font-weight: 550; }

.detail-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.detail-message.user .detail-bubble {
  background: #4a6a8a;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.detail-message.assistant .detail-bubble {
  background: var(--bg, #faf9f6);
  color: var(--text, #3c3a37);
  border: 1px solid var(--border, #e8e4dd);
  border-bottom-left-radius: 4px;
}

.detail-time { font-size: 11px; color: var(--text-muted, #a09c95); margin-top: 4px; }
.detail-message.user .detail-time { text-align: right; }
</style>
