<template>
  <div class="header-left">
    <span class="hl-date">{{ dateStr }}</span><span class="hl-divider">|</span>
    <span class="hl-week">{{ weekStr }}</span><span class="hl-divider">|</span>
    <span class="hl-time">{{ timeStr }}</span><span class="hl-divider">|</span>
    <span class="hl-weather">{{ weatherText }} {{ weatherTemp }}</span>
  </div>
</template>

<script setup>
/**
 * 顶部时钟（独立子组件）
 * 时钟每秒更新只触发本组件重渲染，避免父组件整屏模板每秒全量 diff
 */
defineOptions({ name: 'MapHeaderClock' })
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

defineProps({
  weatherText: { type: String, default: '加载中...' },
  weatherTemp: { type: String, default: '--' }
})

const now = ref(new Date())
let timer = null

const dateStr = computed(() => {
  const d = now.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})
const timeStr = computed(() => now.value.toLocaleTimeString('zh-CN', { hour12: false }))
const weekStr = computed(() => ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.value.getDay()])

onMounted(() => { timer = setInterval(() => { now.value = new Date() }, 1000) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style lang="scss" scoped>
.header-left { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--map-header-text);
  .hl-divider { color: var(--map-divider-color); }
  .hl-time { font-weight: 600; color: var(--map-primary); letter-spacing: 1px; }
}
</style>
