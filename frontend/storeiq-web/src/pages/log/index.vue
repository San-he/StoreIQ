<script setup>
/**
 * 操作日志 — 只读表格 + 筛选
 */
import { getLogListApi, getLogModulesApi } from '@@/apis/storeiq/logs'
import { formatDateTime } from '@@/utils/datetime'
import { Search, Refresh } from '@element-plus/icons-vue'

defineOptions({ name: 'LogManagement' })

const loading = ref(false)
const tableData = ref([])
const moduleOptions = ref([])
const actionOptions = ['CREATE', 'UPDATE', 'DELETE', 'IMPORT', 'EXPORT']

const pagination = reactive({ currentPage: 1, pageSize: 15, total: 0 })
const searchForm = reactive({ module: '', action: '' })

// 后端存英文码，展示层翻译为中文；入库/出库按"模块+操作"组合显示
const MODULE_LABELS = {
  store: '店铺', product: '商品', category: '分类', inventory: '库存',
  purchase: '进货', sale: '销售', supplier: '供应商', user: '用户',
  cleanup: '数据清理', unknown: '其他'
}
const ACTION_LABELS = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', IMPORT: '导入', EXPORT: '导出', UNKNOWN: '其他' }

function moduleLabel(module) { return MODULE_LABELS[module] || module }
function actionLabel(row) {
  if (row.module === 'purchase' && row.action === 'CREATE') return '入库'
  if (row.module === 'sale' && row.action === 'CREATE') return '出库'
  return ACTION_LABELS[row.action] || row.action
}

async function loadData() {
  loading.value = true
  try {
    const res = await getLogListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { module: searchForm.module, action: searchForm.action }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

onMounted(async () => {
  try { const { data } = await getLogModulesApi(); moduleOptions.value = data || [] } catch { /* */ }
  loadData()
})

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.module = ''; searchForm.action = ''; handleSearch() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="操作模块">
          <el-select v-model="searchForm.module" placeholder="全部" clearable style="width:140px">
            <el-option v-for="m in moduleOptions" :key="m" :label="moduleLabel(m)" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.action" placeholder="全部" clearable style="width:120px">
            <el-option v-for="a in actionOptions" :key="a" :label="ACTION_LABELS[a] || a" :value="a" />
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
        <span style="color:#909399;font-size:13px">所有创建/修改/删除操作自动记录</span>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe size="small">
        <el-table-column prop="operatorName" label="操作人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ moduleLabel(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.action === 'DELETE' ? 'danger' : row.action === 'CREATE' ? 'success' : 'warning'" size="small" effect="plain">
              {{ actionLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作时间" min-width="170">
          <template #default="{ row }">{{ row.createTime ? formatDateTime(row.createTime, 'YYYY-MM-DD HH:mm:ss') : '' }}</template>
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
</style>
