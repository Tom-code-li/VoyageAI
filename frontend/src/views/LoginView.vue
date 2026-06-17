<script setup>
import { DataAnalysis, Location } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const loginFormRef = ref(null)

const form = reactive({
  username: '',
  password: '',
})

// 表单校验规则
const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

async function handleLogin() {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const user = await api.post('/auth/login', form)
        authStore.setUser(user)
        router.push(route.query.redirect || (user.role === 'ADMIN' ? '/dashboard' : '/itineraries'))
      } finally {
        loading.value = false
      }
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
            <el-tag size="large" effect="dark" type="success" class="custom-tag">AI定制 × 柔性降级</el-tag>
            <el-tag size="large" effect="plain" type="info" class="ml-2">动态热力 × 数据自演进</el-tag>
          </div>
          
          <h1 class="project-title">VoyageAI<br />智能伴旅引擎</h1>
          <p class="project-desc">
            你的专属 AI 行程管家。一键生成个性化路线，智能网罗热门打卡地
          </p>

          <div class="feature-grid">
            <div class="glass-card">
              <el-icon :size="22" class="icon-cyan mb-1"><Location /></el-icon>
              <h3>智能规划</h3>
              <p>通过高内聚的 Prompt 设计，将用户模糊的旅行偏好转化为结构化的纯 JSON 行程路线。</p>
            </div>
            <div class="glass-card">
              <el-icon :size="22" class="icon-cyan mb-1"><DataAnalysis /></el-icon>
              <h3>本地沉淀</h3>
              <p>打破人工录入基础数据的限制。AI 生成的新景点在保存时会自动进行清洗并无感入库，实现低成本的数据冷启动。</p>
            </div>
          </div>
        </div>
        <div class="cyan-glow"></div>
      </section>

      <section class="form-block">
        <div class="form-inner">
          <div class="form-header">
            <h2>欢迎登录</h2>
            <p>先进入系统，再开始规划你的演示行程</p>
          </div>

          <el-form 
            ref="loginFormRef"
            :model="form" 
            :rules="rules"
            label-position="top" 
            @submit.prevent="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="form.username" 
                placeholder="请输入用户名" 
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
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            
            <el-button 
              type="primary" 
              size="large" 
              class="submit-btn" 
              :loading="loading" 
              @click="handleLogin"
            >
              登录进入系统
            </el-button>
          </el-form>

          <div class="form-footer">
            还没有账号？
            <router-link to="/register" class="link">立即注册</router-link>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<style scoped>
/* 局部重写本页面的 Element Plus 核心主色为现代浅青色/泰尔色 */
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
  /* 整体背景采用非常淡的青灰色，衬托中央主体 */
  background: linear-gradient(180deg, #f0fdfa 0%, #eef2f5 100%);
  /* 强行留出上下 8vh (视口高度8%) 的空隙，确保绝对不会撑满屏幕 */
  padding: 8vh 4vw; 
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 中央一体化悬浮大卡片 */
.auth-main-card {
  display: flex;
  width: 100%;
  max-width: 980px;
  /* 降低最小高度，并限制最大高度为 84vh，绝不碰顶 */
  min-height: 520px;
  max-height: 84vh;
  background-color: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(15, 118, 110, 0.08), 0 1px 3px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(20, 184, 166, 0.08);
  overflow: hidden;
}

/* 左侧：浅青色品牌区 */
.brand-block {
  position: relative;
  flex: 1.1;
  background: linear-gradient(135deg, #f6fdfc 0%, #e6f7f4 100%);
  color: #2c3e50;
  display: flex;
  align-items: center;
  /* 调小内边距以适应新高度 */
  padding: 2.5rem 3rem; 
  overflow: hidden;
  border-right: 1px solid rgba(20, 184, 166, 0.06);
}

.brand-inner {
  position: relative;
  z-index: 2;
  width: 100%;
}

/* 定制项目标签颜色 */
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

/* 浅青色通透毛玻璃卡片 */
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

/* 装饰光晕 */
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

/* 右侧：操作表单区 */
.form-block {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  /* 调小内边距以适应新高度 */
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

/* 精细化调节输入框 */
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

/* 提交按钮 */
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

/* 辅助间隔 */
.mb-1 { margin-bottom: 0.25rem; }
.ml-2 { margin-left: 0.5rem; }

/* 响应式断点：中等及以下屏幕自动堆叠 */
@media (max-width: 868px) {
  .auth-main-card {
    flex-direction: column;
    min-height: auto;
  }
  .brand-block {
    padding: 2.5rem 2rem;
    border-right: none;
    border-bottom: 1px solid rgba(20, 184, 166, 0.06);
  }
  .form-block {
    padding: 3rem 2rem;
  }
}
</style>
