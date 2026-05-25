<template>
  <div class="kb-container">
    <header class="kb-top glass-card-solid">
      <button class="kb-back" @click="$router.push('/chat')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
        返回聊天
      </button>
      <h1 class="kb-title">知识库</h1>
      <span></span>
    </header>

    <div class="kb-body">
      <div class="kb-grid">
        <div class="kb-main">
          <!-- 上传 -->
          <div class="card">
            <div class="card-head"><h3>上传文档</h3></div>
            <div class="upload-row">
              <input type="file" ref="fileInput" class="hidden" @change="handleUpload" accept=".txt,.md,.java,.py,.js,.html,.css,.json,.xml,.yml,.sql" />
              <button class="submit-btn" @click="$refs.fileInput.click()" :disabled="uploading">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                {{ uploading ? '上传中...' : '选择文件上传' }}
              </button>
              <span class="up-hint">支持 TXT、MD、代码文件（≤5MB）</span>
            </div>
          </div>

          <!-- 知识库问答 -->
          <div class="card">
            <div class="card-head"><h3>知识库问答</h3></div>
            <div class="rag-row">
              <input v-model="ragQuestion" class="glass-input" placeholder="基于你的文档提问..." @keyup.enter="handleRagAsk" :disabled="ragLoading" />
              <button class="submit-btn ask-btn" @click="handleRagAsk" :disabled="ragLoading">{{ ragLoading ? '思考中...' : '提问' }}</button>
            </div>
            <div v-if="ragAnswer" class="rag-result anim-up">
              <div class="rag-meta">参考了 {{ ragChunks }} 个知识块</div>
              <div class="rag-content">{{ ragAnswer }}</div>
            </div>
          </div>

          <!-- 文件列表 -->
          <div class="card">
            <div class="card-head"><h3>我的文档</h3><span class="badge">{{ files.length }}</span></div>
            <div v-if="files.length === 0" class="empty-mini"><p>暂无文档，上传第一个试试</p></div>
            <div v-for="f in files" :key="f.filename" class="file-item">
              <div class="fi-left">
                <div class="fi-icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                </div>
                <div>
                  <span class="fi-name">{{ f.title || f.filename }}</span>
                  <span class="fi-meta">{{ f.chunkCount }} 个分块</span>
                </div>
              </div>
              <button class="fi-del" @click="handleDelete(f.filename)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 侧边 -->
        <div class="kb-side">
          <div class="side-card focus-side">
            <span class="focus-badge">知识库说明</span>
            <p class="focus-msg">上传你的学习笔记、课程资料，AI 可以基于这些内容回答你的问题</p>
          </div>
          <div class="side-card">
            <div class="side-title">文档统计</div>
            <div class="side-stat-row"><span>文档总数</span><span class="side-stat-val">{{ files.length }}</span></div>
            <div class="side-stat-row"><span>总分块数</span><span class="side-stat-val">{{ totalChunks }}</span></div>
          </div>
          <div class="side-card">
            <div class="side-title">支持格式</div>
            <ul class="tips-list">
              <li>.txt 文本文件</li>
              <li>.md Markdown</li>
              <li>.java .py .js 代码</li>
              <li>.html .css .json 等</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/index.js'

const files = ref([]); const uploading = ref(false)
const ragQuestion = ref(''); const ragAnswer = ref(''); const ragChunks = ref(0); const ragLoading = ref(false)
const totalChunks = computed(() => files.value.reduce((sum, f) => sum + (f.chunkCount || 0), 0))

const loadFiles = async () => { try { const r = await request({ url: '/knowledge/files', method: 'get' }); files.value = r.data } catch { /* */ } }
const handleUpload = async (e) => {
  const file = e.target.files?.[0]; if (!file) return
  if (file.size > 5*1024*1024) { ElMessage.error('文件不能超过 5MB'); return }
  uploading.value = true
  try { const f = new FormData(); f.append('file', file); const r = await request({ url: '/knowledge/upload', method: 'post', data: f, headers: { 'Content-Type': 'multipart/form-data' } }); ElMessage.success(r.msg); loadFiles() } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false; e.target.value = '' }
}
const handleDelete = async (filename) => {
  try { await ElMessageBox.confirm(`删除 ${filename}？`, '提示', { type: 'warning' }); await request({ url: '/knowledge/delete', method: 'delete', params: { filename } }); ElMessage.success('已删除'); loadFiles() } catch { /* */ }
}
const handleRagAsk = async () => {
  const q = ragQuestion.value.trim(); if (!q) return
  ragLoading.value = true; ragAnswer.value = ''
  try { const r = await request({ url: '/chat/rag', method: 'post', data: { question: q } }); ragAnswer.value = r.data.reply; ragChunks.value = r.data.chunksUsed || 0 } catch { ElMessage.error('问答失败') }
  finally { ragLoading.value = false }
}
onMounted(loadFiles)
</script>

