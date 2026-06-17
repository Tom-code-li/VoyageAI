<script setup>
import AMapLoader from '@amap/amap-jsapi-loader'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Picture } from '@element-plus/icons-vue'
import api from '@/services/api'
import BaikeDrawer from '@/components/BaikeDrawer.vue'
import { useBaikeDrawer } from '@/composables/useBaikeDrawer'
import { useAuthStore } from '@/stores/auth'
import { usePlannerStore } from '@/stores/planner'

const authStore = useAuthStore()
const plannerStore = usePlannerStore()
const { baikeVisible, baikeTitle, baikeUrl, openBaikeDetail } = useBaikeDrawer()

const userInput = ref('')
const sending = ref(false)
const mapReady = ref(false)
const mapContainer = ref(null)
const map = ref(null)
const markers = ref([])
const routeOverlays = ref([])
const activeDayNumber = ref(null)
const routingMode = ref('driving')
const routingBusy = ref(false)
const routeSummary = ref('')
const routeSteps = ref([])
const routeMetaByDay = ref({})
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')
const previewImageName = ref('')

const draft = computed(() => plannerStore.currentDraft)
const requestContext = computed(() => plannerStore.requestContext)
const messages = computed(() => plannerStore.messages)
const canSave = computed(() => Boolean(draft.value?.dailySchedules?.some((day) => day.attractions?.length)))
const availableDays = computed(() =>
  (draft.value?.dailySchedules || []).filter((day) => (day.attractions || []).length),
)
const activeDay = computed(() =>
  availableDays.value.find((day) => day.dayNumber === activeDayNumber.value) || availableDays.value[0] || null,
)
const saving = ref(false)

function buildRequestContext(message) {
  const base = requestContext.value || {}
  return {
    destination: draft.value?.destination || base.destination || '',
    preference: base.preference || '',
    mustVisit: base.mustVisit || [],
    message,
  }
}

