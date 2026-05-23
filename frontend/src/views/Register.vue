<template>
  <div class="page">
    <div class="card">
      <h1 class="title">注册</h1>
      <p class="subtitle">创建一个账号开始使用</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        class="form"
        @keyup.enter="handleRegister"
      >
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
          <el-button type="primary" :loading="loading" class="btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="footer">已有账号？<router-link to="/login">登录</router-link></p>
    </div>
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

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
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

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      nickname: form.nickname || form.username,
      password: form.password
    })

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg, #faf9f6);
}

.card {
  width: 380px;
  padding: 40px 40px 36px;
  background: var(--surface, #fff);
  border: 1px solid var(--border, #e8e4dd);
  border-radius: var(--radius-lg, 16px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.06));
}

.title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 650;
  color: var(--text, #3c3a37);
  letter-spacing: -0.3px;
}

.subtitle {
  margin: 0 0 24px;
  color: var(--text-secondary, #787570);
  font-size: 14px;
}

.form :deep(.el-input__wrapper) {
  background: var(--bg, #faf9f6);
  border: 1px solid var(--border, #e8e4dd);
  border-radius: var(--radius, 10px);
  box-shadow: none !important;
  padding: 2px 12px;
  transition: border-color 0.15s;
}
.form :deep(.el-input__wrapper:hover) { border-color: #c5c0b8; }
.form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--accent, #5b5bd6);
  background: #fff;
  box-shadow: 0 0 0 3px var(--accent-bg, rgba(91,91,214,0.06)) !important;
}
.form :deep(.el-input__inner) { height: 44px; font-size: 14.5px; }
.form :deep(.el-input__prefix) { margin-right: 8px; }
.form :deep(.el-input__prefix-inner) { color: var(--text-muted, #a09c95); }
.form :deep(.el-form-item) { margin-bottom: 14px; }

.btn {
  width: 100%;
  height: 44px;
  font-size: 14px;
  font-weight: 550;
  border-radius: var(--radius, 10px);
  background: var(--accent, #5b5bd6);
  border: none;
  letter-spacing: 0.5px;
}
.btn:hover { background: var(--accent-hover, #4a4ac4); }

.footer {
  text-align: center;
  margin: 18px 0 0;
  color: var(--text-muted, #a09c95);
  font-size: 13.5px;
}
.footer a {
  color: var(--accent, #5b5bd6);
  text-decoration: none;
  font-weight: 550;
  margin-left: 2px;
}
.footer a:hover { text-decoration: underline; }
</style>
