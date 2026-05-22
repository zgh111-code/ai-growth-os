<template>
  <div class="kb-container">
    <header class="kb-header">
      <el-button text @click="$router.push('/chat')" class="back-btn">
        <el-icon :size="16"><ArrowLeft /></el-icon>
        返回聊天
      </el-button>
      <h1 class="kb-title">知识库</h1>
      <span></span>
    </header>

    <div class="kb-body">
      <!-- 上传区 -->
      <div class="upload-area">
        <input type="file" ref="fileInput" class="file-hidden" @change="handleUpload" accept=".txt,.md,.java,.py,.js,.html,.css,.json,.xml,.yml,.sql" />
        <el-button type="primary" @click="$refs.fileInput.click()" :loading="uploading">
          <el-icon :size="14"><Upload /></el-icon>
          上传文档
        </el-button>
        <span class="upload-hint">支持 TXT、MD、代码文件（最大 5MB）</span>
      </div>

      <!-- 快速问答 -->
      <div class="rag-ask">
        <el-input v-model="ragQuestion" placeholder="基于知识库提问..." @keyup.enter="handleRagAsk" :disabled="ragLoading">
          <template #append>
            <el-button @click="handleRagAsk" :loading="ragLoading">提问</el-button>
          </template>
        </el-input>
        <div v-if="ragAnswer" class="rag-answer">
          <div class="rag-label">回答（参考了 {{ ragChunks }} 个知识块）</div>
          <div class="rag-content">{{ ragAnswer }}</div>
        </div>
      </div>

      <!-- 文件列表 -->
      <div class="file-list">
        <div v-if="files.length === 0" class="empty">暂无文档，上传一个试试</div>
        <div v-for="f in files" :key="f.filename" class="file-item">
          <div class="file-info">
            <el-icon :size="18"><Document /></el-icon>
            <span class="file-name">{{ f.title || f.filename }}</span>
            <span class="file-chunks">{{ f.chunkCount }} 个分块</span>
          </div>
          <el-button text size="small" type="danger" @click="handleDelete(f.filename)">
            <el-icon :size="14"><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, Upload, Document, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/index.js'

const files = ref([])
const uploading = ref(false)
const ragQuestion = ref('')
const ragAnswer = ref('')
const ragChunks = ref(0)
const ragLoading = ref(false)

const loadFiles = async () => {
  try {
    const res = await request({ url: '/knowledge/files', method: 'get' })
    files.value = res.data
  } catch { /* ignore */ }
}

const handleUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { ElMessage.error('文件不能超过 5MB'); return }
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await request({ url: '/knowledge/upload', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success(res.msg)
    loadFiles()
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false; e.target.value = '' }
}

const handleDelete = async (filename) => {
  try {
    await ElMessageBox.confirm(`确定删除 ${filename}？`, '提示', { type: 'warning' })
    await request({ url: '/knowledge/delete', method: 'delete', params: { filename } })
    ElMessage.success('已删除')
    loadFiles()
  } catch { /* cancel */ }
}

const handleRagAsk = async () => {
  const q = ragQuestion.value.trim()
  if (!q) return
  ragLoading.value = true
  ragAnswer.value = ''
  try {
    const res = await request({ url: '/chat/rag', method: 'post', data: { question: q } })
    ragAnswer.value = res.data.reply
    ragChunks.value = res.data.chunksUsed || 0
  } catch { ElMessage.error('问答失败') }
  finally { ragLoading.value = false }
}

onMounted(loadFiles)
</script>

<style scoped>
.kb-container { height: 100vh; display: flex; flex-direction: column; background: var(--bg, #faf9f6); }
.kb-header { display: flex; align-items: center; padding: 14px 28px; background: var(--surface, #fff); border-bottom: 1px solid var(--border, #e8e4dd); }
.kb-title { flex: 1; margin: 0; text-align: center; font-size: 16px; font-weight: 650; color: var(--text, #3c3a37); }
.back-btn { color: var(--accent, #4a6a8a); }
.kb-body { flex: 1; overflow-y: auto; padding: 24px 28px; max-width: 700px; width: 100%; margin: 0 auto; box-sizing: border-box; }
.upload-area { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.file-hidden { display: none; }
.upload-hint { font-size: 13px; color: var(--text-muted, #a09c95); }
.rag-ask { margin-bottom: 28px; }
.rag-answer { margin-top: 14px; padding: 14px 18px; background: var(--surface, #fff); border: 1px solid var(--border, #e8e4dd); border-radius: 10px; }
.rag-label { font-size: 12px; color: var(--text-muted, #a09c95); margin-bottom: 8px; }
.rag-content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.empty { text-align: center; color: var(--text-muted, #a09c95); padding: 40px 0; font-size: 14px; }
.file-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background: var(--surface, #fff); border: 1px solid var(--border, #e8e4dd); border-radius: 10px; margin-bottom: 8px; }
.file-info { display: flex; align-items: center; gap: 8px; }
.file-name { font-size: 14px; font-weight: 550; color: var(--text, #3c3a37); }
.file-chunks { font-size: 12px; color: var(--text-muted, #a09c95); }
</style>
