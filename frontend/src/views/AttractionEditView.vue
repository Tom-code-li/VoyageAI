<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  id: '',
  city: '',
  name: '',
  description: '',
  imageUrl: '',
  suggestedHours: 2,
})

async function loadDetail() {
  if (!route.params.id) {
    return
  }
  loading.value = true
  try {
    const data = await api.get(`/attraction/${route.params.id}`)
    form.id = data.id
    form.city = data.city
    form.name = data.name
    form.description = data.description
    form.imageUrl = data.imageUrl || ''
    form.suggestedHours = data.playTime || 2
  } finally {
    loading.value = false
  }
}

async function saveAttraction() {
  loading.value = true
  try {
    await api.put('/attraction', {
      ...form,
      operatorUserId: authStore.user?.id,
    })
    router.push('/attractions')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-hero">
      <h1>基础图鉴管理（管理员控制台）</h1>
      <p>管理员对 AI 生成和本地沉淀的公共景点数据进行人工修正。</p>
    </section>

    <section class="glass-card" style="padding: 24px">
      <el-empty v-if="!route.params.id" description="请从景点广场选择一个景点进行编辑">
        <el-button type="primary" @click="router.push('/attractions')">返回景点广场</el-button>
      </el-empty>

      <el-form v-else label-position="top" class="editor-grid">
        <div class="form-grid">
          <el-form-item label="景点 ID">
            <el-input v-model="form.id" disabled />
          </el-form-item>
          <el-form-item label="所属城市">
            <el-input v-model="form.city" placeholder="请输入城市" />
          </el-form-item>
        </div>
        <el-form-item label="景点名称">
          <el-input v-model="form.name" placeholder="请输入景点名称" />
        </el-form-item>
        <el-form-item label="景点图片 URL">
          <el-input v-model="form.imageUrl" placeholder="请输入图片 URL，后续可供行程卡片展示" />
        </el-form-item>
        <el-form-item label="建议游玩时长（小时）">
          <el-input-number v-model="form.suggestedHours" :min="0.5" :step="0.5" :max="12" />
        </el-form-item>
        <el-form-item label="景点简介">
          <el-input v-model="form.description" type="textarea" :rows="6" placeholder="请输入更准确、更完整的景点介绍" />
        </el-form-item>
        <div class="inline-actions">
          <el-button type="primary" @click="saveAttraction">保存修改</el-button>
          <el-button @click="router.push('/attractions')">返回</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>
