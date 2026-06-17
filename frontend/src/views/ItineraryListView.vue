<script setup>
import { Delete, View } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])

async function loadData() {
  loading.value = true
  try {
    records.value = await api.get(`/itinerary/user/${authStore.user.id}`)
  } finally {
    loading.value = false
  }
}

async function removeItinerary(id) {
  await ElMessageBox.confirm('删除后将同步移除该行程下的明细记录，是否继续？', '提示', {
    type: 'warning',
  })
  await api.delete(`/itinerary/${id}`)
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-hero">
      <h1>我的行程</h1>
      <p>这里展示你保存过的全部行程，以路线摘要、景点数量和封面图片为主，方便继续编辑和浏览。</p>
    </section>

    <section class="glass-card" style="padding: 22px">
      <div class="inline-actions">
        <router-link to="/planner">
          <el-button type="primary">新增一份行程</el-button>
        </router-link>
      </div>
    </section>

    <section v-if="records.length" class="section-grid" style="grid-template-columns: repeat(auto-fit, minmax(320px, 1fr))">
      <div v-for="item in records" :key="item.id" class="glass-card" style="padding: 22px">
        <div
          v-if="item.coverImageUrl"
          style="height: 180px; border-radius: 8px; margin-bottom: 18px; background-size: cover; background-position: center"
          :style="{ backgroundImage: `linear-gradient(180deg, rgba(15,118,110,0.08), rgba(15,118,110,0.22)), url(${item.coverImageUrl})` }"
        ></div>
        <div class="action-bar">
          <div>
            <h3 style="margin: 0 0 10px">{{ item.title }}</h3>
            <p class="muted" style="margin: 0">{{ item.city }}</p>
          </div>
        </div>
        <el-divider />
        <p class="muted" style="line-height: 1.7; min-height: 46px">
          {{ item.routeSummary || '当前行程已保存，可继续进入详情查看每天的路径安排与景点内容。' }}
        </p>
        <div class="action-bar" style="margin-top: 14px">
          <span class="muted">{{ item.totalDays }} 天 / {{ item.attractionCount }} 个景点</span>
          <span class="muted">创建于 {{ item.createTime?.replace('T', ' ') }}</span>
        </div>
        <div class="inline-actions" style="margin-top: 18px">
          <el-button type="primary" @click="router.push(`/itineraries/${item.id}`)">
            <el-icon><View /></el-icon>
            查看详情
          </el-button>
          <el-button type="danger" plain @click="removeItinerary(item.id)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </section>

    <section v-else class="glass-card empty-panel">
      <el-empty description="还没有保存过行程，先去智能规划页生成一份吧。">
        <router-link to="/planner">
          <el-button type="primary">去智能规划</el-button>
        </router-link>
      </el-empty>
    </section>
  </div>
</template>
