<script setup>
import { Bell } from "@element-plus/icons-vue"
import { getNotificationListApi, getUnreadCountApi, markReadApi, markAllReadApi } from '@@/apis/storeiq/notifications'
import { formatDateTime } from '@@/utils/datetime'
import CleanupDetailDialog from './CleanupDetailDialog.vue'

const unreadCount = ref(0)
const notifyList = ref([])
const popoverWidth = 380
const activeTab = ref('unread')
const detailVisible = ref(false)
const detailNotification = ref(null)
let pollTimer = null

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

function handleDetail(item) {
  detailNotification.value = item
  detailVisible.value = true
}

function getCleanupSummary(content) {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    if (!data?.tables) return content
    const labels = data.tables.map(t => `${t.tableLabel || t.tableType}(${t.totalRecords}条)`)
    return `待清理 ${data.totalRecords} 条：${labels.join('、')}`
  } catch { return content }
}

// 类型配置
const typeConfig = {
  stock_alert: 'warning', restock_suggest: 'info', monthly_report: 'success',
  cost_reminder: 'danger', data_cleanup: 'warning'
}
function getTypeTag(type) { return typeConfig[type] || 'info' }
function getTypeLabel(type) {
  const map = {
    stock_alert: '库存预警', restock_suggest: '补货建议', monthly_report: '月报',
    cost_reminder: '成本提醒', data_cleanup: '数据清理'
  }
  return map[type] || type
}

// computed lists
const unreadList = computed(() => notifyList.value.filter(n => !n.isRead))
const readList = computed(() => notifyList.value.filter(n => n.isRead))

onMounted(() => {
  loadNotifications()
  pollTimer = setInterval(loadNotifications, 5 * 60 * 1000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
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
        <el-tabs v-model="activeTab" size="small">
          <el-tab-pane label="未读" name="unread">
            <template #label>
              <span>未读 <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="tab-badge" /></span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="已读" name="read" />
        </el-tabs>
        <el-scrollbar max-height="360px">
          <div v-if="notifyList.length === 0" class="notify-empty">暂无通知</div>
          <template v-for="item in (activeTab === 'unread' ? unreadList : readList)" :key="item.id">
            <!-- data_cleanup 类型：特殊渲染 -->
            <div v-if="item.type === 'data_cleanup'" class="notify-item cleanup-item">
              <div class="ni-header">
                <el-tag :type="getTypeTag(item.type)" size="small" effect="plain">{{ getTypeLabel(item.type) }}</el-tag>
                <span class="ni-time">{{ item.createTime ? formatDateTime(item.createTime, 'YYYY-MM-DD HH:mm') : '' }}</span>
              </div>
              <div class="ni-title">{{ item.title }}</div>
              <div class="ni-content">{{ getCleanupSummary(item.content) }}</div>
              <div class="ni-actions">
                <el-button size="small" type="primary" link @click="handleDetail(item)">详情</el-button>
                <el-button v-if="!item.isRead" size="small" link @click="handleMarkRead(item)">已读</el-button>
              </div>
            </div>
            <!-- 普通类型 -->
            <div v-else
              class="notify-item" :class="{ unread: !item.isRead }"
              @click="handleMarkRead(item)"
            >
              <div class="ni-header">
                <el-tag :type="getTypeTag(item.type)" size="small" effect="plain">{{ getTypeLabel(item.type) }}</el-tag>
                <span class="ni-time">{{ item.createTime ? formatDateTime(item.createTime, 'YYYY-MM-DD HH:mm') : '' }}</span>
              </div>
              <div class="ni-title">{{ item.title }}</div>
              <div class="ni-content">{{ item.content }}</div>
            </div>
          </template>
        </el-scrollbar>
      </template>
    </el-popover>

    <CleanupDetailDialog v-model="detailVisible" :notification="detailNotification" />
  </div>
</template>

<style lang="scss" scoped>
.notify-top-bar { display: flex; justify-content: space-between; align-items: center; padding: 8px 4px; }
.notify-title { font-weight: 600; font-size: 14px; }
.notify-empty { text-align: center; padding: 40px; color: var(--el-text-color-secondary); }
.tab-badge { margin-left: 4px; :deep(.el-badge__content) { position: static; } }

.notify-item {
  padding: 12px 8px; border-radius: 6px; margin-bottom: 4px; cursor: pointer; transition: background 0.2s;
  &:hover { background: var(--el-fill-color-lighter); }
  &.unread { background: var(--el-color-primary-light-9); }
  .ni-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
  .ni-time { font-size: 11px; color: var(--el-text-color-secondary); }
  .ni-title { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
  .ni-content { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  &.cleanup-item { cursor: default; }
  .ni-actions { margin-top: 8px; display: flex; gap: 8px; }
}

:deep(.el-tabs__header) { margin-bottom: 4px; }
:deep(.el-tabs__nav-wrap::after) { height: 1px; }
</style>
