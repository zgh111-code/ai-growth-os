<template>
  <div class="chat-layout">
    <!-- ====== 左侧：会话列表 ====== -->
    <aside class="chat-sidebar dot-bg">
      <!-- Logo -->
      <div class="sb-brand">
        <div class="sb-logo">G</div>
        <div>
          <div class="sb-name">Growth OS</div>
          <div class="sb-sub">AI 学习伙伴</div>
        </div>
      </div>

      <!-- 会话操作 -->
      <div class="sb-actions">
        <button class="sb-btn sb-btn-primary" @click="handleNewSession" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新对话
        </button>
        <button class="sb-btn sb-btn-ghost" @click="handleCleanSessions" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
          清理
        </button>
      </div>

      <!-- 会话列表 -->
      <div class="sb-sessions">
        <div v-if="sessions.length === 0" class="sb-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" opacity="0.4"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <p>点击上方开始对话</p>
        </div>
        <div
          v-for="session in sessions"
          :key="session.id"
          class="sb-session"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session)"
        >
          <div class="sbs-left">
            <div class="sbs-dot"></div>
            <span class="sbs-title">{{ session.title }}</span>
          </div>
          <button class="sbs-del" @click.stop="handleDeleteSession(session)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
          </button>
        </div>
      </div>

      <!-- 知识库折叠 -->
      <div class="sb-kb">
        <div class="sb-kb-toggle" @click="kbOpen = !kbOpen">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
          <span>知识库</span>
          <span v-if="kbFiles.length" class="kb-count">({{ kbFiles.length }})</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="kb-arrow" :class="{ open: kbOpen }"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
        <div v-if="kbOpen" class="sb-kb-body">
          <input type="file" ref="kbInput" class="hidden" @change="handleKbUpload" accept=".txt,.md,.java,.py,.js,.html,.css,.json,.xml,.yml,.sql" />
          <div class="kb-files">
            <div v-for="f in kbFiles" :key="f.filename" class="kb-file">
              <span class="kb-fname">{{ f.title || f.filename }}</span>
              <button class="kb-fdel" @click.stop="handleKbDelete(f.filename)">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <p v-if="kbFiles.length === 0" class="kb-empty">暂无文档</p>
          </div>
          <button class="sb-btn sb-btn-ghost sb-btn-sm" @click="$refs.kbInput.click()" :disabled="kbUploading">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            上传文档
          </button>
        </div>
      </div>

      <!-- 底部用户 -->
      <div class="sb-user">
        <img v-if="user?.avatar" :src="avatarUrl" class="sbu-avatar" />
        <div v-else class="sbu-avatar sbu-avatar-fallback">{{ (user?.nickname || user?.username || '?').charAt(0) }}</div>
        <span class="sbu-name">{{ user?.nickname || user?.username }}</span>
        <button class="sbu-btn" @click="userProfileVisible = true" title="设置">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
        </button>
        <button class="sbu-btn sbu-btn-logout" @click="handleLogout" title="退出">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </aside>

    <!-- ====== 右侧：聊天区域 ====== -->
    <main class="chat-main">
      <!-- 顶部栏 -->
      <header class="chat-topbar glass-card-solid">
        <div class="ctb-left">
          <div class="ctb-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          </div>
          <h3 class="ctb-title">{{ currentSession?.title || 'AI 聊天' }}</h3>
        </div>
        <div class="ctb-right">
          <button class="ctb-btn" @click="handleExportChat" :disabled="!currentSessionId">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <button class="ctb-btn" @click="goToHistory">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            历史
          </button>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="chat-messages" ref="messageAreaRef">
        <!-- 空状态 -->
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="ce-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/><path d="M8 9h8M8 13h6" stroke-linecap="round"/></svg>
          </div>
          <h3 class="ce-title">Hi，开始学习吧</h3>
          <p class="ce-sub">有问题随时问我，我是你的 AI 学习伙伴</p>
          <div class="ce-hints">
            <button class="ce-hint" @click="inputText = '帮我制定今天的学习计划'; handleSend()">📋 帮我制定学习计划</button>
            <button class="ce-hint" @click="inputText = '如何提高学习效率？'; handleSend()">🧠 如何提高学习效率</button>
            <button class="ce-hint" @click="inputText = '用费曼学习法给我讲一下今天的知识点'; handleSend()">📖 用费曼学习法讲解知识点</button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="msg-list">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="msg-row"
            :class="msg.role === 'user' ? 'msg-you' : 'msg-ai'"
          >
            <div class="msg-avatar">
              <div v-if="msg.role === 'user'" class="msg-av msg-av-you">{{ (user?.nickname || user?.username || 'Y').charAt(0) }}</div>
              <div v-else class="msg-av msg-av-bot">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              </div>
            </div>
            <div class="msg-body">
              <div class="msg-bubble" :class="msg.role === 'user' ? 'bubble-you' : 'bubble-ai'">
                <MarkdownRenderer :content="msg.content" />
              </div>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>

          <!-- 加载动画 -->
          <div v-if="loading" class="msg-row msg-ai">
            <div class="msg-avatar">
              <div class="msg-av msg-av-bot">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              </div>
            </div>
            <div class="msg-body">
              <div class="msg-bubble bubble-ai typing">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <footer class="chat-input-area">
        <div class="cia-row">
          <!-- RAG 开关 -->
          <label class="rag-toggle">
            <input type="checkbox" v-model="ragMode" :disabled="loading || kbFiles.length === 0" />
            <span class="rag-slider"></span>
            <span class="rag-label">知识库检索</span>
          </label>
        </div>
        <div class="cia-box glass-card">
          <textarea
            v-model="inputText"
            class="cia-textarea"
            :placeholder="ragMode ? '基于知识库提问...' : '想问什么？按 Enter 发送...'"
            :disabled="loading"
            rows="1"
            @keyup.enter="handleSend"
            @input="autoResize"
            ref="inputEl"
          ></textarea>
          <button
            class="cia-send"
            :class="{ active: inputText.trim() && !loading }"
            :disabled="loading || !inputText.trim()"
            @click="handleSend"
          >
            <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><circle cx="12" cy="12" r="10" stroke-opacity="0.25"/><path d="M12 2a10 10 0 019.95 9" stroke-linecap="round"/></svg>
          </button>
        </div>
      </footer>
    </main>
  </div>

  <UserProfile v-model="userProfileVisible" @profile-updated="handleProfileUpdated" />
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sendMessage, createSession, getSessionList, deleteSession, cleanSessions,
  createStreamConnection, exportSession, getSessionHistory
} from '../api/index.js'
import request from '../api/index.js'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import UserProfile from '../components/UserProfile.vue'

