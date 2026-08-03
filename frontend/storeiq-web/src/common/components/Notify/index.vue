<script setup>
import { Bell } from "@element-plus/icons-vue"
import { getNotificationListApi, getUnreadCountApi, markReadApi, markAllReadApi } from '@@/apis/storeiq/notifications'

const unreadCount = ref(0)
const notifyList = ref([])
const popoverWidth = 350

async function loadNotifications() {
  try {
    const [listRes, countRes] = await Promise.all([getNotificationListApi(), getUnreadCountApi()])
    notifyList.value = listRes.data || []
    unreadCount.value = countRes.data || 0
  } catch { /* */ }
}

async function handleMarkRead(item) {
  if (item.isRead) return
  try { await markReadApi(item.id); item.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1) } catch { /* */ }
}

async function handleMarkAllRead() {
  try { await markAllReadApi(); notifyList.value.forEach(n => n.isRead = 1); unreadCount.value = 0 } catch { /* */ }
}

const typeConfig = { stock_alert: 'warning', restock_suggest: 'info', monthly_report: 'success', cost_reminder: 'danger' }
function getTypeTag(type) { return typeConfig[type] || 'info' }
function getTypeLabel(type) {
  const map = { stock_alert: '库存预警', restock_suggest: '补货建议', monthly_report: '月报', cost_reminder: '成本提醒' }
  return map[type] || type
}

onMounted(() => loadNotifications())
</script>

<template>
  <div class="notify">
    <el-popover placement="bottom" :width="popoverWidth" trigger="click" @show="loadNotifications">
      <template #reference>
        <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0">
          <el-tooltip effect="dark" content="消息通知" placement="bottom">
            <el-icon :size="20"><Bell /></el-icon>
          </el-tooltip>
        </el-badge>
      </template>
      <template #default>
        <div class="notify-top-bar">
          <span class="notify-title">消息通知</span>
          <el-button v-if="unreadCount > 0" size="small" link type="primary" @click="handleMarkAllRead">全部已读</el-button>
        </div>
        <el-scrollbar max-height="360px">
          <div v-if="notifyList.length === 0" class="notify-empty">暂无通知</div>
          <div
            v-for="item in notifyList" :key="item.id"
            class="notify-item" :class="{ unread: !item.isRead }"
            @click="handleMarkRead(item)"
          >
            <div class="ni-header">
              <el-tag :type="getTypeTag(item.type)" size="small" effect="plain">{{ getTypeLabel(item.type) }}</el-tag>
              <span class="ni-time">{{ item.createTime?.substring(0, 16) }}</span>
            </div>
            <div class="ni-title">{{ item.title }}</div>
            <div class="ni-content">{{ item.content }}</div>
          </div>
        </el-scrollbar>
      </template>
    </el-popover>
  </div>
</template>

<style lang="scss" scoped>
.notify-top-bar { display: flex; justify-content: space-between; align-items: center; padding: 8px 4px; }
.notify-title { font-weight: 600; font-size: 14px; }
.notify-empty { text-align: center; padding: 40px; color: var(--el-text-color-secondary); }
.notify-item {
  padding: 12px 8px; border-radius: 6px; margin-bottom: 4px; cursor: pointer; transition: background 0.2s;
  &:hover { background: var(--el-fill-color-lighter); }
  &.unread { background: var(--el-color-primary-light-9); }
  .ni-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
  .ni-time { font-size: 11px; color: var(--el-text-color-secondary); }
  .ni-title { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
  .ni-content { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
}
</style>
