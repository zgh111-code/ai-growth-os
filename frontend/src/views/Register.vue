<template>
  <div class="login-page dot-bg">
    <div class="bg-orbs" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>
    <div class="login-card glass-card-solid anim-up">
      <div class="login-logo">G</div>
      <h1 class="login-title">创建账号</h1>
      <p class="login-sub">开启你的 AI 学习之旅</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="EditPen" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-bottom">已有账号？<router-link to="/login">登录</router-link></p>
    </div>
    <p class="login-footer">AI 学习助手 — 陪伴你的成长之路</p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '../api/index.js'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [{ max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}
const handleRegister = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    await register({ username: form.username, nickname: form.nickname || form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) { console.error('注册失败:', e) }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: linear-gradient(160deg, #F8FAFD 0%, #F0F5FF 40%, #F6F9FD 100%);
  padding: 20px;
}
.bg-orbs { position: fixed; inset: 0; pointer-events: none; overflow: hidden; z-index: 0; }
.orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.12; }
.orb-1 { width: 400px; height: 400px; background: #7CCBFF; top: -120px; right: -120px; }
.orb-2 { width: 350px; height: 350px; background: #BDD3FF; bottom: -100px; left: -100px; }

.login-card {
  position: relative; z-index: 1;
  width: 100%; max-width: 400px;
  border-radius: 24px; padding: 40px 32px;
  text-align: center;
}
.login-logo {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, #4F8CFF, #7CCBFF);
  color: #fff; display: inline-flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700; margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(79,140,255,0.25);
}
.login-title { font-size: 24px; font-weight: 750; color: #1C2640; margin: 0 0 4px; }
.login-sub { font-size: 14px; color: #909BB5; margin: 0 0 28px; }

.login-btn {
  width: 100%; height: 44px; font-size: 15px; font-weight: 600;
  border-radius: 14px !important;
  background: linear-gradient(135deg, #4F8CFF, #3B6FDF) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(79,140,255,0.2) !important;
  transition: all 0.2s !important;
}
.login-btn:hover { box-shadow: 0 6px 24px rgba(79,140,255,0.3) !important; transform: translateY(-1px); }

.login-bottom { font-size: 13px; color: #A0ACC5; margin: 16px 0 0; }
.login-bottom a { color: #4F8CFF; font-weight: 600; text-decoration: none; }
.login-bottom a:hover { text-decoration: underline; }
.login-footer { position: relative; z-index: 1; font-size: 11px; color: #B5C0D8; margin-top: 32px; }

:deep(.el-input__wrapper) {
  background: #F8FAFD !important;
  border: 1px solid #E4EAF4 !important;
  border-radius: 14px !important;
  box-shadow: none !important;
  padding: 2px 14px !important;
  transition: all 0.2s !important;
}
:deep(.el-input__wrapper:hover) { border-color: #BDD3FF !important; }
:deep(.el-input__wrapper.is-focus) {
  border-color: #4F8CFF !important;
  background: #fff !important;
  box-shadow: 0 0 0 3px rgba(79,140,255,0.08) !important;
}
:deep(.el-input__inner) { height: 44px !important; font-size: 14px !important; }
:deep(.el-input__prefix) { margin-right: 8px !important; }
:deep(.el-input__prefix-inner) { color: #909BB5 !important; }
:deep(.el-form-item) { margin-bottom: 14px !important; }
</style>