const router = useRouter()
const route = useRoute()
const messageAreaRef = ref(null)
const inputEl = ref(null)
const inputText = ref('')
const loading = ref(false)
const messages = reactive([])
const sessions = reactive([])
const currentSessionId = ref(null)
const currentSession = ref(null)
const userProfileVisible = ref(false)
let abortController = null

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
    const res = await request({ url: '/knowledge/upload', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
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
loadKbFiles()

const user = computed(() => {
  try { const s = localStorage.getItem('user'); return s ? JSON.parse(s) : null }
  catch { return null }
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
  if (!token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  await loadSessions()
})

// 从历史页跳转：加载指定会话（即使不在侧栏列表中也能加载）
const resumeChatSession = async (sessionId) => {
  try {
    const res = await getSessionHistory(sessionId)
    if (res.data?.messages?.length > 0) {
      // 会话有消息，创建虚拟会话对象并切换
      const virtualSession = {
        id: Number(sessionId),
        title: res.data.title || '对话记录'
      }
      currentSessionId.value = virtualSession.id
      currentSession.value = virtualSession
      messages.length = 0
      messages.push(...res.data.messages)
      await nextTick()
      // 加入侧栏列表（如果不存在）
      if (!sessions.find(s => s.id == sessionId)) {
        sessions.unshift(virtualSession)
      }
      router.replace({ path: '/chat', query: {} })
    }
  } catch (e) { console.error('恢复会话失败:', e) }
}

// 监听历史页跳转参数
watch(() => route.query.resumeId, (newId) => {
  if (newId) resumeChatSession(newId)
}, { immediate: true })

const loadSessions = async () => {
  try {
    const res = await getSessionList()
    sessions.length = 0
    sessions.push(...res.data)
    if (sessions.length > 0 && !currentSessionId.value && !route.query.resumeId) {
      switchSession(sessions[0])
    }
  } catch (e) { console.error('加载会话列表失败:', e) }
}
const handleNewSession = async () => {
  try {
    const res = await createSession()
    sessions.unshift(res.data)
    switchSession(res.data)
    ElMessage.success('已创建新会话')
  } catch (e) { ElMessage.error('创建会话失败') }
}
const switchSession = async (session) => {
  currentSessionId.value = session.id
  currentSession.value = session
  inputText.value = ''
  messages.length = 0
  try {
    const res = await getSessionHistory(session.id)
    messages.push(...res.data.messages)
    await scrollToBottom()
  } catch (e) { console.error('加载会话历史失败:', e) }
}
const handleDeleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${session.title}"吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteSession(session.id)
    const idx = sessions.findIndex(s => s.id === session.id)
    if (idx !== -1) sessions.splice(idx, 1)
    if (currentSessionId.value === session.id) {
      if (sessions.length > 0) switchSession(sessions[0])
      else { currentSessionId.value = null; currentSession.value = null; messages.length = 0 }
    }
    ElMessage.success('已删除')
  } catch { /* cancel */ }
}
const scrollToBottom = async () => {
  await nextTick()
  if (messageAreaRef.value) messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
}
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  if (isToday) return `${h}:${m}`
  return `${date.getMonth() + 1}/${date.getDate()} ${h}:${m}`
}
const autoResize = () => {
  const el = inputEl.value
  if (el) { el.style.height = 'auto'; el.style.height = Math.min(el.scrollHeight, 120) + 'px' }
}
const handleSend = async () => {
  const content = inputText.value.trim()
  if (!content || loading.value) return
  let isNewlyCreated = false
  if (!currentSessionId.value) {
    try {
      const res = await createSession()
      sessions.unshift(res.data)
      switchSession(res.data)
      isNewlyCreated = true
    } catch { ElMessage.error('创建会话失败，请重试'); return }
  }
  messages.push({ role: 'user', content, createdAt: new Date().toISOString() })
  inputText.value = ''
  if (inputEl.value) { inputEl.value.style.height = 'auto' }
  await scrollToBottom()
  const assistantMsg = { role: 'assistant', content: '', createdAt: new Date().toISOString() }
  messages.push(assistantMsg)
  await scrollToBottom()

  if (ragMode.value) {
    loading.value = true
    try {
      const res = await request({ url: '/chat/rag', method: 'post', data: { question: content } })
      assistantMsg.content = res.data.reply
      loading.value = false
      if (!assistantMsg.content.trim()) { const idx = messages.indexOf(assistantMsg); if (idx !== -1) messages.splice(idx, 1) }
    } catch { loading.value = false; ElMessage.error('RAG 问答失败'); const idx = messages.indexOf(assistantMsg); if (idx !== -1) messages.splice(idx, 1) }
    return
  }

  loading.value = true
  abortController = createStreamConnection(
    { content, sessionId: currentSessionId.value },
    {
      onMessage: (text) => { assistantMsg.content += text; scrollToBottom() },
      onDone: () => {
        loading.value = false; abortController = null
        if (!assistantMsg.content.trim()) { const idx = messages.indexOf(assistantMsg); if (idx !== -1) messages.splice(idx, 1) }
        if (currentSession.value && currentSession.value.title === '新对话') loadSessions()
        scrollToBottom()
      },
      onError: (error) => {
        loading.value = false; abortController = null
        if (!assistantMsg.content.trim()) { const idx = messages.indexOf(assistantMsg); if (idx !== -1) messages.splice(idx, 1) }
        ElMessage.error('发送失败：' + (error.message || '请检查网络连接'))
        if (isNewlyCreated && currentSessionId.value) {
          deleteSession(currentSessionId.value).then(() => { const idx = sessions.findIndex(s => s.id === currentSessionId.value); if (idx !== -1) sessions.splice(idx, 1) }).catch(() => {})
          currentSessionId.value = null; currentSession.value = null; messages.length = 0
        }
      }
    }
  )
}
const handleCleanSessions = async () => {
  try {
    await ElMessageBox.confirm('将删除所有对话记录，此操作不可恢复，确定继续吗？', '清理全部会话', { confirmButtonText: '确定清理', cancelButtonText: '取消', type: 'warning' })
    const res = await cleanSessions()
    const count = res.data ?? 0
    if (count > 0) {
      ElMessage.success(`已清理 ${count} 个会话`)
    } else {
      ElMessage.info('没有会话需要清理')
    }
    await loadSessions()
  } catch { /* cancel or error */ }
}
const goToHistory = () => router.push('/history')
const handleExportChat = async () => {
  if (!currentSessionId.value) return
  try { await exportSession(currentSessionId.value, 'md'); ElMessage.success('导出成功') }
  catch { ElMessage.error('导出失败') }
}
const handleLogout = () => {
  localStorage.removeItem('token'); localStorage.removeItem('user')
  ElMessage.success('已退出登录'); router.push('/login')
}
const handleProfileUpdated = (updatedUser) => {
  if (updatedUser.nickname) {
    try {
      const userStr = localStorage.getItem('user')
      if (userStr) { const u = JSON.parse(userStr); u.nickname = updatedUser.nickname; localStorage.setItem('user', JSON.stringify(u)) }
    } catch { /* ignore */ }
  }
}
</script>

