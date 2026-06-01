<template>
  <div class="login-container">
    <!-- 动画背景粒子 -->
    <div class="bg-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)" />
    </div>

    <!-- 左侧品牌面板 -->
    <div class="brand-panel">
      <div class="brand-overlay" />
      <div class="brand-content">
        <div class="brand-logo">
          <div class="logo-icon">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="12" fill="url(#logoGrad)" />
              <path d="M14 16h20M14 24h14M14 32h8" stroke="white" stroke-width="2.5" stroke-linecap="round" />
              <circle cx="34" cy="30" r="6" fill="white" opacity="0.9" />
              <path d="M32.5 28.5L35 31l4-3" stroke="#1a1a2e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
          <h1 class="brand-name">SmartExpense</h1>
          <p class="brand-subtitle">智能差旅报销系统</p>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
            </div>
            <span>AI 发票识别，秒级录入</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4"/><path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/></svg>
            </div>
            <span>智能审批流，高效协作</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <span>异常检测，合规保障</span>
          </div>
        </div>
        <p class="brand-footer">© 2026 SmartExpense Team</p>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2>{{ mode === 'login' ? '欢迎回来' : '创建账户' }}</h2>
          <p>{{ mode === 'login' ? '登录您的账户以继续使用' : '填写信息，注册后即可发起报销' }}</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleSubmit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              class="custom-input"
            />
          </el-form-item>

          <template v-if="mode === 'register'">
            <el-form-item prop="realName">
              <el-input
                v-model="form.realName"
                placeholder="姓名"
                :prefix-icon="UserFilled"
                class="custom-input"
              />
            </el-form-item>

            <el-form-item prop="deptId">
              <el-select
                v-model="form.deptId"
                placeholder="选择部门"
                class="custom-select"
                style="width: 100%"
              >
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </template>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :placeholder="mode === 'login' ? '密码' : '密码（至少4位）'"
              :prefix-icon="Lock"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              :prefix-icon="Lock"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleSubmit"
            >
              <span v-if="!loading">{{ mode === 'login' ? '登 录' : '注 册' }}</span>
              <span v-else>{{ mode === 'login' ? '验证中...' : '注册中...' }}</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <a class="mode-toggle" @click="toggleMode">
            {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？去登录' }}
          </a>
          <span v-if="mode === 'login'" class="demo-hint">
            <span class="hint-dot" />
            演示账号：admin / 123456
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { registerUser } from '@/api/user'
import { getDeptList } from '@/api/dept'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const mode = ref<'login' | 'register'>('login')
const deptOptions = ref<{ id: number; deptName: string }[]>([])

const form = reactive({
  username: '',
  password: '',
  realName: '',
  deptId: undefined as number | undefined,
  confirmPassword: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码至少4位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const particleStyle = (i: number) => {
  const size = 2 + Math.random() * 4
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    animationDelay: `${Math.random() * 8}s`,
    animationDuration: `${4 + Math.random() * 6}s`
  }
}

const toggleMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  formRef.value?.clearValidate()
}

const loadDepts = async () => {
  try {
    const res: any = await getDeptList()
    deptOptions.value = res.data || []
  } catch {
    /* 忽略 */
  }
}

const handleSubmit = () => {
  if (mode.value === 'login') handleLogin()
  else handleRegister()
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } catch {
      ElMessage.error('用户名或密码错误')
    } finally {
      loading.value = false
    }
  })
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await registerUser({
        username: form.username,
        password: form.password,
        realName: form.realName,
        deptId: form.deptId!
      })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      form.password = ''
      form.confirmPassword = ''
    } catch {
      /* 错误已由拦截器提示 */
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => loadDepts())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

// ===== 容器 =====
.login-container {
  display: flex;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #f8f9fc;
}

// ===== 背景粒子 =====
.bg-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  background: $color-primary;
  border-radius: 50%;
  opacity: 0;
  animation: float-up linear infinite;
}

@keyframes float-up {
  0% { opacity: 0; transform: translateY(100vh) scale(0); }
  10% { opacity: 0.06; }
  90% { opacity: 0.06; }
  100% { opacity: 0; transform: translateY(-10vh) scale(1.5); }
}

// ===== 左侧品牌面板 =====
.brand-panel {
  position: relative;
  flex: 0 0 45%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #1a1a2e 40%, #16213e 100%);
  overflow: hidden;
  z-index: 1;
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(79, 70, 229, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 30%, rgba(90, 200, 250, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(52, 199, 89, 0.08) 0%, transparent 50%);
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 60px 48px;
  max-width: 420px;
}

.brand-logo {
  margin-bottom: 48px;

  .logo-icon {
    display: inline-block;
    margin-bottom: 20px;
    filter: drop-shadow(0 8px 24px rgba(79, 70, 229, 0.3));
  }

  .brand-name {
    font-size: 32px;
    font-weight: 700;
    color: #fff;
    letter-spacing: -0.5px;
    margin-bottom: 8px;
  }

  .brand-subtitle {
    font-size: 15px;
    color: rgba(255, 255, 255, 0.55);
    letter-spacing: 1px;
    font-weight: 400;
  }
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 64px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  text-align: left;
  transition: all 0.3s ease;
  cursor: default;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(255, 255, 255, 0.12);
    transform: translateX(4px);
  }

  .feature-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: rgba(79, 70, 229, 0.2);
    color: $color-accent;
    flex-shrink: 0;
  }
}

.brand-footer {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

// ===== 右侧表单面板 =====
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 40px;

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8px;
    letter-spacing: -0.3px;
  }

  p {
    font-size: 14px;
    color: $text-secondary;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.custom-input {
  :deep(.el-input__wrapper) {
    padding: 6px 16px;
    border-radius: 12px;
    background: #fff;
    border: 1px solid #e8e8ed;
    box-shadow: none;
    transition: all 0.3s ease;

    &:hover {
      border-color: #d0d0da;
    }

    &.is-focus {
      border-color: $color-primary;
      box-shadow: 0 0 0 3px rgba(26, 26, 46, 0.08);
    }
  }

  :deep(.el-input__inner) {
    height: 44px;
    font-size: 15px;
  }

  :deep(.el-input__prefix) {
    color: #b0b0bc;
    margin-right: 4px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d4a 100%);
  border: none;
  transition: all 0.3s ease;
  margin-top: 4px;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 24px rgba(26, 26, 46, 0.3);
  }

  &:active {
    transform: translateY(0);
  }
}

// ===== 表单底部 =====
.form-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 32px;
}

.mode-toggle {
  font-size: 13px;
  color: $color-primary;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.75;
    text-decoration: underline;
  }
}

.custom-select {
  :deep(.el-select__wrapper) {
    min-height: 44px;
    border-radius: 12px;
    background: #fff;
    border: 1px solid #e8e8ed;
    box-shadow: none;
    transition: all 0.3s ease;

    &:hover {
      border-color: #d0d0da;
    }

    &.is-focused {
      border-color: $color-primary;
      box-shadow: 0 0 0 3px rgba(26, 26, 46, 0.08);
    }
  }

  :deep(.el-select__placeholder) {
    color: #b0b0bc;
  }
}

.demo-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-placeholder;
  padding: 8px 20px;
  background: #f5f5f7;
  border-radius: 20px;
}

.hint-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $color-success;
  animation: pulse-dot 2s ease infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

// ===== 响应式 =====
@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 24px;
  }

  .form-card {
    max-width: 100%;
  }
}
</style>
