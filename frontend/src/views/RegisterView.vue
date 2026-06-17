<script setup>
import { DataAnalysis, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'

const router = useRouter()
const loading = ref(false)
const registerFormRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
})

async function handleRegister() {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await api.post('/auth/register', {
        username: form.username,
        password: form.password,
      })
      ElMessage.success('注册成功')
      router.push('/login')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-main-card">
      <section class="brand-block">
        <div class="brand-inner">
          <div class="header-tags">
            <el-tag size="large" effect="dark" type="success" class="custom-tag">账号初始化 × 用户隔离</el-tag>
            <el-tag size="large" effect="plain" type="info" class="ml-2">草稿归档 × 行程持久化</el-tag>
          </div>

          <h1 class="project-title">VoyageAI<br />智能伴旅引擎</h1>
          <p class="project-desc">
            创建演示账号后，就可以体验从 AI 规划、路线联动到行程沉淀入库的完整业务闭环。
          </p>

          <div class="feature-grid">
            <div class="glass-card">
              <el-icon :size="22" class="icon-cyan mb-1"><EditPen /></el-icon>
              <h3>专属草稿</h3>
              <p>注册后每位用户都拥有自己的草稿上下文、聊天记录和行程数据，方便完整演示按用户隔离的业务流程。</p>
            </div>
            <div class="glass-card">
              <el-icon :size="22" class="icon-cyan mb-1"><DataAnalysis /></el-icon>
              <h3>持续沉淀</h3>
              <p>AI 推荐的景点在保存时会自动完成标准化、坐标补全和知识入库，逐渐形成系统自己的本地旅游知识库。</p>
            </div>
          </div>
        </div>
        <div class="cyan-glow"></div>
      </section>

      <section class="form-block">
        <div class="form-inner">
          <div class="form-header">
            <h2>创建账号</h2>
            <p>先准备一个演示账号，后续我的行程会按用户维度展示。</p>
          </div>

          <el-form
            ref="registerFormRef"
            :model="form"
            :rules="rules"
            label-position="top"
            @submit.prevent="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="例如：demo_user"
                size="large"
                class="modern-input"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="请输入密码"
                size="large"
                class="modern-input"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
                size="large"
                class="modern-input"
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册并进入系统
            </el-button>
          </el-form>

          <div class="form-footer">
            已有账号？
            <router-link to="/login" class="link">返回登录</router-link>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  --el-color-primary: #14b8a6;
  --el-color-primary-light-3: #5eead4;
  --el-color-primary-light-5: #99f6e4;
  --el-color-primary-light-8: #ccfbf1;
  --el-color-primary-light-9: #f0fdfa;
  --el-color-primary-dark-2: #0f766e;

  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(180deg, #f0fdfa 0%, #eef2f5 100%);
  padding: 8vh 4vw;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.auth-main-card {
  display: flex;
  width: 100%;
  max-width: 980px;
  min-height: 520px;
  max-height: 84vh;
  background-color: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(15, 118, 110, 0.08), 0 1px 3px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(20, 184, 166, 0.08);
  overflow: hidden;
}

.brand-block {
  position: relative;
  flex: 1.1;
  background: linear-gradient(135deg, #f6fdfc 0%, #e6f7f4 100%);
  color: #2c3e50;
  display: flex;
  align-items: center;
  padding: 2.5rem 3rem;
  overflow: hidden;
  border-right: 1px solid rgba(20, 184, 166, 0.06);
}

.brand-inner {
  position: relative;
  z-index: 2;
  width: 100%;
}

.custom-tag {
  background-color: #14b8a6 !important;
  border-color: #14b8a6 !important;
}

.project-title {
  font-size: 2.8rem;
  line-height: 1.25;
  font-weight: 800;
  margin: 1.5rem 0 1rem 0;
  letter-spacing: -0.01em;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.project-desc {
  font-size: 1rem;
  line-height: 1.6;
  color: #506b67;
  margin-bottom: 2.5rem;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.glass-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(15, 118, 110, 0.03);
  border-radius: 14px;
  padding: 1.25rem;
  transition: all 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(15, 118, 110, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

.glass-card h3 {
  font-size: 1.05rem;
  margin: 0.4rem 0;
  font-weight: 600;
  color: #111827;
}

.glass-card p {
  font-size: 0.85rem;
  color: #66807c;
  line-height: 1.5;
  margin: 0;
}

.icon-cyan {
  color: #14b8a6;
}

.cyan-glow {
  position: absolute;
  bottom: -10%;
  right: -10%;
  width: 60%;
  height: 60%;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.12) 0%, rgba(255,255,255,0) 70%);
  filter: blur(50px);
  z-index: 1;
}

.form-block {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  padding: 2.5rem 3rem;
}

.form-inner {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 2rem;
}

.form-header h2 {
  font-size: 1.8rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.4rem 0;
}

.form-header p {
  color: #6b7280;
  font-size: 0.95rem;
  margin: 0;
}

:deep(.modern-input .el-input__wrapper) {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02) inset;
  border-radius: 8px;
  padding: 8px 12px;
  background-color: #f9fafb;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

:deep(.modern-input .el-input__wrapper:hover) {
  background-color: #f3f4f6;
}

:deep(.modern-input .el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1) !important;
  background-color: #ffffff;
}

.submit-btn {
  width: 100%;
  margin-top: 1.25rem;
  border-radius: 8px;
  font-weight: 600;
  height: 46px;
  background-color: #14b8a6;
  border-color: #14b8a6;
  box-shadow: 0 4px 10px rgba(20, 184, 166, 0.15);
  transition: all 0.2s ease;
}

.submit-btn:hover {
  background-color: #0f766e;
  border-color: #0f766e;
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(20, 184, 166, 0.25);
}

.form-footer {
  margin-top: 2rem;
  text-align: center;
  color: #6b7280;
  font-size: 0.9rem;
}

.link {
  color: #14b8a6;
  text-decoration: none;
  font-weight: 600;
  margin-left: 0.4rem;
  transition: color 0.2s;
}

.link:hover {
  color: #0f766e;
}

.mb-1 { margin-bottom: 0.25rem; }
.ml-2 { margin-left: 0.5rem; }

@media (max-width: 960px) {
  .auth-main-card {
    flex-direction: column;
    max-height: none;
  }

  .brand-block,
  .form-block {
    padding: 2rem 1.5rem;
  }
}

@media (max-width: 640px) {
  .auth-container {
    padding: 24px 16px;
  }

  .project-title {
    font-size: 2.2rem;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
