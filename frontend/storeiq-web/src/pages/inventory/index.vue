<script setup>
/**
 * 库存管理 — 只读总览表格 + 筛选 + 预警高亮
 */
import { getInventoryListApi, getInventoryAlertsApi } from '@@/apis/storeiq/inventory'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getCategoryListApi } from '@@/apis/storeiq/categories'
import { Search, Refresh, WarningFilled, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'

defineOptions({ name: 'InventoryManagement' })

const loading = ref(false)
const tableData = ref([])
const alertData = ref([])
const storeOptions = ref([])
const categoryOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ storeId: '', categoryId: '', productName: '', quantityMin: '', quantityMax: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getInventoryListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { storeId: searchForm.storeId, categoryId: searchForm.categoryId, productName: searchForm.productName, quantityMin: searchForm.quantityMin, quantityMax: searchForm.quantityMax }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

async function loadAlerts() {
  try { const { data } = await getInventoryAlertsApi(); alertData.value = data || [] } catch { alertData.value = [] }
}

onMounted(async () => {
  try {
    const [stores, cats] = await Promise.all([getStoreMapApi(), getCategoryListApi()])
    storeOptions.value = stores.data || []
    categoryOptions.value = cats.data || []
  } catch { /* ignore */ }
  loadData(); loadAlerts()
})

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.storeId = ''; searchForm.categoryId = ''; searchForm.productName = ''; searchForm.quantityMin = ''; searchForm.quantityMax = ''; handleSearch() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }

function quantityClass(row) {
  const threshold = row.safetyStock || 10
  if (row.quantity <= 0) return 'qty-danger'
  if (row.quantity <= threshold) return 'qty-warning'
  return 'qty-normal'
}

function handleExport() {
  if (tableData.value.length === 0) { ElMessage.warning('没有可导出的数据'); return }
  const exportData = tableData.value.map(row => ({
    '店铺': row.storeName, '商品名称': row.productName, 'SKU': row.productCode,
    '品牌': row.brand, '库存数量': row.quantity, '安全库存': row.safetyStock,
    '售价': row.salePrice, '更新时间': row.updateTime?.substring(0, 19)
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '库存总览')
  XLSX.writeFile(wb, `StoreIQ_库存总览_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}
</script>

<template>
  <div class="app-container">
    <!-- 预警横幅 -->
    <el-alert v-if="alertData.length > 0" :title="`库存预警：${alertData.length} 项商品库存不足`" type="warning"
      :closable="false" show-icon style="margin-bottom:16px">
      <template #default>
        <span v-for="(a, i) in alertData.slice(0,5)" :key="i" style="margin-right:16px;font-size:13px">
          {{ a.storeName }} — {{ a.productName }}（{{ a.quantity }}/{{ a.safetyStock }}）
        </span>
        <span v-if="alertData.length > 5" style="color:#909399">...等{{ alertData.length }}项</span>
      </template>
    </el-alert>



    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="店铺">
          <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:180px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width:160px">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入" clearable style="width:140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="库存数量">
          <el-input v-model="searchForm.quantityMin" placeholder="最低" clearable style="width:90px" @keyup.enter="handleSearch" />
          <span style="margin:0 4px;color:#909399">—</span>
          <el-input v-model="searchForm.quantityMax" placeholder="最高" clearable style="width:90px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar-wrapper">
        <span style="color:#909399;font-size:13px">库存总览（只读）</span>
        <div>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
          <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="storeName" label="店铺" min-width="100" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品" min-width="130" show-overflow-tooltip />
        <el-table-column prop="productCode" label="SKU"  show-overflow-tooltip />
        <el-table-column prop="brand" label="品牌"  show-overflow-tooltip />
        <el-table-column label="库存数量" align="center">
          <template #default="{ row }">
            <span :class="quantityClass(row)">{{ row.quantity }}</span>
            <el-tag v-if="row.quantity <= 0" type="danger" size="small" effect="dark" style="margin-left:4px">缺货</el-tag>
            <el-tag v-else-if="row.quantity <= (row.safetyStock || 10)" type="warning" size="small" effect="dark" style="margin-left:4px">预警</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存"  />
        <el-table-column label="售价" >
          <template #default="{ row }">¥{{ row.salePrice }}</template>
        </el-table-column>
        <el-table-column label="更新时间">
          <template #default="{ row }">{{ row.updateTime?.substring(0,16) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.app-container { 
  padding: 16px; 
}
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.qty-normal { color: #67c23a; font-weight: 600; }
.qty-warning { color: #e6a23c; font-weight: 700; }
.qty-danger { color: #f56c6c; font-weight: 700; }
</style>
