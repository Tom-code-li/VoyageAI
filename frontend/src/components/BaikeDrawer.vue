<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '百科详情',
  },
  url: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

watch(
  () => [props.modelValue, props.url],
  ([visible, url]) => {
    loading.value = Boolean(visible && url)
  },
  { immediate: true },
)

function handleFrameLoad() {
  loading.value = false
}

function openInNewTab() {
  if (!props.url) {
    return
  }
  window.open(props.url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    class="baike-drawer"
    direction="rtl"
    size="62%"
    append-to-body
  >
    <template #header>
      <div class="baike-drawer-header">
        <div>
          <h3>{{ title || '百科详情' }}</h3>
          <p>右侧抽屉内快速查看百科内容，左侧行程和对话会保持在原位。</p>
        </div>
        <el-button link type="primary" @click="openInNewTab">新窗口打开</el-button>
      </div>
    </template>

    <div class="baike-drawer-body">
      <div v-if="url" v-loading="loading" class="baike-frame-shell">
        <iframe
          :key="url"
          class="baike-frame"
          :src="url"
          referrerpolicy="no-referrer"
          @load="handleFrameLoad"
        />
      </div>
      <el-empty v-else description="暂无可查看的百科详情" />

      <div class="baike-drawer-tip">
        如果百科页面加载较慢，或者你的浏览器限制了外部页面嵌入，可以点击右上角“新窗口打开”。
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.baike-drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.baike-drawer-header h3 {
  margin: 0;
  font-size: 20px;
}

.baike-drawer-header p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.baike-drawer-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.baike-frame-shell {
  flex: 1;
  min-height: 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.baike-frame {
  width: 100%;
  height: 100%;
  min-height: 72vh;
  border: none;
  background: #fff;
}

.baike-drawer-tip {
  font-size: 12px;
  color: #64748b;
}

.baike-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px 12px;
}

.baike-drawer :deep(.el-drawer__body) {
  padding: 0 24px 20px;
  overflow: hidden;
}

@media (max-width: 900px) {
  .baike-drawer-header {
    flex-direction: column;
    align-items: stretch;
  }

  .baike-frame {
    min-height: 62vh;
  }
}
</style>