<style scoped>
.kb-container { height: 100vh; display: flex; flex-direction: column; background: linear-gradient(170deg, #F8FAFD, #F0F5FF); }
.kb-top { display: flex; align-items: center; padding: 14px 24px; border-bottom: 1px solid rgba(79,140,255,0.06); }
.kb-back { display: flex; align-items: center; gap: 5px; background: none; border: none; cursor: pointer; font-size: 13px; color: #4F8CFF; font-family: inherit; }
.kb-title { flex: 1; margin: 0; text-align: center; font-size: 16px; font-weight: 650; color: #1C2640; }
.kb-body { flex: 1; overflow-y: auto; padding: 24px 28px; max-width: 1100px; width: 100%; margin: 0 auto; box-sizing: border-box; }
.kb-grid { display: grid; grid-template-columns: 1fr 260px; gap: 16px; align-items: start; }

.card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 18px 20px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 14px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-head h3 { font-size: 14px; font-weight: 650; color: #1C2640; margin: 0; }
.badge { font-size: 11px; color: #909BB5; font-weight: 550; padding: 2px 8px; border-radius: 10px; background: rgba(79,140,255,0.05); }

.hidden { display: none; }
.upload-row { display: flex; align-items: center; gap: 12px; }
.submit-btn { display: flex; align-items: center; gap: 6px; padding: 9px 18px; border-radius: 10px; border: none; background: linear-gradient(135deg, #4F8CFF, #3B6FDF); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; box-shadow: 0 2px 8px rgba(79,140,255,0.15); transition: all 0.15s; font-family: inherit; white-space: nowrap; }
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(79,140,255,0.22); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ask-btn { padding: 9px 14px; font-size: 13px; }
.up-hint { font-size: 12px; color: #A0ACC5; }

.rag-row { display: flex; gap: 8px; }
.glass-input { flex: 1; border: 1px solid #E4EAF4; border-radius: 12px; padding: 9px 14px; font-size: 13.5px; font-family: inherit; color: #1C2640; background: #F8FAFD; outline: none; box-sizing: border-box; }
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.07); }
.rag-result { margin-top: 12px; padding: 14px 16px; background: #F8FAFD; border: 1px solid #BDD3FF; border-radius: 12px; }
.rag-meta { font-size: 10.5px; color: #909BB5; margin-bottom: 6px; }
.rag-content { font-size: 13px; line-height: 1.6; color: #3C4A6E; }

.empty-mini { text-align: center; padding: 24px 0; } .empty-mini p { font-size: 13px; color: #A0ACC5; margin: 0; }
.file-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 10px; transition: background 0.15s; }
.file-item:hover { background: rgba(79,140,255,0.02); }
.fi-left { display: flex; align-items: center; gap: 10px; }
.fi-icon { width: 34px; height: 34px; border-radius: 9px; background: rgba(79,140,255,0.06); color: #4F8CFF; display: flex; align-items: center; justify-content: center; }
.fi-name { display: block; font-size: 13px; font-weight: 550; color: #1C2640; } .fi-meta { font-size: 11px; color: #A0ACC5; }
.fi-del { background: none; border: none; cursor: pointer; color: #BDD3FF; padding: 5px; border-radius: 6px; }
.fi-del:hover { color: #FF6B5B; background: rgba(255,107,91,0.06); }

.kb-side { position: sticky; top: 24px; }
.side-card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 16px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 12px; }
.side-title { font-size: 11.5px; font-weight: 650; color: #909BB5; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 10px; }
.focus-side { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border-color: #BDD3FF; }
.focus-badge { font-size: 10px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 6px; }
.focus-msg { font-size: 12.5px; color: #3C4A6E; margin: 0; line-height: 1.5; }
.side-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 12.5px; color: #5B6780; }
.side-stat-val { font-weight: 650; color: #4F8CFF; }
.tips-list { margin: 0; padding-left: 14px; display: flex; flex-direction: column; gap: 5px; }
.tips-list li { font-size: 12px; color: #5B6780; line-height: 1.4; }

@media (max-width: 800px) { .kb-grid { grid-template-columns: 1fr; } .kb-side { position: static; } }
</style>
