<template>
  <div class="chat-layout">
    <!-- ========== 左侧：会话列表 ========== -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">AI 聊天</h2>
        <div class="sidebar-actions">
          <el-button type="primary" size="small" @click="handleNewSession" :disabled="loading">
            <el-icon :size="14"><Plus /></el-icon>
            新建会话
          </el-button>
          <el-button size="small" plain @click="handleCleanSessions" :disabled="loading" title="清理空会话">
            <el-icon :size="14"><Delete /></el-icon>
            清理
          </el-button>
        </div>
      </div>

      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session)"
        >
          <div class="session-info">
            <el-icon :size="16"><ChatDotSquare /></el-icon>
            <span class="session-title" :title="session.title">{{ session.title }}</span>
          </div>
          <el-button
            class="delete-btn"
            size="small"
            text
            type="danger"
            @click.stop="handleDeleteSession(session)"
          >
            <el-icon :size="14"><Delete /></el-icon>
          </el-button>
        </div>

        <!-- 空状态 -->
        <div v-if="sessions.length === 0" class="empty-sessions">
          <el-icon :size="36" color="#c0c4cc"><ChatLineSquare /></el-icon>
          <p>暂无会话，点击上方按钮新建</p>
        </div>
      </div>

      <!-- 知识库 (折叠) -->
      <div class="kb-section" :class="{ open: kbOpen }">
        <div class="kb-toggle" @click="kbOpen = !kbOpen">
          <el-icon :size="14"><Collection /></el-icon>
          <span>知识库</span>
          <span class="kb-count" v-if="kbFiles.length">({{ kbFiles.length }})</span>
          <el-icon :size="12" class="kb-arrow">{{ kbOpen ? '▲' : '▼' }}</el-icon>
        </div>
        <div v-if="kbOpen" class="kb-body">
          <input type="file" ref="kbInput" class="file-hidden" @change="handleKbUpload" accept=".txt,.md,.java,.py,.js,.html,.css,.json,.xml,.yml,.sql" />
          <div class="kb-files">
            <div v-for="f in kbFiles" :key="f.filename" class="kb-file" :title="f.filename">
              <span class="kb-fname">{{ f.title || f.filename }}</span>
              <el-button text size="small" @click.stop="handleKbDelete(f.filename)"><el-icon :size="12"><Delete /></el-icon></el-button>
            </div>
            <div v-if="kbFiles.length === 0" class="kb-empty">点击上传文档</div>
          </div>
          <el-button size="small" class="kb-upload-btn" @click="$refs.kbInput.click()" :loading="kbUploading">
            <el-icon :size="12"><Upload /></el-icon> 上传
          </el-button>
        </div>
      </div>

      <div class="sidebar-footer">
        <span class="user-info">
          <img v-if="user?.avatar" :src="avatarUrl" class="sidebar-avatar" />
          <el-icon v-else :size="16"><User /></el-icon>
          {{ user?.nickname || user?.username }}
        </span>
        <div class="sidebar-footer-actions">
          <el-button size="small" text @click="userProfileVisible = true" title="设置">
            <el-icon :size="16"><Setting /></el-icon>
          </el-button>
          <el-button size="small" text @click="handleLogout">退出</el-button>
        </div>
      </div>
    </aside>

    <!-- ========== 右侧：聊天区域 ========== -->
    <main class="chat-main">
      <!-- 顶部栏 -->
      <header class="chat-header">
        <h3 class="current-title">{{ currentSession?.title || 'AI 聊天' }}</h3>
        <div class="header-btns">
          <el-button size="small" plain @click="handleExportChat" :disabled="!currentSessionId">
            <el-icon :size="14"><Download /></el-icon>
          </el-button>
