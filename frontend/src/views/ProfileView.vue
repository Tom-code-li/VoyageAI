<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const profile = reactive({
  id: '',
  username: '',
  itineraryCount: 0,
  role: '',
  createTime: '',
})
const summary = ref({
  itineraryCount: 0,
})
const form = reactive({
  id: '',
  username: '',
  password: '',
})

async function loadData() {
  loading.value = true
  try {
    const [profileRes, summaryRes] = await Promise.all([
      api.get(`/user/${authStore.user.id}`),
      api.get('/dashboard/summary', { params: { userId: authStore.user.id } }),
    ])
    Object.assign(profile, profileRes)
    Object.assign(form, {
      id: profileRes.id,
      username: profileRes.username,
      password: '',
    })
    summary.value = summaryRes
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  const data = await api.put('/user', form)
  authStore.setUser({ id: data.id, username: data.username, role: data.role })
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-hero">
      <h1>我的资料</h1>
      <p>这里用于维护账号资料，并快速查看自己沉淀下来的行程与景点探索成果。</p>
    </section>

    <section class="two-column">
      <div class="glass-card" style="padding: 24px">
        <h3 style="margin-top: 0">账号信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ profile.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag type="success" effect="plain">{{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ profile.createTime?.replace('T', ' ') }}</el-descriptions-item>
          <el-descriptions-item v-if="!authStore.isAdmin" label="累计行程">
            <el-badge :value="summary.itineraryCount" type="primary">
              <el-tag effect="plain">我的行程总数</el-tag>
            </el-badge>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="glass-card" style="padding: 24px">
        <h3 style="margin-top: 0">更新资料</h3>
        <el-form label-position="top" class="editor-grid">
          <el-form-item label="用户名">
            <el-input v-model="form.username" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" />
          </el-form-item>
          <div class="inline-actions">
            <el-button type="primary" @click="saveProfile">保存修改</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </div>
</template>