<style scoped>
/* ==========================================
   Layout
   ========================================== */
.chat-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(165deg, #E4F0FD 0%, #EEF4FC 25%, #F3F7FC 55%, #ECF3FB 85%, #E6F0FA 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==========================================
   Sidebar
   ========================================== */
.chat-sidebar {
  width: 250px;
  min-width: 250px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(175deg, #F6F9FE 0%, #F0F5FF 50%, #F4F8FD 100%);
  border-right: 1px solid #E4EAF4;
  z-index: 10;
}

.sb-brand {
  display: flex; align-items: center; gap: 10px;
  padding: 18px 16px 12px;
}
.sb-logo {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700;
  box-shadow: 0 4px 12px rgba(79,140,255,0.25);
  flex-shrink: 0;
}
.sb-name { font-size: 14px; font-weight: 650; color: #1C2640; line-height: 1.2; }
.sb-sub { font-size: 10.5px; color: #909BB5; font-weight: 500; }

.sb-actions {
  display: flex; gap: 6px; padding: 0 12px 10px;
}
.sb-btn {
  flex: 1;
  display: flex; align-items: center; justify-content: center; gap: 5px;
  padding: 7px 10px; border-radius: 10px; border: none;
  font-size: 12px; font-weight: 550; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
}
.sb-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sb-btn-primary {
  background: #4F8CFF; color: #fff;
  box-shadow: 0 2px 8px rgba(79,140,255,0.2);
}
.sb-btn-primary:hover:not(:disabled) { background: #3B6FDF; }
.sb-btn-ghost {
  background: rgba(79,140,255,0.06); color: #5B6780;
}
.sb-btn-ghost:hover:not(:disabled) { background: rgba(79,140,255,0.10); color: #3B6FDF; }
.sb-btn-sm { flex: none; padding: 5px 10px; font-size: 11px; }

.sb-sessions {
  flex: 1; overflow-y: auto; padding: 4px 10px;
}
.sb-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 40px 10px; color: #909BB5; font-size: 12px; gap: 8px;
}

.sb-session {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-radius: 10px;
  cursor: pointer; transition: all 0.18s;
  margin-bottom: 2px;
}
.sb-session:hover { background: rgba(79,140,255,0.05); }
.sb-session.active {
  background: linear-gradient(135deg, rgba(79,140,255,0.10), rgba(124,203,255,0.06));
  box-shadow: 0 0 0 1px rgba(79,140,255,0.14);
}
.sbs-left { display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1; }
.sbs-dot {
  width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
  background: #BDD3FF;
}
.sb-session.active .sbs-dot { background: #4F8CFF; box-shadow: 0 0 6px rgba(79,140,255,0.3); }
.sbs-title {
  font-size: 13px; color: #5B6780; font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sb-session.active .sbs-title { color: #2D52B5; font-weight: 600; }
.sbs-del {
  opacity: 0; background: none; border: none; cursor: pointer;
  color: #909BB5; padding: 2px; border-radius: 4px; transition: all 0.15s;
}
.sb-session:hover .sbs-del { opacity: 1; }
.sbs-del:hover { color: #FF6B5B; background: rgba(255,107,91,0.08); }

/* KB */
.sb-kb { border-top: 1px solid #E4EAF4; }
.sb-kb-toggle {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 16px; cursor: pointer;
  font-size: 12px; color: #5B6780; font-weight: 520;
  transition: color 0.15s;
}
.sb-kb-toggle:hover { color: #3B6FDF; }
.kb-arrow { margin-left: auto; transition: transform 0.2s; }
.kb-arrow.open { transform: rotate(180deg); }
.kb-count { font-size: 10px; opacity: 0.6; }
.sb-kb-body { padding: 0 12px 10px; }
.kb-files { max-height: 100px; overflow-y: auto; margin-bottom: 6px; }
.kb-file {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 8px; border-radius: 6px; font-size: 11px; color: #5B6780;
}
.kb-file:hover { background: rgba(79,140,255,0.04); }
.kb-fname { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.kb-fdel { background: none; border: none; cursor: pointer; color: #909BB5; opacity: 0; padding: 2px; }
.kb-file:hover .kb-fdel { opacity: 1; }
.kb-fdel:hover { color: #FF6B5B; }
.kb-empty { font-size: 11px; color: #A0ACC5; padding: 4px 8px; margin: 0; }
.hidden { display: none; }

/* User */
.sb-user {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 14px; border-top: 1px solid #E4EAF4;
}
.sbu-avatar {
  width: 28px; height: 28px; border-radius: 50%; object-fit: cover;
  flex-shrink: 0;
}
.sbu-avatar-fallback {
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 600;
}
.sbu-name {
  flex: 1; font-size: 12px; color: #5B6780; font-weight: 550;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sbu-btn {
  background: none; border: none; cursor: pointer;
  color: #909BB5; padding: 4px; border-radius: 6px; transition: all 0.15s;
  display: flex;
}
.sbu-btn:hover { color: #3B6FDF; background: rgba(79,140,255,0.06); }
.sbu-btn-logout:hover { color: #FF6B5B; background: rgba(255,107,91,0.06); }

/* ==========================================
   Main Chat Area
   ========================================== */
.chat-main {
  flex: 1; display: flex; flex-direction: column;
  min-width: 0; position: relative;
}

/* Topbar */
.chat-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; z-index: 10;
  border-bottom: 1px solid rgba(79,140,255,0.06);
}
.ctb-left { display: flex; align-items: center; gap: 10px; }
.ctb-icon {
  width: 32px; height: 32px; border-radius: 9px;
  background: rgba(79,140,255,0.08); color: #4F8CFF;
  display: flex; align-items: center; justify-content: center;
}
.ctb-title {
  font-size: 14px; font-weight: 600; color: #1C2640;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 300px;
}
.ctb-right { display: flex; gap: 4px; }
.ctb-btn {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 12px; border-radius: 9px; border: none;
  font-size: 12px; color: #5B6780; cursor: pointer;
  background: rgba(79,140,255,0.04); font-family: inherit;
  transition: all 0.18s;
}
.ctb-btn:hover { background: rgba(79,140,255,0.08); color: #3B6FDF; }
.ctb-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Messages */
.chat-messages {
  flex: 1; overflow-y: auto;
  padding: 20px 0;
}

/* Empty state */
.chat-empty {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; height: 100%; gap: 12px;
  padding: 40px 20px; text-align: center;
}
.ce-icon {
  width: 72px; height: 72px; border-radius: 20px;
  background: rgba(79,140,255,0.06); color: #4F8CFF;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 4px;
}
.ce-title { font-size: 18px; font-weight: 650; color: #1C2640; margin: 0; }
.ce-sub { font-size: 13.5px; color: #909BB5; margin: 0 0 8px; }
.ce-hints { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.ce-hint {
  padding: 10px 18px; border-radius: 12px; border: 1px solid #E4EAF4;
  background: rgba(255,255,255,0.6); cursor: pointer;
  font-size: 13px; color: #5B6780; font-family: inherit;
  transition: all 0.2s; text-align: left;
}
.ce-hint:hover { border-color: #BDD3FF; color: #3B6FDF; background: rgba(79,140,255,0.04); }

/* Message list */
.msg-list { max-width: 780px; margin: 0 auto; padding: 0 24px; }

.msg-row {
  display: flex; gap: 10px; margin-bottom: 20px;
  animation: msgIn 0.3s ease-out both;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-you { flex-direction: row-reverse; }

.msg-avatar { flex-shrink: 0; margin-top: 2px; }
.msg-av {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 650;
}
.msg-av-you {
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; box-shadow: 0 3px 10px rgba(79,140,255,0.2);
}
.msg-av-bot {
  background: rgba(255,255,255,0.7);
  border: 1px solid #E4EAF4; color: #4F8CFF;
  box-shadow: 0 1px 3px rgba(79,140,255,0.05);
}

.msg-body { max-width: 72%; min-width: 0; }
.msg-you .msg-body { display: flex; flex-direction: column; align-items: flex-end; }

.msg-bubble {
  padding: 10px 16px; border-radius: 18px;
  font-size: 14px; line-height: 1.6; word-break: break-word;
}
.bubble-you {
  background: linear-gradient(135deg, #4F8CFF 0%, #3B6FDF 100%);
  color: #fff; border-bottom-right-radius: 6px;
  box-shadow: 0 3px 12px rgba(79,140,255,0.18);
}
.bubble-ai {
  background: rgba(255,255,255,0.70);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(79,140,255,0.08);
  color: #1C2640; border-bottom-left-radius: 6px;
  box-shadow: 0 1px 6px rgba(79,140,255,0.04);
}

.typing {
  display: flex; align-items: center; gap: 4px;
  padding: 14px 20px;
}
.typing-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #BDD3FF;
  animation: bounce 1.2s infinite ease-in-out;
}
.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.15s; }
.typing-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.msg-time {
  font-size: 10.5px; color: #A0ACC5; margin-top: 3px; display: block;
}
.msg-you .msg-time { text-align: right; }

/* ==========================================
   Input Area
   ========================================== */
.chat-input-area {
  padding: 0 20px 16px;
  max-width: 820px; margin: 0 auto; width: 100%;
}
.cia-row {
  display: flex; align-items: center; padding: 0 4px 8px;
}
.rag-toggle {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; font-size: 12px; color: #909BB5; user-select: none;
}
.rag-toggle input { display: none; }
.rag-slider {
  width: 36px; height: 20px; border-radius: 10px;
  background: #E4EAF4; position: relative; transition: background 0.2s;
}
.rag-slider::after {
  content: ''; position: absolute;
  width: 16px; height: 16px; border-radius: 50%;
  background: #fff; top: 2px; left: 2px;
  transition: transform 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.rag-toggle input:checked + .rag-slider { background: #4F8CFF; }
.rag-toggle input:checked + .rag-slider::after { transform: translateX(16px); }
.rag-label { font-weight: 500; }

.cia-box {
  display: flex; align-items: flex-end; gap: 8px;
  padding: 6px 8px 6px 16px;
  border-radius: 20px;
  transition: box-shadow 0.25s;
}
.cia-box:focus-within {
  box-shadow: 0 0 0 3px rgba(79,140,255,0.12), 0 4px 20px rgba(79,140,255,0.08);
}
.cia-textarea {
  flex: 1; border: none; outline: none; resize: none;
  background: transparent; font-size: 14px; line-height: 1.5;
  font-family: inherit; color: #1C2640; padding: 6px 0;
  min-height: 24px; max-height: 120px;
}
.cia-textarea::placeholder { color: #B5C0D8; }
.cia-send {
  width: 36px; height: 36px; min-width: 36px; border-radius: 12px;
  border: none; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
  background: #E4EAF4; color: #B5C0D8;
}
.cia-send.active {
  background: linear-gradient(135deg, #4F8CFF, #3B6FDF);
  color: #fff; box-shadow: 0 3px 12px rgba(79,140,255,0.25);
}
.cia-send.active:hover { transform: scale(1.04); }
.cia-send:disabled { cursor: not-allowed; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .chat-sidebar { display: none; }
  .ctb-title { max-width: 160px; }
}
</style>
