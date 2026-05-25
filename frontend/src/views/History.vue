<template>
  <div class="hist-container">
    <!-- 顶部 -->
    <header class="hist-top glass-card-solid">
      <button class="hist-back" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
        返回聊天
      </button>
      <h1 class="hist-title">聊天历史</h1>
      <div class="hist-actions">
        <span class="hist-user">{{ user?.nickname || user?.username }}</span>
        <button class="hist-logout" @click="handleLogout">退出</button>
      </div>
    </header>

    <!-- 搜索 -->
    <div class="hist-search">
      <input v-model="searchKeyword" class="glass-input" placeholder="搜索聊天记录..." @keyup.enter="handleSearch" />
      <button v-if="!searchMode" class="submit-btn" @click="handleSearch">搜索</button>
      <button v-else class="btn-ghost" @click="clearSearch">清除</button>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchMode" class="hist-body">
      <div class="search-info">搜索「{{ searchKeyword }}」找到 {{ searchTotal }} 条结果</div>
      <div v-if="searchLoading" class="empty-state"><p>搜索中...</p></div>
      <div v-else-if="searchRecords.length === 0" class="empty-state">
        <div class="empty-dot"></div><p>未找到匹配的消息</p>
      </div>
      <div v-else class="search-list">
        <div v-for="msg in searchRecords" :key="msg.id" class="search-item">
          <div class="si-head">
            <span class="si-role" :class="msg.role">{{ msg.role === 'user' ? '你' : 'AI' }}</span>
            <span class="si-time">{{ formatTime(msg.createdAt) }}</span>
          </div>
          <div class="si-content">{{ msg.content }}</div>
        </div>
        <div v-if="searchPages > 1" class="pager">
          <button v-for="p in searchPages" :key="p" :class="['page-btn', { active: p === searchPage }]" @click="searchPage = p; handleSearch()">{{ p }}</button>
        </div>
      </div>
    </div>

    <!-- 加载 -->
    <div v-else-if="loading" class="hist-body empty-state"><p>加载中...</p></div>

    <!-- 空 -->
    <div v-else-if="sessions.length === 0" class="hist-body empty-state">
      <div class="empty-dot"></div>
      <p>暂无聊天记录</p>
      <button class="submit-btn" @click="goToChat" style="margin-top:12px">开始聊天</button>
    </div>

    <!-- 历史列表 -->
    <div v-else class="hist-body">
      <div class="total-info">共 {{ total }} 条消息，{{ sessions.length }} 个对话</div>
      <div v-for="session in sessions" :key="session.sessionId" class="sess-card" @click="viewSession(session)">
        <div class="sess-head">
          <span class="sess-title">{{ session.title }}</span>
          <span class="sess-count">{{ session.count }} 条</span>
        </div>
        <div class="sess-foot">
          <span class="sess-time">{{ formatTime(session.lastTime) }}</span>
          <button class="sess-export" @click.stop="handleExport(session.sessionId)" title="导出">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
        <div class="modal-card modal-wide anim-lift">
          <div class="modal-top">
            <h3>对话记录 — {{ currentSessionTitle }}</h3>
            <button class="modal-close" @click="detailVisible = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div v-if="detailLoading" class="empty-state"><p>加载中...</p></div>
          <div v-else class="detail-msgs">
            <div v-for="(msg, i) in detailMessages" :key="i" class="dmsg" :class="msg.role">
              <div class="dmsg-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
              <div class="dmsg-bubble" :class="msg.role">
                <MarkdownRenderer :content="msg.content" />
              </div>
              <div class="dmsg-time">{{ formatDetailTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHistory, getSessionHistory, searchMessages, exportSession } from '../api/index.js'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'

const router = useRouter()
const loading = ref(true)
const sessions = ref([])
const total = ref(0)
const searchKeyword = ref('')
const searchMode = ref(false)
const searchLoading = ref(false)
const searchRecords = ref([])
const searchTotal = ref(0)
const searchPage = ref(1)
const searchSize = ref(10)
const searchPages = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentSessionTitle = ref('')
const detailMessages = ref([])

const user = computed(() => {
  try { const s = localStorage.getItem('user'); return s ? JSON.parse(s) : null } catch { return null }
})

onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try { const res = await getHistory(); sessions.value = res.data.sessions; total.value = res.data.total } catch (e) { console.error('获取历史记录失败:', e) }
  finally { loading.value = false }
})