<el-button type="primary" size="small" plain @click="goToHistory">
            <el-icon :size="14"><Clock /></el-icon>
            历史记录
          </el-button>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="message-area" ref="messageAreaRef">
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon :size="48" color="#909399"><ChatLineSquare /></el-icon>
          <p>开始和 AI 对话吧！</p>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-wrapper"
          :class="msg.role"
        >
          <div class="avatar">
            <el-avatar :size="36" :icon="msg.role === 'user' ? User : Avatar" />
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <MarkdownRenderer :content="msg.content" />
            </div>
            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>

        <!-- 加载中动画 -->
        <div v-if="loading" class="message-wrapper assistant">
          <div class="avatar">
            <el-avatar :size="36" icon="Avatar" />
          </div>
          <div class="message-content">
            <div class="message-bubble loading-bubble">
              <span class="dot-pulse"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <footer class="chat-footer">
        <div class="input-wrapper">
          <div class="input-top">
            <el-switch v-model="ragMode" size="small" active-text="RAG" :disabled="loading || kbFiles.length === 0" />
          </div>
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            :placeholder="ragMode ? '基于知识库提问...' : '输入消息，按 Enter 发送...'"
            :disabled="loading"
            @keyup.enter="handleSend"
            resize="none"
          />
          <el-button
            type="primary"
            :loading="loading"
            class="send-btn"
            @click="handleSend"
          >
            {{ loading ? '发送中...' : '发 送' }}
          </el-button>
        </div>
      </footer>
    </main>
  </div>

  <!-- 用户设置对话框 -->
  <UserProfile v-model="userProfileVisible" @profile-updated="handleProfileUpdated" />
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, Avatar, ChatLineSquare, ChatDotSquare,
  Plus, Delete, Clock, Setting, Download, Collection, Upload
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sendMessage,
  createSession,
  getSessionList,
  deleteSession,
  cleanSessions,
  createStreamConnection,
  exportSession,
  getSessionHistory
} from '../api/index.js'
import request from '../api/index.js'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import UserProfile from '../components/UserProfile.vue'

const router = useRouter()
const messageAreaRef = ref(null)
const inputText = ref('')
const loading = ref(false)
const messages = reactive([])
const sessions = reactive([])
const currentSessionId = ref(null)
const currentSession = ref(null)
const userProfileVisible = ref(false)
let abortController = null

// 知识库
const kbOpen = ref(false)
const kbFiles = ref([])
const kbUploading = ref(false)
const ragMode = ref(false)

const loadKbFiles = async () => {
  try {
    const res = await request({ url: '/knowledge/files', method: 'get' })
    kbFiles.value = res.data || []
  } catch { /* ignore */ }
}

const handleKbUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  kbUploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await request({ url: '/knowledge/upload', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success(res.msg)
    loadKbFiles()
  } catch { ElMessage.error('上传失败') }
  finally { kbUploading.value = false; e.target.value = '' }
}

const handleKbDelete = async (filename) => {
  try {
    await ElMessageBox.confirm(`删除 ${filename}？`, '提示', { type: 'warning' })
    await request({ url: '/knowledge/delete', method: 'delete', params: { filename } })
    ElMessage.success('已删除')
    loadKbFiles()
  } catch { /* cancel */ }
}

// 页面加载时获取知识库文件
loadKbFiles()

// 获取当前用户信息
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
  // 如果已经是完整 URL 就直接用，否则拼接后端地址
  if (u.avatar.startsWith('http')) return u.avatar
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  return base.replace('/api', '') + u.avatar
})

// 检查登录并加载会话列表
onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await loadSessions()
})

