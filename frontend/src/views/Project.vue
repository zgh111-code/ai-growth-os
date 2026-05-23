<template>
  <div class="page">
    <div class="page-header">
      <h1>项目推进</h1>
      <p>把模糊的想法，变成清晰的下一步</p>
    </div>

    <!-- Submit form -->
    <div class="card form-card">
      <div class="form-grid">
        <div class="field">
          <label>项目名称</label>
          <el-input v-model="form.projectName" placeholder="例如：个人博客、课程设计..." />
        </div>
        <div class="field">
          <label>当前状态</label>
          <el-input v-model="form.status" type="textarea" :rows="2"
            placeholder="现在进展到哪一步了？" />
        </div>
        <div class="field">
          <label>卡点 / 困难</label>
          <el-input v-model="form.blocker" type="textarea" :rows="2"
            placeholder="遇到了什么阻碍？（选填）" />
        </div>
        <div class="field">
          <label>今日进度</label>
          <el-input v-model="form.progress" type="textarea" :rows="2"
            placeholder="今天做了什么？（选填）" />
        </div>
      </div>
      <el-button type="primary" :loading="loading" @click="submit" class="submit-btn">
        让 AI 帮我拆解任务
      </el-button>
    </div>

    <!-- AI Result -->
    <div v-if="result" class="card result-card">
      <h2>AI 分析 — {{ result.projectName }}</h2>

      <div v-if="result.aiCurrentFocus" class="result-section">
        <h3>当前焦点</h3>
        <p class="highlight">{{ result.aiCurrentFocus }}</p>
      </div>

      <div class="result-section">
        <h3 class="task-title">下一步任务</h3>
        <div v-if="parsedTasks.length" class="task-list">
          <div v-for="(t, i) in parsedTasks" :key="i" class="task-item">
            <span class="task-checkbox"></span>
            <span class="task-text">{{ t.task }}</span>
            <span :class="['task-prio', t.priority]">{{ t.priority }}</span>
          </div>
        </div>
        <p v-else class="empty-hint">暂无任务拆分</p>
      </div>

      <div v-if="parsedRisks.length" class="result-section">
        <h3>风险提醒</h3>
        <ul class="simple-list">
          <li v-for="(r, i) in parsedRisks" :key="i">{{ r }}</li>
        </ul>
      </div>

      <div v-if="parsedAdvice.length" class="result-section">
        <h3>执行建议</h3>
        <ul class="simple-list">
          <li v-for="(a, i) in parsedAdvice" :key="i">{{ a }}</li>
        </ul>
      </div>
    </div>

    <!-- History -->
    <div class="card history-card">
      <h2>历史记录</h2>
      <div v-if="projects.length === 0" class="empty">还没有项目记录</div>
      <div v-for="p in projects" :key="p.id" class="project-item"
        @click="loadDetail(p)">
        <div class="project-header">
          <span class="project-name">{{ p.projectName }}</span>
          <span class="project-date">{{ p.createdAt?.slice(0, 16)?.replace('T', ' ') }}</span>
        </div>
        <p class="project-status">{{ p.status?.slice(0, 100) }}</p>
        <div v-if="p.aiCurrentFocus" class="project-next">→ {{ p.aiCurrentFocus?.slice(0, 80) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submitProject, getProjectList, getProjectDetail } from '../api/index.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const form = ref({ projectName: '', status: '', blocker: '', progress: '' })
const result = ref(null)
const projects = ref([])

const parsedTasks = computed(() => {
  try { const v = JSON.parse(result.value?.aiNextTasks); return Array.isArray(v) ? v : [] } catch { return [] }
})

const parsedRisks = computed(() => {
  try { const v = JSON.parse(result.value?.aiRisks); return Array.isArray(v) ? v : [] } catch { return [] }
})

const parsedAdvice = computed(() => {
  try { const v = JSON.parse(result.value?.aiAdvice); return Array.isArray(v) ? v : [] } catch { return [] }
})

async function submit() {
  if (!form.value.projectName.trim() || !form.value.status.trim()) {
    ElMessage.warning('请至少填写项目名称和当前状态')
    return
  }
  loading.value = true
  try {
    const res = await submitProject({
      projectName: form.value.projectName,
      status: form.value.status,
      blocker: form.value.blocker,
      progress: form.value.progress
    })
    result.value = res.data
    ElMessage.success('分析完成')
    form.value = { projectName: '', status: '', blocker: '', progress: '' }
    loadList()
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function loadList() {
  try {
    const res = await getProjectList()
    projects.value = res.data || []
  } catch { /* ignore */ }
}

async function loadDetail(p) {
  try {
    const res = await getProjectDetail(p.id)
    result.value = res.data
  } catch { /* ignore */ }
}

onMounted(loadList)
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px 80px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0; }
.page-header p { color: #86868b; margin: 4px 0 0; font-size: 14px; }

.card { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card h2 { font-size: 16px; font-weight: 600; margin: 0 0 16px; color: #1d1d1f; }

.form-grid { display: grid; gap: 16px; margin-bottom: 20px; }
.field label { display: block; font-size: 13px; font-weight: 500; color: #86868b; margin-bottom: 6px; }
.submit-btn { width: 100%; }

.result-section { margin-bottom: 16px; }
.result-section h3 { font-size: 12px; font-weight: 600; margin: 0 0 8px; color: #86868b; text-transform: uppercase; letter-spacing: 0.5px; }
.result-section p { font-size: 14px; line-height: 1.7; color: #3a3a3c; }
.result-section .highlight { background: #f5f9ff; padding: 12px 14px; border-radius: 10px; border-left: 3px solid #007aff; font-weight: 500; }
.task-title { color: #34c759 !important; }
.task-list { display: flex; flex-direction: column; gap: 8px; }
.task-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; font-size: 14px; line-height: 1.6; }
.task-checkbox { display: inline-block; width: 18px; height: 18px; border: 2px solid #d1d1d6; border-radius: 4px; flex-shrink: 0; }
.task-text { flex: 1; }
.task-prio { font-size: 11px; font-weight: 600; text-transform: uppercase; padding: 2px 8px; border-radius: 4px; }
.task-prio.high { background: #ffebeb; color: #ff3b30; }
.task-prio.medium { background: #fff3e0; color: #ff9500; }
.task-prio.low { background: #e5e5ea; color: #86868b; }
.empty-hint { color: #c7c7cc !important; font-style: italic; }

.simple-list { margin: 4px 0 0; padding-left: 18px; }
.simple-list li { font-size: 14px; line-height: 1.8; color: #3a3a3c; }

.empty { text-align: center; color: #c7c7cc; padding: 32px 0; font-size: 14px; }
.project-item { padding: 14px 0; border-bottom: 1px solid #f5f5f7; cursor: pointer; }
.project-item:hover { background: #fafafa; margin: 0 -24px; padding-left: 24px; padding-right: 24px; }
.project-header { display: flex; justify-content: space-between; align-items: center; }
.project-name { font-size: 15px; font-weight: 600; color: #1d1d1f; }
.project-date { font-size: 12px; color: #c7c7cc; }
.project-status { font-size: 13px; color: #6e6e73; margin: 4px 0; line-height: 1.5; }
.project-next { font-size: 12px; color: #007aff; margin-top: 4px; }
</style>
