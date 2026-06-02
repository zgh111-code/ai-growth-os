import axios from 'axios'
import { ElMessage } from 'element-plus'

// ============================================
// API 基础地址配置
// ============================================
// 从环境变量读取 API 地址。
// Vite 在构建时会自动替换 import.meta.env.VITE_xxx 为实际值。
//
// 开发环境（npm run dev）：
//   读取 .env.development 中的 VITE_API_BASE_URL
//   → http://localhost:8080/api
//
// 生产环境（npm run build）：
//   读取 .env.production 中的 VITE_API_BASE_URL
//   → /api（通过 Nginx 反向代理）
//
// 兜底值：如果环境变量没设置，默认使用 http://localhost:8080/api
// ============================================
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

console.log('[API] 当前 API 地址:', API_BASE_URL)

// 创建 axios 实例
const request = axios.create({
  // 后端 API 基础地址（从环境变量读取，不再硬编码）
  baseURL: API_BASE_URL,
  // 请求超时时间：30秒
  timeout: 30000,
  // 请求头
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动携带 JWT token
request.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      // 在请求头中添加 Authorization
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    // 如果后端返回了错误码
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      // 如果是 401 未登录，清除 token 并跳转到登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/#/login'
      }
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    // 网络错误处理
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/#/login'
      } else if (status === 500) {
        ElMessage.error('服务器错误，请稍后重试')
      } else {
        ElMessage.error(`请求失败 (${status})`)
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

// ========== API 接口方法 ==========

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

/**
 * 发送聊天消息（非流式，等待完整回复）
 */
export function sendMessage(data) {
  return request({
    url: '/chat/send',
    method: 'post',
    data
  })
}

/**
 * 流式发送消息（SSE 逐字输出）
 * 使用 fetch API 实现，支持 Authorization 请求头
 *
 * @param {object} params - { content, sessionId }
 * @param {object} handlers - 事件处理器
 * @param {function} handlers.onMessage - 收到逐字片段时回调 (text: string) => void
 * @param {function} handlers.onDone - 流结束时回调 () => void
 * @param {function} handlers.onError - 出错时回调 (error: Error) => void
 * @returns {AbortController} 用于取消请求的控制器
 */
export function createStreamConnection(params, handlers) {
  const { onMessage, onDone, onError } = handlers
  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const url = `${baseUrl}/chat/send/stream?content=${encodeURIComponent(params.content)}&sessionId=${params.sessionId}`

  const controller = new AbortController()

  const timeoutId = setTimeout(() => {
    controller.abort()
    onError(new Error('请求超时，请稍后重试'))
  }, 60000)

  fetch(url, {
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` },
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      // 逐行解析 SSE 事件流
      while (true) {
        const { done, value } = await reader.read()

        if (value) {
          buffer += decoder.decode(value, { stream: true })
        }

        // 从 buffer 中提取完整行（以 \n 结尾）
        let newlineIdx
        while ((newlineIdx = buffer.indexOf('\n')) !== -1) {
          const line = buffer.slice(0, newlineIdx)
          buffer = buffer.slice(newlineIdx + 1)

          // 空行 = 事件边界，触发上一个收到的事件
          if (line === '' || line === '\r') {
            flushEvent()
          } else if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            // data: 后面可能有一个空格
            const d = line.slice(5).replace(/^ /, '')
            currentData = currentData ? currentData + '\n' + d : d
          }
        }

        if (done) {
          // 处理最后可能残留的事件
          if (currentEvent || currentData) {
            flushEvent()
          }
          break
        }
      }
    })
    .catch((error) => {
      if (error.name === 'AbortError') return
      onError(error)
    })
    .finally(() => {
      clearTimeout(timeoutId)
    })

  // SSE 事件状态
  let currentEvent = null
  let currentData = null

  function flushEvent() {
    const eventType = currentEvent || 'message'
    if (eventType === 'message' && currentData != null) {
      onMessage(currentData)
    } else if (eventType === 'done') {
      onDone()
    } else if (eventType === 'error') {
      onError(new Error(currentData || 'AI 回复异常'))
    }
    currentEvent = null
    currentData = null
  }

  return controller
}

/**
 * 获取聊天历史记录列表（按天分组）
 */
export function getHistory() {
  return request({
    url: '/chat/history',
    method: 'get'
  })
}

/**
 * 获取某天的详细聊天记录
 * @param {string} date 日期，格式：yyyy-MM-dd
 */
export function getHistoryByDate(date) {
  return request({
    url: `/chat/history/${date}`,
    method: 'get'
  })
}

/**
 * 创建新会话
 */
export function createSession() {
  return request({
    url: '/session/create',
    method: 'post'
  })
}

/**
 * 获取会话列表
 */
export function getSessionList() {
  return request({
    url: '/session/list',
    method: 'get'
  })
}

/**
 * 删除会话
 * @param {number} sessionId 会话ID
 */
export function deleteSession(sessionId) {
  return request({
    url: `/session/delete/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 清理空会话（标题为"新对话"且没有聊天记录的会话）
 */
export function cleanSessions() {
  return request({
    url: '/session/clean',
    method: 'post'
  })
}

/**
 * 获取某个会话的详细聊天记录
 * @param {number} sessionId 会话ID
 */
export function getSessionHistory(sessionId) {
  return request({
    url: `/chat/history/session/${sessionId}`,
    method: 'get'
  })
}

/**
 * 上传头像（multipart/form-data）
 * @param {File} file
 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 修改用户基本信息（昵称、头像）
 * @param {object} data { nickname, avatar } 两个字段都可选
 */
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

/**
 * 导出会话聊天记录（触发浏览器下载）
 * @param {number} sessionId
 * @param {string} format - 'md' 或 'txt'
 */
export function exportSession(sessionId, format = 'md') {
  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const url = `${baseUrl}/chat/export/${sessionId}?format=${format}`

  // 用 fetch + blob 触发下载
  return fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
    .then(res => {
      if (!res.ok) throw new Error('导出失败')
      return res.blob()
    })
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = `chat_${sessionId}.${format}`
      a.click()
      URL.revokeObjectURL(a.href)
    })
}

/**
 * 搜索聊天记录
 * @param {object} params { keyword, sessionId?, page?, size? }
 */
export function searchMessages(params) {
  return request({
    url: '/chat/search',
    method: 'get',
    params
  })
}

/**
 * 修改密码
 * @param {object} data { oldPassword, newPassword }
 */
export function updatePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

// ============================================
// AI Growth OS — 4 个成长模块 API
// ============================================

/**
 * 每日复盘 — 提交复盘
 * @param {object} data { content, mood }
 */
export function submitReview(data) {
  return request({ url: '/daily-review/submit', method: 'post', data })
}

/**
 * 每日复盘 — 获取历史列表
 */
export function getReviewList() {
  return request({ url: '/daily-review/list', method: 'get' })
}

/**
 * 每日复盘 — 获取单条详情
 * @param {number} id
 */
export function getReviewDetail(id) {
  return request({ url: `/daily-review/${id}`, method: 'get' })
}

/**
 * 学习追踪 — 打卡
 * @param {object} data { category, durationMin, note }
 */
export function learningCheckin(data) {
  return request({ url: '/learning/checkin', method: 'post', data })
}

/**
 * 学习追踪 — 统计（连续天数、本周统计等）
 */
export function getLearningStats() {
  return request({ url: '/learning/stats', method: 'get' })
}

/**
 * 学习追踪 — 月度日历
 * @param {number} year
 * @param {number} month
 */
export function getLearningCalendar(year, month) {
  return request({ url: '/learning/calendar', method: 'get', params: { year, month } })
}

/**
 * 表达训练 — 获取今日话题
 */
export function getTodayTopic() {
  return request({ url: '/expression/today', method: 'get' })
}

/**
 * 表达训练 — 提交表达内容
 * @param {object} data { content }
 */
export function submitExpression(data) {
  return request({ url: '/expression/submit', method: 'post', data })
}

/**
 * 表达训练 — 历史记录
 */
export function getExpressionHistory() {
  return request({ url: '/expression/history', method: 'get' })
}

/**
 * 表达训练 — 查看单条详情
 * @param {number} id 记录ID
 */
export function getExpressionDetail(id) {
  return request({ url: `/expression/${id}`, method: 'get' })
}

/**
 * 表达训练 — 删除单条记录
 * @param {number} id 记录ID
 */
export function deleteExpression(id) {
  return request({ url: `/expression/${id}`, method: 'delete' })
}

/**
 * 项目推进 — 提交项目状态
 * @param {object} data { projectName, status, blocker, progress }
 */
export function submitProject(data) {
  return request({ url: '/project/submit', method: 'post', data })
}

/**
 * 项目推进 — 项目记录列表
 */
export function getProjectList() {
  return request({ url: '/project/list', method: 'get' })
}

/**
 * 项目推进 — 单条详情
 * @param {number} id
 */
export function getProjectDetail(id) {
  return request({ url: `/project/${id}`, method: 'get' })
}

export default request