async function sendMessage() {
  const message = userInput.value.trim()
  if (!message) {
    return
  }
  userInput.value = ''
  sending.value = true
  try {
    const payload = {
      ...buildRequestContext(message),
      userId: authStore.user?.id,
      itineraryId: plannerStore.itineraryId,
      currentDraft: draft.value,
      draftTitle: draft.value?.title,
      conversationHistory: messages.value.map((item) => `${(item.sender || item.role) === 'USER' || (item.sender || item.role) === 'user' ? '用户' : '系统'}: ${item.content}`),
    }
    const session = await api.post('/itinerary/ai/generate', payload)
    plannerStore.setSession(
      session,
      {
        destination: session.currentDraft?.destination,
        preference: payload.preference,
        mustVisit: payload.mustVisit,
      }
    )
    await nextTick()
    ensureActiveDay()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '生成行程失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function normalizeSort(day) {
  day.attractions.forEach((item, index) => {
    item.sortOrder = index + 1
  })
}

function removeAttraction(day, index) {
  day.attractions.splice(index, 1)
  normalizeSort(day)
  plannerStore.markModified()
  ensureActiveDay()
}

function openAttractionImage(item) {
  if (!item?.imageUrl) {
    ElMessage.warning('当前景点暂时没有可预览的图片')
    return
  }
  previewImageUrl.value = item.imageUrl
  previewImageName.value = item.name || '景点图片'
  imagePreviewVisible.value = true
}

function openAttractionDetail(item) {
  openBaikeDetail(item?.name, draft.value?.destination)
}

async function saveToDatabase() {
  if (!authStore.user?.id || !draft.value || !requestContext.value) {
    ElMessage.warning('当前草稿信息不完整，暂时无法保存')
    return
  }
  const dailySchedules = (draft.value.dailySchedules || []).map((day) => ({
    ...day,
    routeSummary: routeMetaByDay.value[day.dayNumber]?.routeSummary || day.routeSummary || '',
    routeDistance: routeMetaByDay.value[day.dayNumber]?.routeDistance || day.routeDistance || '',
    routeDuration: routeMetaByDay.value[day.dayNumber]?.routeDuration || day.routeDuration || '',
    attractions: (day.attractions || []).map((item) => ({ ...item })),
  }))
  const payload = {
    itineraryId: plannerStore.itineraryId,
    userId: authStore.user.id,
    title: draft.value.title,
    city: draft.value.destination || requestContext.value.destination,
    dailySchedules,
    modified: plannerStore.isModified,
  }
  saving.value = true
  try {
    await api.post('/itinerary/save', payload)
    plannerStore.clearDraft()
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function ensureActiveDay() {
  if (!availableDays.value.length) {
    activeDayNumber.value = null
    return
  }
  const exists = availableDays.value.some((day) => day.dayNumber === activeDayNumber.value)
  if (!exists) {
    activeDayNumber.value = availableDays.value[0].dayNumber
  }
}

function selectDay(dayNumber) {
  activeDayNumber.value = dayNumber
}

function clearRouteOverlays() {
  if (!map.value) {
    return
  }
  if (markers.value.length) {
    map.value.remove(markers.value)
    markers.value = []
  }
  if (routeOverlays.value.length) {
    map.value.remove(routeOverlays.value)
    routeOverlays.value = []
  }
  routeSummary.value = ''
  routeSteps.value = []
}

async function initMap() {
  if (map.value || !mapContainer.value) {
    return
  }
  const key = import.meta.env.VITE_AMAP_JS_KEY
  if (!key) {
    ElMessage.warning('未配置高德地图 JS Key，地图区域暂不可用')
    return
  }
  const AMap = await AMapLoader.load({
    key,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar'],
  })
  map.value = new AMap.Map(mapContainer.value, {
    viewMode: '2D',
    zoom: 11,
    center: [116.397428, 39.90923],
    mapStyle: 'amap://styles/whitesmoke',
  })
  map.value.addControl(new AMap.Scale())
  map.value.addControl(new AMap.ToolBar({ position: 'RB' }))
  mapReady.value = true
  ensureActiveDay()
  await renderMap()
}

function getActiveDayPoints() {
  return (activeDay.value?.attractions || [])
    .filter((item) => item.longitude != null && item.latitude != null)
    .map((item, index) => ({
      ...item,
      position: [Number(item.longitude), Number(item.latitude)],
      orderLabel: `${activeDay.value.dayNumber}-${index + 1}`,
    }))
}

function buildDayMarkers(points) {
  const AMap = window.AMap
  markers.value = points.map((point) => {
    const marker = new AMap.Marker({
      position: point.position,
      offset: new AMap.Pixel(-14, -28),
      label: {
        content: `<div class="agent-marker">${point.orderLabel}</div>`,
        direction: 'top',
      },
      title: point.name,
    })
    marker.setExtData(point)
    return marker
  })
  map.value.add(markers.value)
}

function drawFallbackPolyline(points) {
  const AMap = window.AMap
  const polyline = new AMap.Polyline({
    path: points.map((point) => point.position),
    strokeColor: '#14b8a6',
    strokeWeight: 5,
    strokeOpacity: 0.9,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true,
  })
  routeOverlays.value = [polyline]
  map.value.add(polyline)
  map.value.setFitView([...markers.value, polyline], false, [80, 80, 80, 80])
}

function collectRouteOverlays() {
  const all = map.value?.getAllOverlays?.() || []
  routeOverlays.value = all.filter((overlay) => !markers.value.includes(overlay))
}

async function fetchRouteFromBackend() {
  return api.post('/itinerary/route/day', {
    draft: draft.value,
    dayNumber: activeDayNumber.value,
    mode: routingMode.value,
  })
}

function drawBackendRoute(paths) {
  const AMap = window.AMap
  const overlays = []
  paths.forEach((path) => {
    const polyline = new AMap.Polyline({
      path: (path.polyline || []).map((point) => [Number(point.longitude), Number(point.latitude)]),
      strokeColor: '#14b8a6',
      strokeWeight: 5,
      strokeOpacity: 0.9,
      lineJoin: 'round',
      lineCap: 'round',
      showDir: true,
    })
    overlays.push(polyline)
  })
  routeOverlays.value = overlays
  if (overlays.length) {
    map.value.add(overlays)
    map.value.setFitView([...markers.value, ...overlays], false, [80, 80, 80, 80])
  }
}

async function renderMap() {
  if (!mapReady.value || !map.value) {
    return
  }
  clearRouteOverlays()
  const points = getActiveDayPoints()
  if (!points.length) {
    return
  }
  buildDayMarkers(points)
  if (points.length === 1) {
    map.value.setFitView(markers.value, false, [80, 80, 80, 80])
    return
  }
  routingBusy.value = true
  try {
    const routeData = await fetchRouteFromBackend()
    const routes = routeData?.routes?.length ? routeData.routes : routeData?.paths
    routeSummary.value = routeData?.summary || ''
    routeSteps.value = routes?.[0]?.steps?.slice(0, 6) || []
    if (routes?.length) {
      drawBackendRoute(routes)
    } else {
      drawFallbackPolyline(points)
    }
    if (activeDayNumber.value != null) {
      routeMetaByDay.value = {
        ...routeMetaByDay.value,
        [activeDayNumber.value]: {
          routeSummary: routeData?.summary || '',
          routeDistance: routes?.[0]?.distance || '',
          routeDuration: routes?.[0]?.duration || '',
        },
      }
    }
    if (routeData?.message && routeData.fallback) {
      ElMessage.warning(routeData.message)
    }
  } catch (error) {
    drawFallbackPolyline(points)
    ElMessage.warning('高德实际路线暂时获取失败，当前先用顺序连线展示该天行程')
  } finally {
    routingBusy.value = false
  }
}

watch(
  () => plannerStore.currentDraft,
  async () => {
    ensureActiveDay()
    await nextTick()
    await renderMap()
  },
  { deep: true },
)

watch(activeDayNumber, async () => {
  await nextTick()
  await renderMap()
})

watch(routingMode, async () => {
  await nextTick()
  await renderMap()
})

onMounted(async () => {
  await initMap()
  if (!messages.value.length) {
    plannerStore.setMessages([
      {
        sender: 'BOT',
        content: '告诉我你的目的地、出游天数、同行人和偏好，我会先给出一版可拖拽微调的行程草稿。',
      },
    ])
  }
})

onBeforeUnmount(() => {
  clearRouteOverlays()
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})
</script>

<template>
  <div class="agent-page">
    <section class="page-hero">
      <h1>智能规划 Agent</h1>
    </section>

    <section class="agent-layout glass-card">
      <div class="chat-panel">
        <div class="panel-heading">
          <div>
            <h3>对话式规划</h3>
            <p class="muted">自然语言生成、追问修改、卡片微调会同步更新同一份草稿。</p>
          </div>
        </div>

        <div class="message-list">
          <div
            v-for="(message, index) in messages"
            :key="`${message.sender || message.role}-${index}`"
            class="message-item"
            :class="(message.sender || message.role || '').toLowerCase() === 'user' ? 'user' : 'assistant'"
          >
            <span class="message-role">{{ (message.sender || message.role || '').toLowerCase() === 'user' ? '你' : 'AI' }}</span>
            <p>{{ message.content }}</p>
          </div>
        </div>

        <div v-if="draft" class="draft-board">
          <div class="draft-summary">
            <div>
              <h3>{{ draft.title }}</h3>
              <p class="muted">
                {{ draft.destination }} · 共 {{ draft.totalDays }} 天
              </p>
            </div>
          </div>

          <div class="day-filter">
            <button
              v-for="day in availableDays"
              :key="day.dayNumber"
              type="button"
              class="day-chip"
              :class="{ active: day.dayNumber === activeDayNumber }"
              @click="selectDay(day.dayNumber)"
            >
              第 {{ day.dayNumber }} 天
            </button>
          </div>

          <div class="day-stack">
            <div
              v-for="day in draft.dailySchedules"
              v-show="day.dayNumber === activeDayNumber"
              :key="day.dayNumber"
              class="day-card agent-day-card"
            >
              <div class="day-card-header">
                <div>
                  <h4>第 {{ day.dayNumber }} 天</h4>
                  <p class="muted">当前地图只展示这一天的实际路线，删除后会重新规划。</p>
                  <p v-if="routeSummary" style="margin-top: 8px">{{ routeSummary }}</p>
                  <p v-if="routeSteps.length" class="muted" style="margin-top: 8px">
                    路线提示：{{ routeSteps.map((step, index) => `${index + 1}. ${step.instruction}`).join(' / ') }}
                  </p>
                </div>
              </div>
              <div v-if="!day.attractions?.length" class="empty-day">
                <p class="muted">这一天还没有安排，继续在下方输入你的要求。</p>
              </div>
              <div
                v-for="(item, index) in day.attractions"
                :key="`${day.dayNumber}-${item.name}-${index}`"
                class="agent-attraction"
              >
                <div class="attraction-main">
                  <div class="attraction-order">{{ index + 1 }}</div>
                  <div class="attraction-copy">
                    <div class="attraction-heading">
                      <strong>{{ item.name }}</strong>
                      <el-button text type="primary" @click="openAttractionDetail(item)">查看详情</el-button>
                    </div>
                    <p>{{ item.description }}</p>
                    <p v-if="item.imageUrl" class="mini-meta" style="margin-top: 2px">已关联图片素材</p>
                    <span class="mini-meta">
                      建议 {{ item.suggestedHours }} 小时
                      <template v-if="item.longitude && item.latitude">
                        · {{ item.longitude }}, {{ item.latitude }}
                      </template>
                    </span>
                  </div>
                </div>
                <div class="attraction-tools">
                  <el-tooltip :content="item.imageUrl ? '查看景点图片' : '暂无景点图片'" placement="top">
                    <el-button circle :icon="Picture" @click="openAttractionImage(item)" />
                  </el-tooltip>
                  <el-tooltip content="删除景点" placement="top">
                    <el-button circle type="danger" plain :icon="Delete" @click="removeAttraction(day, index)" />
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="例如：给我制定四天游玩北京的计划，带老人，多吃烤鸭"
            @keyup.enter.exact.prevent="sendMessage"
          />
          <div class="input-toolbar">
            <p class="muted">如果对我推荐的行程有什么不满意尽管告诉我。</p>
            <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </div>

      <div class="map-panel">
        <div class="map-toolbar">
          <div>
            <h3>实时地图联动</h3>
            <p class="muted">
              <template v-if="activeDay">当前展示第 {{ activeDay.dayNumber }} 天的实际路线规划。</template>
              <template v-else>根据当前草稿自动打点并展示单日路线。</template>
            </p>
          </div>
          <div class="map-toolbar-actions">
            <span v-if="routingBusy" class="route-status">正在规划路线...</span>
            <el-button class="save-btn" type="primary" :loading="saving" :disabled="!canSave || saving" @click="saveToDatabase">
              保存至我的行程
            </el-button>
          </div>
        </div>

        <div ref="mapContainer" class="map-surface">
          <div v-if="!draft" class="map-empty">
            <p>先在左侧发一句需求，地图会跟着你的草稿一起活过来。</p>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="imagePreviewVisible" :title="previewImageName" width="720px">
      <img
        v-if="previewImageUrl"
        :src="previewImageUrl"
        :alt="previewImageName"
        style="display: block; width: 100%; max-height: 70vh; object-fit: cover; border-radius: 12px"
      />
    </el-dialog>

    <BaikeDrawer v-model="baikeVisible" :title="baikeTitle" :url="baikeUrl" />
  </div>
</template>

<style scoped>
.agent-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.agent-layout {
  display: grid;
  grid-template-columns: minmax(420px, 0.92fr) minmax(420px, 1.08fr);
  min-height: calc(100vh - 220px);
  overflow: hidden;
}

.chat-panel,
.map-panel {
  min-height: 0;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.84);
  border-right: 1px solid rgba(20, 184, 166, 0.12);
}

.panel-heading h3,
.map-toolbar h3,
.draft-summary h3,
.day-card-header h4 {
  margin: 0 0 6px;
}

.message-list {
  display: grid;
  gap: 12px;
  max-height: 200px;
  overflow: auto;
  padding-right: 6px;
}

.message-item {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid rgba(20, 184, 166, 0.12);
}

.message-item.user {
  background: rgba(20, 184, 166, 0.08);
}

.message-item.assistant {
  background: #f8fafc;
}

.message-role {
  font-size: 12px;
  color: #0f766e;
  font-weight: 600;
}

.draft-board {
  display: grid;
  gap: 14px;
  min-height: 0;
  flex: 1;
}

.draft-summary {
  padding: 16px 18px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(15, 118, 110, 0.06));
}

.day-filter {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.day-chip {
  height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(20, 184, 166, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  cursor: pointer;
  transition: all 0.2s ease;
}

.day-chip.active {
  border-color: #14b8a6;
  background: #14b8a6;
  color: #fff;
}

.agent-day-card {
  padding: 16px;
  border: 1px solid rgba(20, 184, 166, 0.14);
  background: #ffffff;
}

.agent-attraction {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.agent-attraction:first-of-type {
  border-top: none;
}

.attraction-main {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.attraction-order {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #14b8a6;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  flex: none;
}

.attraction-copy {
  display: grid;
  gap: 6px;
}

.attraction-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.attraction-copy p {
  color: #475569;
  line-height: 1.6;
}

.mini-meta {
  font-size: 12px;
  color: #64748b;
}

.attraction-tools {
  display: inline-flex;
  gap: 8px;
  flex: none;
}

.input-area {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.map-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  background: rgba(240, 253, 250, 0.56);
}

.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 22px 14px;
}

.map-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.route-status {
  font-size: 13px;
  color: #0f766e;
}

.map-surface {
  position: relative;
  flex: 1;
  min-height: 560px;
}

.map-empty {
  position: absolute;
  inset: 24px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(20, 184, 166, 0.28);
  border-radius: 8px;
  color: #64748b;
  z-index: 1;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.66);
}

.empty-day {
  padding: 12px 0 2px;
}

@media (max-width: 1280px) {
  .agent-layout {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    border-right: none;
    border-bottom: 1px solid rgba(20, 184, 166, 0.12);
  }
}

@media (max-width: 760px) {
  .chat-panel,
  .map-toolbar {
    padding: 16px;
  }

  .input-toolbar,
  .map-toolbar,
  .agent-attraction {
    align-items: stretch;
    flex-direction: column;
  }

  .map-toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .attraction-tools {
    justify-content: flex-end;
  }
}
</style>
