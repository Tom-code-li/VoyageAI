<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import BaikeDrawer from '@/components/BaikeDrawer.vue'
import { useBaikeDrawer } from '@/composables/useBaikeDrawer'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const { baikeVisible, baikeTitle, baikeUrl, openBaikeDetail } = useBaikeDrawer()
const loading = ref(false)
const carouselLoading = ref(false)
const total = ref(0)
const records = ref([])
const createVisible = ref(false)
const createLoading = ref(false)
const carouselCards = ref([])
const createForm = reactive({
  city: '',
  name: '',
  description: '',
  imageUrl: '',
  suggestedHours: 2,
})

const query = reactive({
  current: 1,
  size: 8,
  keyword: '',
  city: '',
})

async function loadData() {
  loading.value = true
  try {
    const data = await api.get('/attraction/page', { params: query })
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadCarousel() {
  carouselLoading.value = true
  try {
    const data = await api.get('/attraction/top', { params: { limit: 3 } })
    carouselCards.value = (data || []).map((item) => ({
      id: item.id,
      city: item.city,
      title: item.name,
      desc: item.description || `${item.city} 的热门景点，当前引用热度较高，适合优先了解并加入行程安排。`,
      image: item.imageUrl || '',
      referenceCount: item.referenceCount || 0,
    }))
  } finally {
    carouselLoading.value = false
  }
}

function resetQuery() {
  query.current = 1
  query.keyword = ''
  query.city = ''
  loadData()
}

function openDetail(row) {
  if (authStore.isAdmin) {
    router.push(`/attractions/edit/${row.id}`)
    return
  }
  openBaikeDetail(row?.name, row?.city)
}

function openCreateDialog() {
  createForm.city = query.city || ''
  createForm.name = ''
  createForm.description = ''
  createForm.imageUrl = ''
  createForm.suggestedHours = 2
  createVisible.value = true
}

async function createAttraction() {
  createLoading.value = true
  try {
    await api.post('/attraction', {
      ...createForm,
      operatorUserId: authStore.user?.id,
    })
    ElMessage.success('景点已添加')
    createVisible.value = false
    query.current = 1
    loadCarousel()
    loadData()
  } finally {
    createLoading.value = false
  }
}

async function removeAttraction(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」吗？删除后不可恢复。`, '删除景点', {
    type: 'warning',
  })
  await api.delete(`/attraction/${row.id}`, {
    params: { operatorUserId: authStore.user?.id },
  })
  ElMessage.success('景点已删除')
  loadCarousel()
  loadData()
}

onMounted(() => {
  loadCarousel()
  loadData()
})
</script>

<template>
  <div class="page-shell">
    <section class="page-hero">
      <h1>{{ authStore.isAdmin ? '基础图鉴管理' : '热门景点广场' }}</h1>
      <p>{{ authStore.isAdmin ? '管理员可维护公共景点图鉴，修正 AI 沉淀内容。' : '浏览公共景点图鉴，查看城市灵感与建议游玩时长。' }}</p>
    </section>

    <section class="glass-card" style="padding: 10px" v-loading="carouselLoading">
      <el-carousel v-if="carouselCards.length" height="220px" indicator-position="outside">
        <el-carousel-item v-for="(item, index) in carouselCards" :key="item.id">
          <div class="plaza-carousel-card" :style="{ backgroundImage: `linear-gradient(90deg, rgba(15, 118, 110, 0.86), rgba(20, 184, 166, 0.28)), url(${item.image})` }">
            <el-tag effect="dark" type="success">{{ item.city }}</el-tag>
            <h2>{{ item.title }}</h2>
            <p>{{ item.desc }}</p>
            <span class="carousel-rank">当前引用第 {{ index + 1 }} 名 · {{ item.referenceCount }} 次</span>
          </div>
        </el-carousel-item>
      </el-carousel>
      <el-empty v-else description="暂无可展示的热门景点" />
    </section>

    <section class="glass-card" style="padding: 24px">
      <div class="form-grid">
        <el-input v-model="query.keyword" placeholder="按景点名称模糊搜索" clearable />
        <el-input v-model="query.city" placeholder="按城市筛选，例如：北京" clearable />
      </div>
      <div class="inline-actions" style="margin-top: 16px">
        <el-button type="primary" @click="query.current = 1; loadData()">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button v-if="authStore.isAdmin" plain type="primary" @click="openCreateDialog">添加景点</el-button>
      </div>
    </section>

    <section class="glass-card" style="padding: 10px 10px 16px" v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="景点名称" min-width="180" />
        <el-table-column prop="city" label="城市" width="130" />
        <el-table-column prop="referenceCount" label="引用次数" width="110" />
        <el-table-column label="建议时长" width="120">
          <template #default="{ row }">
            {{ row.playTime || 2 }} 小时
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="320" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">
              {{ authStore.isAdmin ? '完善' : '查看详情' }}
            </el-button>
            <el-button v-if="authStore.isAdmin" text type="danger" @click="removeAttraction(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          layout="prev, pager, next, total"
          :total="total"
          @current-change="loadData"
        />
      </div>
    </section>

    <el-dialog v-model="createVisible" title="添加景点" width="640px">
      <el-form label-position="top" class="editor-grid">
        <div class="form-grid">
          <el-form-item label="所属城市">
            <el-input v-model="createForm.city" placeholder="请输入城市" />
          </el-form-item>
          <el-form-item label="景点名称">
            <el-input v-model="createForm.name" placeholder="请输入景点名称" />
          </el-form-item>
        </div>
        <el-form-item label="建议游玩时长（小时）">
          <el-input-number v-model="createForm.suggestedHours" :min="0.5" :step="0.5" :max="12" />
        </el-form-item>
        <el-form-item label="景点简介">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="请输入景点简介" />
        </el-form-item>
        <el-form-item label="景点图片 URL">
          <el-input v-model="createForm.imageUrl" placeholder="请输入图片 URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="inline-actions" style="justify-content: flex-end">
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="createLoading" @click="createAttraction">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <BaikeDrawer v-model="baikeVisible" :title="baikeTitle" :url="baikeUrl" />
  </div>
</template>

<style scoped>
.plaza-carousel-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 28px;
  border-radius: 14px;
  background-size: cover;
  background-position: center;
  color: #fff;
  overflow: hidden;
}

.plaza-carousel-card h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.02em;
}

.plaza-carousel-card p {
  max-width: 520px;
  margin: 0;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

.carousel-rank {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.92);
}
</style>
