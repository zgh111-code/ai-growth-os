<template>
  <el-dialog
    v-model="visible"
    title="个人设置"
    width="460px"
    top="10vh"
    class="profile-dialog"
    destroy-on-close
    @close="handleClose"
  >
    <el-tabs v-model="activeTab" class="profile-tabs">
      <!-- ===== Tab 1: 基本资料 ===== -->
      <el-tab-pane label="基本资料" name="profile">
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="80px"
          class="profile-form"
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="profileForm.nickname"
              placeholder="输入新的昵称"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="头像">
            <div class="avatar-upload">
              <div class="avatar-preview" v-if="avatarPreview">
                <img :src="avatarPreview" />
              </div>
              <div class="avatar-placeholder" v-else>
                <el-icon :size="32"><User /></el-icon>
              </div>
              <input
                type="file"
                accept="image/*"
                ref="fileInput"
                class="file-input-hidden"
                @change="handleFileChange"
              />
              <el-button size="small" @click="$refs.fileInput.click()">选择图片</el-button>
              <el-button size="small" type="primary" :loading="uploading" @click="handleUploadAvatar" :disabled="!selectedFile">
                上传头像
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSaveProfile" :loading="saving">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- ===== Tab 2: 修改密码 ===== -->
      <el-tab-pane label="修改密码" name="password">
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="profile-form"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="输入当前密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="输入新密码（至少6位）"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="再次输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSavePassword" :loading="saving">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { updateProfile, updatePassword, uploadAvatar } from '../api/index.js'

// ===== 对话框显示状态 =====
const props = defineProps({
  modelValue: Boolean
})
const emit = defineEmits(['update:modelValue', 'profile-updated'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, (val) => {
  visible.value = val
})
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// ===== 当前 Tab =====
const activeTab = ref('profile')

// ===== 基本资料表单 =====
const profileFormRef = ref(null)
const profileForm = reactive({
  nickname: '',
  avatar: ''
})
const profileRules = {
  nickname: [
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ]
}

// 头像上传
const fileInput = ref(null)
const selectedFile = ref(null)
const avatarPreview = ref(null)
const uploading = ref(false)

const handleFileChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片不能超过 2MB')
    return
  }
  selectedFile.value = file
  // 本地预览
  const reader = new FileReader()
  reader.onload = (ev) => { avatarPreview.value = ev.target.result }
  reader.readAsDataURL(file)
}

const handleUploadAvatar = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const res = await uploadAvatar(selectedFile.value)
    // 更新 localStorage
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      user.avatar = res.data
      localStorage.setItem('user', JSON.stringify(user))
    }
    emit('profile-updated', { avatar: res.data })
    ElMessage.success('头像上传成功')
    selectedFile.value = null
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 从 localStorage 初始化昵称
const initProfileForm = () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      profileForm.nickname = user.nickname || ''
    }
  } catch {
    // ignore
  }
}
initProfileForm()

// ===== 密码表单 =====
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 确认密码校验
const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少需要6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

// ===== 提交状态 =====
const saving = ref(false)

// ===== 保存基本资料 =====
const handleSaveProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // 只传有值的字段
    const data = {}
    if (profileForm.nickname && profileForm.nickname.trim()) {
      data.nickname = profileForm.nickname.trim()
    }
    if (profileForm.avatar && profileForm.avatar.trim()) {
      data.avatar = profileForm.avatar.trim()
    }

    if (Object.keys(data).length === 0) {
      ElMessage.warning('没有需要修改的内容')
      return
    }

    const res = await updateProfile(data)

    // 更新 localStorage 中的用户信息
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (res.data.nickname) user.nickname = res.data.nickname
      localStorage.setItem('user', JSON.stringify(user))
    }

    ElMessage.success('资料修改成功')
    emit('profile-updated', res.data)
    visible.value = false
  } catch (error) {
    console.error('修改资料失败:', error)
  } finally {
    saving.value = false
  }
}

// ===== 保存密码 =====
const handleSavePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')

    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    activeTab.value = 'profile'
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    saving.value = false
  }
}

// ===== 关闭对话框 =====
const handleClose = () => {
  // 重置密码表单
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style scoped>
.profile-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
}

.profile-dialog :deep(.el-dialog__title) {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
}

.profile-dialog :deep(.el-dialog__body) {
  padding: 8px 24px 24px;
}

.profile-tabs {
  margin-top: 4px;
}

.profile-form {
  margin-top: 16px;
}

/* 头像上传区域 */
.avatar-upload {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.file-input-hidden { display: none; }

.avatar-input {
  margin-bottom: 12px;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef0f5;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  position: relative;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  border: 2px dashed #d0d5dd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #fafbfc;
  color: #b0b5bd;
}

.avatar-hint {
  font-size: 12px;
  color: #b0b5bd;
}
</style>
