<script setup>
import { computed } from 'vue'
import { getStatusMeta } from '@/utils/status'

const props = defineProps({
  status: {
    type: Number,
    default: 0,
  },
})

const meta = computed(() => getStatusMeta(props.status))
</script>

<template>
  <el-tag :type="meta.type" effect="dark" round>
    {{ meta.label }}
  </el-tag>
</template>
