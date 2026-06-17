<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import StatCard from '@/components/StatCard.vue'
import api from '@/services/api'

const loading = ref(false)
const summary = ref({
  attractionCount: 0,
  itineraryCount: 0,
  userCount: 0,
})
const cityDistribution = ref([])
const cityChartRef = ref()
let cityChart

async function loadData() {
  loading.value = true
  try {
    const [summaryRes, cityRes, userCountRes] = await Promise.all([
      api.get('/dashboard/summary'),
      api.get('/attraction/cityDistribution'),
      api.get('/dashboard/userCount'),
    ])
    summary.value = {
      ...summary.value,
      ...summaryRes,
      userCount: userCountRes,
    }
    cityDistribution.value = cityRes || []
    await nextTick()
    renderCityChart()
  } finally {
    loading.value = false
  }
}

function renderCityChart() {
  if (!cityChartRef.value) {
    return
  }
  cityChart?.dispose()
  cityChart = echarts.init(cityChartRef.value)
  cityChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 18, top: 32, bottom: 48 },
    xAxis: {
      type: 'category',
      data: cityDistribution.value.map((item) => item.name),
      axisTick: { show: false },
      axisLabel: { color: '#60717f', interval: 0, rotate: 18 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(20, 184, 166, 0.12)' } },
    },
    series: [
      {
        name: '景点数量',
        type: 'bar',
        barWidth: 34,
        data: cityDistribution.value.map((item) => item.value),
        itemStyle: {
          borderRadius: [12, 12, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5eead4' },
            { offset: 0.55, color: '#14b8a6' },
            { offset: 1, color: '#0f766e' },
          ]),
        },
      },
    ],
  })
}

function handleResize() {
  cityChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  cityChart?.dispose()
})
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-hero">
      <h1>系统数据看板</h1>
      <p>集中查看系统注册用户、公共景点图鉴、行程沉淀与内容素材覆盖的核心指标。</p>
    </section>

    <section class="stat-grid">
      <StatCard title="当前系统用户数量" :value="summary.userCount" subtitle="" tone="#14b8a6" />
      <StatCard title="图鉴景点总数" :value="summary.attractionCount" subtitle="" tone="#0f766e" />
      <StatCard title="系统行程总数" :value="summary.itineraryCount" subtitle="" tone="#14b8a6" />
      <StatCard title="平均内容覆盖" :value="summary.itineraryCount ? Math.max(1, Math.round(summary.attractionCount / summary.itineraryCount)) : 0" subtitle="每条行程平均景点数（估算）" tone="#0f766e" />
    </section>

    <section class="glass-card" style="padding: 22px">
      <div class="action-bar">
        <div>
          <h3 style="margin: 0 0 8px">不同城市的景点数量分布</h3>
          <p class="muted" style="margin: 0">基于公共景点图鉴按城市聚合统计，便于观察数据覆盖情况。</p>
        </div>
        <el-tag type="success" effect="plain">ECharts</el-tag>
      </div>
      <div ref="cityChartRef" class="chart-box"></div>
    </section>
  </div>
</template>
