<template>
  <div class="page">
    <div class="page-hero">
      <div class="ph-left">
        <p class="page-eyebrow">项目推进 · {{ today }}</p>
        <h1 class="page-title">把想法变成清晰的下一步</h1>
      </div>
      <div class="ph-pills">
        <span class="ph-pill">🚀 {{ projects.length }} 个项目</span>
      </div>
    </div>

    <div class="page-grid">
      <div class="col-main">
        <!-- 表单 -->
        <div class="card">
          <div class="card-head"><h3>提交项目状态</h3></div>
          <div class="form-cols">
            <input v-model="form.projectName" class="glass-input" placeholder="项目名称" />
            <textarea v-model="form.status" class="glass-input" rows="2" placeholder="现在进展到哪一步了？"></textarea>
            <div class="form-row-2">
              <textarea v-model="form.blocker" class="glass-input" rows="2" placeholder="卡点（选填）"></textarea>
              <textarea v-model="form.progress" class="glass-input" rows="2" placeholder="今日进度（选填）"></textarea>
            </div>
          </div>
          <button class="submit-btn" :disabled="loading" @click="submit">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            {{ loading ? 'AI 分析中...' : 'AI 拆解任务' }}
          </button>
        </div>

        <!-- AI 结果 -->
        <div v-if="result" class="card result-card anim-up">
          <div class="card-head"><h3>AI 分析 — {{ result.projectName }}</h3></div>
          <div v-if="result.aiCurrentFocus" class="focus-box"><span class="fc-chip">当前焦点</span><p>{{ result.aiCurrentFocus }}</p></div>
          <div class="result-sec"><h4>📋 下一步任务</h4>
            <div v-if="parsedTasks.length" class="task-cards">
              <div v-for="(t,i) in parsedTasks" :key="i" class="task-card"><span class="tk-check"></span><span class="tk-text">{{ t.task }}</span><span :class="['tk-tag', t.priority]">{{ t.priority }}</span></div>
            </div><p v-else class="muted">暂无</p>
          </div>
          <div v-if="parsedRisks.length" class="result-sec"><h4>⚠️ 风险</h4><ul><li v-for="(r,i) in parsedRisks" :key="i">{{ r }}</li></ul></div>
          <div v-if="parsedAdvice.length" class="result-sec"><h4>💡 建议</h4><ul><li v-for="(a,i) in parsedAdvice" :key="i">{{ a }}</li></ul></div>
        </div>

        <!-- 历史 -->
        <div class="card">
          <div class="card-head"><h3>历史项目</h3><span class="badge">{{ projects.length }}</span></div>
          <div v-if="projects.length === 0" class="empty-mini"><p>还没有项目记录</p></div>
          <div v-for="p in projects.slice(0, 8)" :key="p.id" class="pj-item" @click="loadDetail(p)">
            <div class="pj-head"><span class="pj-name">{{ p.projectName }}</span><span class="pj-date">{{ p.createdAt?.slice(0,16)?.replace('T',' ') }}</span></div>
            <p class="pj-status">{{ p.status?.slice(0, 100) }}</p>
            <p v-if="p.aiCurrentFocus" class="pj-next">→ {{ p.aiCurrentFocus?.slice(0, 80) }}</p>
          </div>
        </div>
      </div>

      <!-- 侧边 -->
      <div class="col-side">
        <div class="side-card focus-side">
          <span class="focus-badge">项目执行技巧</span>
          <p class="focus-msg">先拆解最小可行动的第一步，然后立即执行。完美主义是最大的敌人。</p>
        </div>
        <div class="side-card">
          <div class="side-title">项目统计</div>
          <div class="side-stat-row"><span>总项目数</span><span class="side-stat-val">{{ projects.length }}</span></div>
          <div class="side-stat-row"><span>本周活跃</span><span class="side-stat-val">{{ weekActive }}</span></div>
        </div>
        <div class="side-card">
          <div class="side-title">AI 能帮你</div>
          <ul class="tips-list">
            <li>拆解复杂任务为小步骤</li>
            <li>识别潜在风险和卡点</li>
            <li>排序优先级</li>
            <li>提供执行顺序建议</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submitProject, getProjectList, getProjectDetail } from '../api/index.js'
import { ElMessage } from 'element-plus'

const today = new Date().toLocaleDateString('zh-CN', { month:'long', day:'numeric', weekday:'short' })
const loading = ref(false)
const form = ref({ projectName: '', status: '', blocker: '', progress: '' })
const result = ref(null)
const projects = ref([])

const weekActive = computed(() => {
  const weekAgo = new Date(); weekAgo.setDate(weekAgo.getDate() - 7)
  return projects.value.filter(p => new Date(p.createdAt) >= weekAgo).length
})
const parsedTasks = computed(() => { try { const v = JSON.parse(result.value?.aiNextTasks); return Array.isArray(v)?v:[] } catch { return [] } })
const parsedRisks = computed(() => { try { const v = JSON.parse(result.value?.aiRisks); return Array.isArray(v)?v:[] } catch { return [] } })
const parsedAdvice = computed(() => { try { const v = JSON.parse(result.value?.aiAdvice); return Array.isArray(v)?v:[] } catch { return [] } })

async function submit() {
  if (!form.value.projectName.trim() || !form.value.status.trim()) { ElMessage.warning('请填写项目名称和当前状态'); return }
  loading.value = true
  try { const res = await submitProject({ projectName: form.value.projectName, status: form.value.status, blocker: form.value.blocker, progress: form.value.progress }); result.value = res.data; ElMessage.success('分析完成'); form.value = { projectName: '', status: '', blocker: '', progress: '' }; loadList() } catch { /* */ }
  finally { loading.value = false }
}
async function loadList() { try { const r = await getProjectList(); projects.value = r.data || [] } catch { /* */ } }
async function loadDetail(p) { try { const r = await getProjectDetail(p.id); result.value = r.data } catch { /* */ } }
onMounted(loadList)
</script>

