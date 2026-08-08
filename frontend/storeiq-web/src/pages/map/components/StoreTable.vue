<script setup>
/**
 * 右侧店铺表格 — 名称、销量、容量、类型、操作
 * 分页由父组件控制（后端分页），本组件只负责展示和交互
 */
import { ref } from 'vue'
import { PROPERTY_TYPE, getDictLabel, getDictTag } from '@@/constants/dict'

const props = defineProps({
  storeList: { type: Array, default: () => [] },
  selectedStore: { type: Object, default: null },
  checkedIds: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  allStoreIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['select', 'detail', 'edit', 'update:checkedIds', 'pageChange', 'sizeChange'])

const tableRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

function handlePageChange(page) {
  currentPage.value = page
  emit('pageChange', { page, pageSize: pageSize.value })
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  emit('sizeChange', { page: 1, pageSize: size })
}

// 表头全选/反选后跳过紧随的 selectionChange（避免用当前页覆盖全选结果）
let skipNextChange = false

function handleSelectionChange(selection) {
  if (skipNextChange) { skipNextChange = false; return }
  emit('update:checkedIds', selection.map(r => r.id))
}

function handleSelectAll(selection) {
  skipNextChange = true
  if (selection.length === 0) {
    emit('update:checkedIds', [])
  } else {
    // 全选使用全量 ID（allStoreIds），而非当前页
    const allIds = (props.allStoreIds && props.allStoreIds.length > 0)
      ? props.allStoreIds
      : props.storeList.map(r => r.id)
    emit('update:checkedIds', allIds)
  }
}

function formatAmount(val) {
  if (val == null) return '-'
  const n = Number(val)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}
</script>

<template>
  <div class="table-area">
    <el-table
      ref="tableRef"
      :data="storeList"
      size="small"
      stripe
      row-key="id"
      v-loading="loading"
      @row-click="(row) => emit('select', row)"
      @selection-change="handleSelectionChange"
      @select-all="handleSelectAll"
      max-height="480"
      :row-class-name="({ row }) => selectedStore?.id === row.id ? 'selected-row' : ''"
    >
      <el-table-column type="selection" width="32" reserve-selection />
      <el-table-column prop="name" label="店铺名称" min-width="90" show-overflow-tooltip />
      <el-table-column label="销量(总计)" align="center" >
        <template #default="{ row }">
          <span class="sales-num">{{ formatAmount(row.totalSales) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center">
        <template #default="{ row }">
          <el-tag :type="getDictTag(PROPERTY_TYPE, row.propertyType)" size="small" effect="dark">
            {{ getDictLabel(PROPERTY_TYPE, row.propertyType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="容量" align="center"   width="55">
        <template #default="{ row }">
          <el-progress
            :percentage="row.usageRate"
            :stroke-width="6"
            :show-text="false"
            :status="row.usageRate > 85 ? 'exception' : row.usageRate > 60 ? 'warning' : 'success'"
          />
          <span class="cap-text">{{ row.used }}/{{ row.capacity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="88" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click.stop="emit('detail', row)">详情</el-button>
          <el-button type="warning" link size="small" @click.stop="emit('edit', row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager-wrapper">
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
.table-area {
  flex: 1;
  min-height: 0;
  padding: 0 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  :deep(.el-table) {
    background: transparent !important;
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: rgba(64, 158, 255, .06);
    --el-table-row-hover-bg-color: rgba(76, 163, 251, 0.12);
    --el-table-border-color: rgba(64, 158, 255, .08);
    --el-table-text-color: #d0dae8;
    --el-table-header-text-color: #90a8c8;
    font-size: 12px;
    th { font-weight: 500 }
    .selected-row { --el-table-tr-bg-color: rgba(64, 158, 255, .18) !important }
  }
  :deep(.el-table__inner-wrapper::before) { display: none }
  .sales-num { color: #409eff; font-weight: 600 }
  .cap-text { display: block; font-size: 10px; color: rgba(224, 230, 240, .45); margin-top: 2px }
  .pager-wrapper {
    display: flex;
    justify-content: center;
    padding: 6px 0;
    margin-top: auto;
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
