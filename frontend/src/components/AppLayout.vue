<script setup>
import {
  ArrowDown,
  Compass,
  EditPen,
  Histogram,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const menuItems = computed(() => {
  if (authStore.isAdmin) {
    return [
      { index: '/dashboard', label: '系统数据看板', icon: Histogram },
      { index: '/attractions', label: '基础图鉴管理', icon: Compass },
    ]
  }
  return [
    { index: '/attractions', label: '热门景点广场', icon: Compass },
    { index: '/planner', label: '智能规划', icon: EditPen },
    { index: '/itineraries', label: '我的行程', icon: Histogram },
  ]
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/attractions/edit')) return '/attractions'
  if (route.path.startsWith('/planner/draft')) return '/planner'
  if (route.path.startsWith('/itineraries')) return '/itineraries'
  return route.path
})

const roleLabel = computed(() => (authStore.isAdmin ? '管理员' : '普通用户'))
const pageTitle = computed(() => {
  if (authStore.isAdmin && route.name === 'attractions') {
    return '基础图鉴管理'
  }
  return route.meta?.title || '数据看板'
})

function handleDropdown(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="layout-root">
    <el-aside width="252px" class="layout-aside">
      <div class="brand-box">
        <div class="brand-icon">AI</div>
        <div>
          <strong>VoyageAI</strong>
          <p>{{ roleLabel }}</p>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="layout-menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p class="muted">{{ authStore.isAdmin ? '公共图鉴与系统数据监控' : '个人行程规划与图鉴浏览' }}</p>
        </div>

        <el-dropdown trigger="click" @command="handleDropdown">
          <button class="avatar-button" type="button">
            <span class="avatar-dot">{{ authStore.user?.username?.slice(0, 1)?.toUpperCase() || 'U' }}</span>
            <span>{{ authStore.user?.username }}</span>
            <el-tag size="small" effect="plain">{{ roleLabel }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人资料
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-root {
  min-height: 100vh;
  --el-color-primary: #14b8a6;
  --el-color-primary-light-3: #5eead4;
  --el-color-primary-light-5: #99f6e4;
  --el-color-primary-light-7: #ccfbf1;
  --el-color-primary-light-9: #f0fdfa;
  --el-color-primary-dark-2: #0f766e;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  background: rgba(240, 253, 250, 0.86);
  border-right: 1px solid rgba(20, 184, 166, 0.14);
}

.brand-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px 22px;
}

.brand-box strong {
  display: block;
  font-size: 18px;
  color: #0f172a;
}

.brand-box p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.brand-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #14b8a6;
  color: white;
  font-weight: 700;
}

.layout-menu {
  border-right: none;
  background: transparent;
}

.layout-menu :deep(.el-menu-item) {
  height: 48px;
  margin: 4px 0;
  border-radius: 8px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: rgba(20, 184, 166, 0.12);
  color: #0f766e;
  font-weight: 600;
}

.layout-header {
  height: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px 0;
}

.layout-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #0f172a;
}

.layout-main {
  padding: 20px 28px 32px;
}

.avatar-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(20, 184, 166, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: #0f172a;
  cursor: pointer;
}

.avatar-dot {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #14b8a6;
  color: #fff;
  font-weight: 700;
}

@media (max-width: 960px) {
  .layout-aside {
    display: none;
  }

  .layout-header,
  .layout-main {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
