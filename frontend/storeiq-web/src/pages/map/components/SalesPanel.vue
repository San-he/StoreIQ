<script setup>
/**
 * 左侧销售数据面板
 * 默认显示全部商品销售情况，选中店铺后显示该店铺商品
 * 分页由后端完成
 */
import { ref, watch, computed } from 'vue'
import { getProductSalesApi } from '@@/apis/storeiq/stats'

const props = defineProps({
  selectedStore: {
    type: Object,
    default: null
  }
})
const emit = defineEmits(['show-all'])

const loading = ref(false)
const salesData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const panelTitle = computed(() => {
  return props.selectedStore
    ? `${props.selectedStore.name} 商品销售情况`
    : '全部商品销售情况'
})

function formatAmount(val) {
  if (val == null) return '-'
  const n = Number(val)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}

function trendIcon(trend) {
  if (trend === 'up') return '↑'
  if (trend === 'down') return '↓'
  return '→'
}

function trendColor(trend) {
  if (trend === 'up') return '#67c23a'
  if (trend === 'down') return '#f56c6c'
  return '#909399'
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (props.selectedStore?.id) {
      params.storeId = props.selectedStore.id
    }
    const res = await getProductSalesApi(params)
    // 后端返回 { code: 200, data: [...records], total: N }
    salesData.value = res?.data || []
    total.value = res?.total || 0
  } catch {
    salesData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  loadData()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

watch(() => props.selectedStore, () => {
  currentPage.value = 1
  loadData()
}, { immediate: true, deep: true })
</script>

<template>
  <div class="sales-panel">
    <div class="panel-header">
      <span class="panel-title">{{ panelTitle }}</span>
      <el-button v-if="selectedStore" size="small" text @click="emit('show-all')">全部</el-button>
    </div>
    <div class="panel-body">
      <el-table
        :data="salesData"
        size="small"
        v-loading="loading"
        height="100%"
        :header-cell-style="{ background: 'rgba(64,158,255,.06)', color: '#90a8c8' }"
      >
        <el-table-column prop="productName" label="商品名称" min-width="80" show-overflow-tooltip />
        <el-table-column label="总销量" align="center" width="65">
          <template #default="{ row }">{{ row.totalQuantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="销售额" align="center" width="75">
          <template #default="{ row }">
            <span class="amount-cell">{{ formatAmount(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="趋势" align="center" width="50">
          <template #default="{ row }">
            <span :style="{ color: trendColor(row.trend), fontWeight: 700 }">
              {{ trendIcon(row.trend) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="panel-footer">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="total"
        :pager-count="3"
        layout="total, sizes, prev, pager, next"
        small
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.sales-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 12px;
    border-bottom: 1px solid rgba(64, 158, 255, .12);
    .panel-title {
      font-size: 13px;
      font-weight: 600;
      color: #c0d0e8;
    }
  }
  .panel-body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding: 0 6px;
    :deep(.el-table) {
      background: transparent !important;
      --el-table-bg-color: transparent;
      --el-table-tr-bg-color: rgba(13, 43, 74, 0.72);
      --el-table-row-hover-bg-color: rgba(76, 163, 251, 0.12);
      --el-table-border-radius: 4px;
      --el-table-border-color: rgba(64, 158, 255, .08);
      --el-table-text-color: #d0dae8;
      font-size: 11px;
      .amount-cell { color: #409eff; font-weight: 600 }
    }
    :deep(.el-table__inner-wrapper::before) { display: none }
    :deep(.el-loading-mask) { background: rgba(14, 38, 74, .6) }
  }
  .panel-footer {
    display: flex;
    justify-content: center;
    padding: 6px 0;
    border-top: 1px solid rgba(64, 158, 255, .08);
    :deep(.el-pagination) {
      --el-pagination-bg-color: rgba(64, 158, 255, .08);
      --el-pagination-text-color: #90a8c8;
      --el-pagination-button-color: #90a8c8;
      --el-pagination-hover-color: #409eff;
      .btn-prev, .btn-next, .el-pager li {
        background: rgba(64, 158, 255, .06) !important;
        color: #90a8c8 !important;
        border: 1px solid rgba(64, 158, 255, .12) !important;
        min-width: 24px !important;
        height: 24px !important;
        line-height: 24px !important;
        font-size: 11px !important;
      }
      .el-pager li.is-active {
        background: #409eff !important;
        color: #fff !important;
      }
    }
  }
}
</style>
