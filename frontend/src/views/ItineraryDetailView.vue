<script setup>
import { Back, Delete } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'
import BaikeDrawer from '@/components/BaikeDrawer.vue'
import { useBaikeDrawer } from '@/composables/useBaikeDrawer'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const { baikeVisible, baikeTitle, baikeUrl, openBaikeDetail } = useBaikeDrawer()

async function loadData() {
  loading.value = true
  try {
    detail.value = await api.get(`/itinerary/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function removeDetail(detailId) {
  await ElMessageBox.confirm('确认移除这个景点安排吗？', '提示', { type: 'warning' })
  await api.delete(`/itinerary/detail/${detailId}`)
  loadData()
}

function openAttractionDetail(item) {
  openBaikeDetail(item?.name, detail.value?.city)
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section v-if="detail" class="page-hero">
      <div class="action-bar">
        <div>
          <h1>{{ detail.title }}</h1>
          <p>{{ detail.city }} · 共 {{ detail.totalDays }} 天，这里会展示每天的路线安排摘要、景点图文与行程顺序。</p>
        </div>
        <div class="inline-actions">
          <el-button plain @click="router.back()">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="detail" class="day-stack">
      <div v-for="day in detail.dailySchedules" :key="day.dayNumber" class="glass-card timeline-card">
        <div class="day-card-header">
          <div>
            <h3 style="margin: 0">第 {{ day.dayNumber }} 天</h3>
            <p class="muted" style="margin: 8px 0 0">{{ day.routeSummary || '这一天暂未生成路线摘要。' }}</p>
            <p class="muted" style="margin: 8px 0 0" v-if="day.routeDistance || day.routeDuration">
              {{ day.routeDistance || '距离待补充' }} · {{ day.routeDuration || '时长待补充' }}
            </p>
          </div>
        </div>

        <el-timeline v-if="day.attractions?.length">
          <el-timeline-item
            v-for="item in day.attractions"
            :key="item.detailId"
            :timestamp="`建议 ${item.suggestedHours || 2} 小时`"
            placement="top"
          >
            <div class="attraction-item">
              <div
                v-if="item.imageUrl"
                style="height: 180px; border-radius: 8px; margin-bottom: 14px; background-size: cover; background-position: center"
                :style="{ backgroundImage: `linear-gradient(180deg, rgba(15,118,110,0.08), rgba(15,118,110,0.18)), url(${item.imageUrl})` }"
              ></div>
              <div class="action-bar">
                <strong>{{ item.name }}</strong>
                <div class="inline-actions">
                  <el-button text type="primary" @click="openAttractionDetail(item)">查看详情</el-button>
                  <el-button text type="danger" @click="removeDetail(item.detailId)">
                    <el-icon><Delete /></el-icon>
                    删除该安排
                  </el-button>
                </div>
              </div>
              <p class="muted" style="line-height: 1.7; margin-bottom: 0">{{ item.description }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="这一天还没有景点安排" />
      </div>
    </section>

    <section v-else class="glass-card empty-panel">
      <el-empty description="没有找到这个行程，可能已经被删除。">
        <el-button type="primary" @click="router.push('/itineraries')">返回我的行程</el-button>
      </el-empty>
    </section>

    <BaikeDrawer v-model="baikeVisible" :title="baikeTitle" :url="baikeUrl" />
  </div>
</template>