const handleSearch = async () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchMode.value = true; searchLoading.value = true
  try {
    const res = await searchMessages({ keyword: kw, page: searchPage.value, size: searchSize.value })
    searchRecords.value = res.data.records; searchTotal.value = res.data.total; searchPages.value = res.data.pages
  } catch (e) { console.error('搜索失败:', e) }
  finally { searchLoading.value = false }
}
const clearSearch = () => { searchMode.value = false; searchKeyword.value = ''; searchRecords.value = []; searchTotal.value = 0; searchPage.value = 1 }
const handleExport = async (sessionId, format = 'md') => { try { await exportSession(sessionId, format); ElMessage.success('导出成功') } catch { ElMessage.error('导出失败') } }
const goBack = () => router.push('/chat')
const goToChat = () => router.push('/chat')
const viewSession = async (session) => {
  currentSessionTitle.value = session.title; detailVisible.value = true; detailLoading.value = true
  try { const res = await getSessionHistory(session.sessionId); detailMessages.value = res.data.messages } catch (e) { ElMessage.error('获取会话详情失败') }
  finally { detailLoading.value = false }
}
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' })
    .then(() => { localStorage.removeItem('token'); localStorage.removeItem('user'); ElMessage.success('已退出登录'); router.push('/login') })
    .catch(() => {})
}
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr); const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = date.toDateString() === yesterday.toDateString()
  const h = date.getHours().toString().padStart(2, '0'); const m = date.getMinutes().toString().padStart(2, '0')
  if (isToday) return `今天 ${h}:${m}`
  if (isYesterday) return `昨天 ${h}:${m}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${h}:${m}`
}
const formatDetailTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.hist-container { height: 100vh; display: flex; flex-direction: column; background: linear-gradient(170deg, #F8FAFD, #F0F5FF); }
.hist-top { display: flex; align-items: center; padding: 14px 24px; border-bottom: 1px solid rgba(79,140,255,0.06); }
.hist-back { display: flex; align-items: center; gap: 5px; background: none; border: none; cursor: pointer; font-size: 13px; color: #4F8CFF; font-family: inherit; }
.hist-title { flex: 1; margin: 0; text-align: center; font-size: 16px; font-weight: 650; color: #1C2640; }
.hist-actions { display: flex; align-items: center; gap: 12px; }
.hist-user { font-size: 13px; color: #5B6780; font-weight: 500; }
.hist-logout { background: none; border: none; cursor: pointer; font-size: 12px; color: #A0ACC5; font-family: inherit; }
.hist-logout:hover { color: #FF6B5B; }

.hist-search { display: flex; gap: 10px; padding: 16px 24px; max-width: 700px; margin: 0 auto; width: 100%; box-sizing: border-box; align-items: center; }
.glass-input {
  flex: 1; border: 1px solid #E4EAF4; border-radius: 14px;
  padding: 10px 16px; font-size: 14px; font-family: inherit;
  color: #1C2640; background: #F8FAFD; outline: none;
  transition: all 0.2s; box-sizing: border-box;
}
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.08); }
.submit-btn {
  padding: 10px 18px; border-radius: 12px; border: none;
  background: #4F8CFF; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  box-shadow: 0 2px 8px rgba(79,140,255,0.15); font-family: inherit;
  transition: all 0.15s; white-space: nowrap;
}
.submit-btn:hover { background: #3B6FDF; }
.btn-ghost {
  padding: 10px 18px; border-radius: 12px; border: 1px solid #E4EAF4;
  background: #fff; color: #5B6780; font-size: 13px; cursor: pointer; font-family: inherit;
}

.hist-body { flex: 1; overflow-y: auto; padding: 0 24px 40px; max-width: 700px; width: 100%; margin: 0 auto; box-sizing: border-box; }

/* Empty */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #909BB5; text-align: center; }
.empty-dot { width: 56px; height: 56px; border-radius: 50%; background: linear-gradient(135deg, #BDD3FF, #E0ECFF); opacity: 0.4; margin-bottom: 14px; }
.empty-state p { font-size: 14px; margin: 0; }

/* Search */
.search-info { font-size: 13px; color: #909BB5; margin-bottom: 16px; padding-top: 4px; }
.search-list { display: flex; flex-direction: column; gap: 8px; }
.search-item { background: #fff; border: 1px solid #E4EAF4; border-radius: 14px; padding: 14px 18px; }
.si-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.si-role { font-size: 11px; font-weight: 650; padding: 2px 8px; border-radius: 6px; }
.si-role.user { background: rgba(79,140,255,0.08); color: #4F8CFF; }
.si-role.assistant { background: #F0F4FA; color: #5B6780; }
.si-time { font-size: 11px; color: #A0ACC5; }
.si-content { font-size: 13.5px; line-height: 1.6; color: #3C4A6E; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pager { display: flex; gap: 6px; justify-content: center; margin-top: 16px; }
.page-btn { width: 32px; height: 32px; border-radius: 8px; border: 1px solid #E4EAF4; background: #fff; cursor: pointer; font-size: 13px; color: #5B6780; font-family: inherit; }
.page-btn.active { background: #4F8CFF; color: #fff; border-color: #4F8CFF; }

/* Sessions */
.total-info { font-size: 13px; color: #909BB5; margin-bottom: 14px; padding-top: 4px; }
.sess-card {
  background: #fff; border: 1px solid #E4EAF4; border-radius: 16px;
  padding: 16px 20px; margin-bottom: 10px; cursor: pointer;
  transition: all 0.18s;
}
.sess-card:hover { border-color: #BDD3FF; box-shadow: 0 4px 16px rgba(79,140,255,0.06); transform: translateY(-1px); }
.sess-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.sess-title { font-size: 14.5px; font-weight: 650; color: #1C2640; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; margin-right: 12px; }
.sess-count { font-size: 11.5px; color: #4F8CFF; font-weight: 550; background: rgba(79,140,255,0.06); padding: 3px 10px; border-radius: 12px; }
.sess-foot { display: flex; justify-content: space-between; align-items: center; }
.sess-time { font-size: 12px; color: #A0ACC5; }
.sess-export { background: none; border: none; cursor: pointer; color: #BDD3FF; padding: 4px; border-radius: 6px; }
.sess-export:hover { color: #4F8CFF; background: rgba(79,140,255,0.06); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.2); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 20px; padding: 28px 24px; width: 380px; max-width: 90vw; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-wide { width: 720px; max-height: 80vh; display: flex; flex-direction: column; }
.modal-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-top h3 { font-size: 16px; font-weight: 650; margin: 0; color: #1C2640; }
.modal-close { background: none; border: none; cursor: pointer; color: #909BB5; }
.modal-close:hover { color: #5B6780; }
.detail-msgs { overflow-y: auto; flex: 1; }
.dmsg { margin-bottom: 20px; }
.dmsg.user { text-align: right; }
.dmsg-role { font-size: 11px; color: #A0ACC5; margin-bottom: 4px; font-weight: 550; }
.dmsg-bubble { display: inline-block; padding: 10px 16px; border-radius: 14px; font-size: 14px; line-height: 1.6; max-width: 80%; text-align: left; word-break: break-word; }
.dmsg-bubble.user { background: linear-gradient(135deg, #4F8CFF, #3B6FDF); color: #fff; border-bottom-right-radius: 4px; }
.dmsg-bubble.assistant { background: #F8FAFD; color: #3C4A6E; border: 1px solid #E4EAF4; border-bottom-left-radius: 4px; }
.dmsg-time { font-size: 10.5px; color: #A0ACC5; margin-top: 3px; }
</style>
