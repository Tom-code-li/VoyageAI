<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/planner')
})
</script>

<template>
  <div class="glass-card redirect-card">
    <p>正在跳转到新的智能规划工作台...</p>
  </div>
</template>

<style scoped>
.redirect-card {
  padding: 24px;
}
</style>
