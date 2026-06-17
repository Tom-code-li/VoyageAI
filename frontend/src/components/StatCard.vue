<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
  value: {
    type: [String, Number],
    default: '',
  },
  subtitle: {
    type: String,
    default: '',
  },
  tone: {
    type: String,
    default: '#0f766e',
  },
})
</script>

<template>
  <div class="glass-card stat-card">
    <span class="stat-line" :style="{ background: tone }"></span>
    <p class="muted">{{ title }}</p>
    <h3>{{ value }}</h3>
    <small>{{ subtitle }}</small>
  </div>
</template>

<style scoped>
.stat-card {
  position: relative;
  padding: 22px 22px 20px;
  overflow: hidden;
}

.stat-line {
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 5px;
}

.stat-card p {
  margin: 12px 0 8px;
}

.stat-card h3 {
  margin: 0 0 8px;
  font-size: 30px;
  font-family: var(--heading-font);
}

.stat-card small {
  color: var(--muted);
}
</style>