<style scoped>
.page { max-width: 1140px; margin: 0 auto; padding: 24px 20px 80px; }
.page-hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 16px; }
.page-eyebrow { font-size: 11px; font-weight: 650; color: #4F8CFF; letter-spacing: 0.06em; margin: 0 0 4px; }
.page-title { font-size: 24px; font-weight: 750; color: #1C2640; margin: 0; letter-spacing: -0.02em; }
.ph-pills { display: flex; gap: 6px; flex-shrink: 0; }
.ph-pill { font-size: 11.5px; color: #5B6780; padding: 4px 12px; border-radius: 16px; background: rgba(255,255,255,0.6); border: 1px solid #E4EAF4; font-weight: 500; }

.page-grid { display: grid; grid-template-columns: 1fr 270px; gap: 16px; align-items: start; }
.card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 18px 20px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 14px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-head h3 { font-size: 14px; font-weight: 650; color: #1C2640; margin: 0; }
.badge { font-size: 11px; color: #909BB5; font-weight: 550; padding: 2px 8px; border-radius: 10px; background: rgba(79,140,255,0.05); }

.form-cols { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.glass-input { width: 100%; border: 1px solid #E4EAF4; border-radius: 12px; padding: 9px 14px; font-size: 13.5px; font-family: inherit; color: #1C2640; background: #F8FAFD; resize: vertical; outline: none; box-sizing: border-box; }
.glass-input:focus { border-color: #4F8CFF; box-shadow: 0 0 0 3px rgba(79,140,255,0.07); }
.submit-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 7px; padding: 10px 20px; border-radius: 12px; border: none; background: linear-gradient(135deg, #4F8CFF, #3B6FDF); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; box-shadow: 0 3px 12px rgba(79,140,255,0.18); transition: all 0.2s; font-family: inherit; }
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 5px 18px rgba(79,140,255,0.25); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.result-card { background: linear-gradient(155deg, #F8FAFD, #F0F5FF); border-color: #BDD3FF; }
.focus-box { background: #fff; border: 1px solid #BDD3FF; border-radius: 12px; padding: 12px 14px; margin-bottom: 12px; }
.fc-chip { font-size: 10.5px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.08); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 6px; }
.focus-box p { font-size: 13px; color: #3C4A6E; margin: 0; line-height: 1.5; }
.result-sec { margin-bottom: 12px; }
.result-sec h4 { font-size: 11.5px; font-weight: 650; color: #5B6780; margin: 0 0 6px; }
.result-sec ul { margin: 0; padding-left: 16px; } .result-sec li { font-size: 13px; line-height: 1.6; color: #3C4A6E; }
.muted { color: #A0ACC5 !important; font-style: italic; }
.task-cards { display: flex; flex-direction: column; gap: 6px; }
.task-card { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border-radius: 10px; border: 1px solid #E4EAF4; }
.tk-check { width: 16px; height: 16px; border: 2px solid #BDD3FF; border-radius: 4px; flex-shrink: 0; }
.tk-text { flex: 1; font-size: 13px; color: #3C4A6E; }
.tk-tag { font-size: 9.5px; font-weight: 700; padding: 2px 7px; border-radius: 4px; }
.tk-tag.high { background: #FEF2F2; color: #EF4444; } .tk-tag.medium { background: #FFFBEB; color: #F59E0B; } .tk-tag.low { background: #F0F4FA; color: #909BB5; }

.empty-mini { text-align: center; padding: 20px 0; } .empty-mini p { font-size: 13px; color: #A0ACC5; margin: 0; }
.pj-item { padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: background 0.15s; border-bottom: 1px solid #F0F4FA; }
.pj-item:last-child { border: none; } .pj-item:hover { background: rgba(79,140,255,0.02); }
.pj-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.pj-name { font-size: 14px; font-weight: 650; color: #1C2640; } .pj-date { font-size: 11px; color: #A0ACC5; }
.pj-status { font-size: 12.5px; color: #5B6780; margin: 3px 0; line-height: 1.4; }
.pj-next { font-size: 12px; color: #4F8CFF; margin-top: 3px; font-weight: 520; }

.col-side { position: sticky; top: 24px; }
.side-card { background: rgba(255,255,255,0.50); backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); border-radius: 16px; padding: 16px; border: 1px solid rgba(79,140,255,0.12); box-shadow: 0 1px 6px rgba(79,140,255,0.03); margin-bottom: 12px; }
.side-title { font-size: 11.5px; font-weight: 650; color: #909BB5; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 10px; }
.focus-side { background: linear-gradient(135deg, #F0F5FF, #E0ECFF); border-color: #BDD3FF; }
.focus-badge { font-size: 10px; font-weight: 650; color: #4F8CFF; background: rgba(79,140,255,0.10); padding: 2px 8px; border-radius: 6px; display: inline-block; margin-bottom: 6px; }
.focus-msg { font-size: 12.5px; color: #3C4A6E; margin: 0; line-height: 1.5; }
.side-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 12.5px; color: #5B6780; }
.side-stat-val { font-weight: 650; color: #4F8CFF; }
.tips-list { margin: 0; padding-left: 14px; display: flex; flex-direction: column; gap: 5px; }
.tips-list li { font-size: 12px; color: #5B6780; line-height: 1.4; }

@media (max-width: 800px) { .page-grid { grid-template-columns: 1fr; } .col-side { position: static; } .form-row-2 { grid-template-columns: 1fr; } }
</style>