// 加载会话列表
const loadSessions = async () => {
  try {
    const res = await getSessionList()
    sessions.length = 0
    sessions.push(...res.data)
    // 如果有会话，默认选中第一个
    if (sessions.length > 0 && !currentSessionId.value) {
      switchSession(sessions[0])
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 新建会话
const handleNewSession = async () => {
  try {
    const res = await createSession()
    const newSession = res.data
    // 插入到列表最前面
    sessions.unshift(newSession)
    // 切换到新会话
    switchSession(newSession)
    ElMessage.success('已创建新会话')
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  }
}

// 切换会话
const switchSession = async (session) => {
  currentSessionId.value = session.id
  currentSession.value = session
  inputText.value = ''
  messages.length = 0

  // 加载该会话的历史消息
  try {
    const res = await getSessionHistory(session.id)
    messages.push(...res.data.messages)
    await scrollToBottom()
  } catch (error) {
    console.error('加载会话历史失败:', error)
  }
}

// 删除会话
const handleDeleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${session.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSession(session.id)
    // 从列表中移除
    const index = sessions.findIndex(s => s.id === session.id)
    if (index !== -1) {
      sessions.splice(index, 1)
    }
    // 如果删除的是当前会话，切换到第一个
    if (currentSessionId.value === session.id) {
      if (sessions.length > 0) {
        switchSession(sessions[0])
      } else {
        currentSessionId.value = null
        currentSession.value = null
        messages.length = 0
      }
    }
    ElMessage.success('已删除')
  } catch {
    // 用户取消删除
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messageAreaRef.value) {
    messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')

  if (isToday) {
    return `${hours}:${minutes}`
  }
  return `${date.getMonth() + 1}/${date.getDate()} ${hours}:${minutes}`
}

// 发送消息（如果没有会话，自动创建新会话）
const handleSend = async () => {
  const content = inputText.value.trim()
  if (!content) return
  if (loading.value) return

  // 标记是否是新创建的会话（用于失败时回滚）
  let isNewlyCreated = false

  // 如果没有选中会话，自动创建一个新会话
  if (!currentSessionId.value) {
    try {
      const res = await createSession()
      const newSession = res.data
      sessions.unshift(newSession)
      switchSession(newSession)
      isNewlyCreated = true
    } catch (error) {
      console.error('创建会话失败:', error)
      ElMessage.error('创建会话失败，请重试')
      return
    }
  }

  // 添加用户消息到界面
  messages.push({
    role: 'user',
    content: content,
    createdAt: new Date().toISOString()
  })
  inputText.value = ''
  await scrollToBottom()

  // 创建一个占位的 AI 回复消息（初始为空字符串）
  const assistantMsg = {
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString()
  }
  messages.push(assistantMsg)
  await scrollToBottom()

  // RAG 模式：直接调 RAG 接口
  if (ragMode.value) {
    loading.value = true
    try {
      const res = await request({ url: '/chat/rag', method: 'post', data: { question: content } })
      assistantMsg.content = res.data.reply
      loading.value = false
      if (!assistantMsg.content.trim()) {
        const idx = messages.indexOf(assistantMsg)
        if (idx !== -1) messages.splice(idx, 1)
      }
      scrollToBottom()
    } catch {
      loading.value = false
      ElMessage.error('RAG 问答失败')
      const idx = messages.indexOf(assistantMsg)
      if (idx !== -1) messages.splice(idx, 1)
    }
    return
  }

  // ===== 使用 SSE 流式接收 AI 回复 =====
  loading.value = true
  abortController = createStreamConnection(
    { content, sessionId: currentSessionId.value },
    {
      onMessage: (text) => {
        assistantMsg.content += text
        scrollToBottom()
      },
      onDone: () => {
        loading.value = false
        abortController = null
        if (!assistantMsg.content.trim()) {
          const idx = messages.indexOf(assistantMsg)
          if (idx !== -1) messages.splice(idx, 1)
        }
        if (currentSession.value && currentSession.value.title === '新对话') {
          loadSessions()
        }
        scrollToBottom()
      },
      onError: (error) => {
        loading.value = false
        abortController = null
        console.error('流式请求失败:', error)
        if (!assistantMsg.content.trim()) {
          const idx = messages.indexOf(assistantMsg)
          if (idx !== -1) messages.splice(idx, 1)
        }
        ElMessage.error('发送失败：' + (error.message || '请检查网络连接'))
        if (isNewlyCreated && currentSessionId.value) {
          deleteSession(currentSessionId.value).then(() => {
            const index = sessions.findIndex(s => s.id === currentSessionId.value)
            if (index !== -1) sessions.splice(index, 1)
          }).catch(() => {})
          currentSessionId.value = null
          currentSession.value = null
          messages.length = 0
        }
        scrollToBottom()
      }
    }
  )
}

// 清理空会话
const handleCleanSessions = async () => {
  try {
    await ElMessageBox.confirm(
      '将删除所有标题为"新对话"且没有聊天记录的空会话，确定继续吗？',
      '清理空会话',
      {
        confirmButtonText: '确定清理',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const res = await cleanSessions()
    ElMessage.success(res.msg || '清理完成')
    // 重新加载会话列表
    await loadSessions()
  } catch (error) {
    // 用户取消或请求失败
    if (error !== 'cancel') {
      console.error('清理空会话失败:', error)
    }
  }
}

// 跳转到历史记录页面
const goToHistory = () => {
  router.push('/history')
}

// 导出当前会话
const handleExportChat = async () => {
  if (!currentSessionId.value) return
  try {
    await exportSession(currentSessionId.value, 'md')
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 用户信息修改后，强制刷新 computed user 的显示
const handleProfileUpdated = (updatedUser) => {
  // user 是 computed，会自动从 localStorage 重新读取
  // 但为了即时刷新，可以手动触发
  if (updatedUser.nickname) {
    try {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.nickname = updatedUser.nickname
        localStorage.setItem('user', JSON.stringify(user))
      }
    } catch {
      // ignore
    }
  }
}
</script>

<style scoped>
/* ========== 整体 ========== */
.chat-layout {
  height: 100vh;
  display: flex;
  background: var(--bg, #faf9f6);
}

/* ========== 侧栏 ========== */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: #2a2a2f;
  display: flex;
  flex-direction: column;
  color: #fff;
}

.sidebar-header {
  padding: 22px 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.sidebar-title {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  color: #e8e4f0;
  letter-spacing: -0.2px;
}

.sidebar-actions { display: flex; gap: 6px; }
.sidebar-actions .el-button {
  flex: 1;
  border-radius: 8px;
  font-weight: 550;
  font-size: 13px;
}
.sidebar-actions .el-button--primary {
  background: var(--accent, #4a6a8a);
  border: none;
}
.sidebar-actions .el-button--primary:hover { background: var(--accent-hover, #3d5a75); }
.sidebar-actions .el-button--default {
  color: #a09cb0;
  border-color: rgba(255,255,255,0.08);
  background: transparent;
}
.sidebar-actions .el-button--default:hover {
  color: #d5d0e0;
  border-color: rgba(255,255,255,0.15);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 1px;
  color: #a09cb0;
  font-size: 13.5px;
  transition: background 0.12s, color 0.12s;
}
.session-item:hover { background: rgba(255,255,255,0.05); color: #d5d0e0; }
.session-item.active {
  background: var(--sidebar-active, #36343f);
  color: #e8e4f0;
}

.session-info { display: flex; align-items: center; gap: 8px; overflow: hidden; min-width: 0; }
.session-info .el-icon { flex-shrink: 0; opacity: 0.5; }
.session-item.active .session-info .el-icon { opacity: 0.9; }

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  opacity: 0;
  flex-shrink: 0;
  color: #706b80 !important;
}
.delete-btn:hover { color: #e08585 !important; }
.session-item:hover .delete-btn { opacity: 1; }

.empty-sessions {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 48px 16px; color: #706b80; gap: 8px;
}
.empty-sessions p { margin: 0; font-size: 13px; }

/* 知识库 */
.kb-section { border-top: 1px solid rgba(255,255,255,0.06); }
.kb-toggle {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 16px; cursor: pointer; font-size: 13px; color: #a09cb0;
  user-select: none;
}
.kb-toggle:hover { color: #d5d0e0; }
.kb-count { font-size: 11px; opacity: 0.7; }
.kb-arrow { margin-left: auto; font-style: normal; }
.kb-body { padding: 0 12px 10px; }
.kb-files { max-height: 120px; overflow-y: auto; margin-bottom: 6px; }
.kb-file {
  display: flex; align-items: center; justify-content: space-between;
  padding: 3px 6px; font-size: 12px; color: #a09cb0; border-radius: 4px;
}
.kb-file:hover { background: rgba(255,255,255,0.04); }
.kb-fname { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-empty { font-size: 12px; color: #706b80; padding: 4px 6px; }
.kb-upload-btn { width: 100%; border-radius: 6px; font-size: 12px; }
.file-hidden { display: none; }

.input-top { display: flex; align-items: center; margin-bottom: 6px; padding-left: 4px; }

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sidebar-footer .user-info {
  display: flex; align-items: center; gap: 6px;
  color: #a09cb0; font-size: 13px;
}
.sidebar-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255,255,255,0.1);
}
.sidebar-footer .el-button { color: #706b80; }
.sidebar-footer-actions .el-button:hover { color: #e08585; }
.sidebar-footer-actions .el-button:first-child:hover { color: #b5b0e8; }

/* ========== 聊天区 ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--surface, #fff);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  border-bottom: 1px solid var(--border, #e8e4dd);
}

.current-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text, #3c3a37);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 消息区 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 28px 0 12px;
  display: flex;
  flex-direction: column;
  background: var(--bg, #faf9f6);
}
.message-area > * { padding-left: 28px; padding-right: 28px; }

.empty-state {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px; color: var(--text-muted, #a09c95);
}
.empty-state p { margin: 0; font-size: 14px; }

/* 气泡 */
.message-wrapper {
  display: flex;
  gap: 10px;
  max-width: 70%;
  margin-bottom: 18px;
}
.message-wrapper.user { align-self: flex-end; flex-direction: row-reverse; }
.message-wrapper.assistant { align-self: flex-start; }
.message-wrapper .avatar { flex-shrink: 0; margin-top: 2px; }

.message-wrapper.user .avatar :deep(.el-avatar) {
  background: #4a6a8a !important;
}
.message-wrapper.assistant .avatar :deep(.el-avatar) {
  background: #787570 !important;
}

.message-content { display: flex; flex-direction: column; gap: 3px; }
.message-wrapper.user .message-content { align-items: flex-end; }

.message-bubble {
  padding: 10px 16px;
  border-radius: 16px;
  font-size: 14.5px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.message-wrapper.user .message-bubble {
  background: #4a6a8a;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-wrapper.assistant .message-bubble {
  background: var(--surface, #fff);
  color: var(--text, #3c3a37);
  border: 1px solid var(--border, #e8e4dd);
  border-bottom-left-radius: 4px;
}

.message-time { font-size: 11px; color: var(--text-muted, #a09c95); padding: 0 4px; }

/* 加载 */
.loading-bubble { min-width: 60px; display: flex; align-items: center; justify-content: center; gap: 4px; }
.dot-pulse {
  width: 6px; height: 6px; background: var(--accent, #4a6a8a);
  border-radius: 50%; animation: dotPulse 1.2s infinite;
}
.dot-pulse:nth-child(2) { animation-delay: 0.15s; }
.dot-pulse:nth-child(3) { animation-delay: 0.3s; }
@keyframes dotPulse {
  0%, 100% { opacity: 0.2; transform: scale(0.6); }
  50% { opacity: 1; transform: scale(1); }
}

/* 输入区 */
.chat-footer {
  padding: 14px 28px 20px;
  background: var(--surface, #fff);
  border-top: 1px solid var(--border, #e8e4dd);
}

.input-wrapper {
  display: flex; gap: 10px; align-items: flex-end;
  max-width: 860px; margin: 0 auto; width: 100%;
}
.input-wrapper .el-textarea { flex: 1; }
.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 12px !important;
  padding: 10px 16px;
  font-size: 14.5px;
  font-family: var(--font, system-ui, sans-serif);
  background: var(--bg, #faf9f6);
  border: 1px solid var(--border, #e8e4dd);
  box-shadow: none !important;
  resize: none;
}
.input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: var(--accent, #4a6a8a);
  background: #fff;
  box-shadow: 0 0 0 3px var(--accent-bg, rgba(91,91,214,0.06)) !important;
}

.send-btn {
  height: 44px;
  width: 80px;
  font-size: 14px;
  font-weight: 550;
  border-radius: 10px;
  background: var(--accent, #4a6a8a);
  border: none;
}
.send-btn:hover { background: var(--accent-hover, #3d5a75); }
</style>
