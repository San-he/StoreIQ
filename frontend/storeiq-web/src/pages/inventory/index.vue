<script setup>
/**
 * 库存管理 — 只读总览表格 + 筛选 + 预警高亮
 */
import { getInventoryListApi, getInventoryAlertsApi } from '@@/apis/storeiq/inventory'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getCategoryListApi } from '@@/apis/storeiq/categories'
import { Search, Refresh, WarningFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'InventoryManagement' })

const loading = ref(false)
const tableData = ref([])
const alertData = ref([])
const storeOptions = ref([])
const categoryOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ storeId: '', categoryId: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getInventoryListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { storeId: searchForm.storeId, categoryId: searchForm.categoryId }
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
function handleReset() { searchForm.storeId = ''; searchForm.categoryId = ''; handleSearch() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
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

    <el-card shadow="never" class="search-wrapper">
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
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar-wrapper">
        <span style="color:#909399;font-size:13px">库存总览（只读）</span>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe border
        :row-class-name="({ row }) => row.quantity <= (row.safetyStock || 10) ? 'warning-row' : ''">
        <el-table-column prop="storeName" label="店铺" width="130" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品" min-width="160" show-overflow-tooltip />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="brand" label="品牌" width="80" />
        <el-table-column label="库存数量" width="130" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.quantity <= (row.safetyStock || 10) ? '#f56c6c' : '', fontWeight: row.quantity <= (row.safetyStock || 10) ? '700' : '' }">
              {{ row.quantity }}
            </span>
            <el-tag v-if="row.quantity <= (row.safetyStock || 10)" type="danger" size="small" effect="dark" style="margin-left:6px">
              {{ row.quantity <= 0 ? '缺货' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="80" align="center" />
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">¥{{ row.salePrice }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
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
.app-container { padding: 16px; }
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
:deep(.warning-row) { --el-table-tr-bg-color: var(--el-color-danger-light-9); }
</style>
