<script setup>
/**
 * 商品管理 — 表格 + 搜索 + 增删改 + 分类筛选 + 导入导出
 */
import { getProductListApi, getProductAllApi, createProductApi, updateProductApi, deleteProductApi } from '@@/apis/storeiq/products'
import { getCategoryListApi } from '@@/apis/storeiq/categories'
import { getPurchaseListApi } from '@@/apis/storeiq/purchases'
import { Search, Refresh, CirclePlus, Delete, Edit, Download, Upload, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { PRODUCT_STATUS, getDictLabel, getDictTag } from '@@/constants/dict'

defineOptions({ name: 'ProductManagement' })

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const isEdit = ref(false)
const formRef = ref(null)
const originalFormData = ref('')
const categoryOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ name: '', categoryId: '', costPriceMin: '', costPriceMax: '', salePriceMin: '', salePriceMax: '', status: '' })

const defaultForm = () => ({
  id: null, name: '', code: '', categoryId: null, brand: '', model: '',
  unit: '台', costPrice: null, salePrice: null, safetyStock: 10,
  image: '', description: '', status: 1
})
const formData = reactive(defaultForm())
const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProductListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { name: searchForm.name, categoryId: searchForm.categoryId, costPriceMin: searchForm.costPriceMin, costPriceMax: searchForm.costPriceMax, salePriceMin: searchForm.salePriceMin, salePriceMax: searchForm.salePriceMax, status: searchForm.status }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

async function loadCategories() {
  try { const { data } = await getCategoryListApi(); categoryOptions.value = data || [] } catch { categoryOptions.value = [] }
}

onMounted(() => { loadCategories(); loadData() })

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.name = ''; searchForm.categoryId = ''; searchForm.costPriceMin = ''; searchForm.costPriceMax = ''; searchForm.salePriceMin = ''; searchForm.salePriceMax = ''; searchForm.status = ''; handleSearch() }

function handleCreate() {
  isEdit.value = false; dialogTitle.value = '新增商品'
  Object.assign(formData, defaultForm())
  originalFormData.value = JSON.stringify(formData)
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true; dialogTitle.value = '编辑商品'
  Object.assign(formData, { ...row })
  originalFormData.value = JSON.stringify(formData)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteProductApi(row.id); ElMessage.success('删除成功'); loadData()
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isEdit.value) {
      await updateProductApi(formData.id, { ...formData })
    } else {
      await createProductApi({ ...formData })
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false; loadData()
  } finally { loading.value = false }
}

function handleDialogClose() { formRef.value?.resetFields() }

function hasFormChanged() {
  return JSON.stringify(formData) !== originalFormData.value
}

async function handleBeforeClose(done) {
  if (hasFormChanged()) {
    try {
      await ElMessageBox.confirm('表单已修改，确定放弃更改吗？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
    } catch { return }
  }
  done()
}

async function handleCancelClick() {
  if (hasFormChanged()) {
    try {
      await ElMessageBox.confirm('表单已修改，确定放弃更改吗？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
    } catch { return }
  }
  dialogVisible.value = false
}

// Excel 导出
function handleExport() {
  if (tableData.value.length === 0) { ElMessage.warning('没有可导出的数据'); return }
  const exportData = tableData.value.map(row => ({
    'SKU编码': row.code, '商品名称': row.name, '分类': row.categoryName || '',
    '品牌': row.brand || '', '型号': row.model || '', '成本价': row.costPrice,
    '售价': row.salePrice, '安全库存': row.safetyStock, '总库存': row.totalInventory || 0, '状态': getDictLabel(PRODUCT_STATUS, row.status)
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '商品列表')
  XLSX.writeFile(wb, `StoreIQ_商品列表_${new Date().toISOString().slice(0,10)}.xlsx`)
  ElMessage.success('导出成功')
}

// 批次详情弹窗
const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const batchProductName = ref('')
const batchProductId = ref(null)
const batchList = ref([])
const batchPagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

function handleBatchDetail(row) {
  batchProductName.value = row.name
  batchProductId.value = row.id
  batchPagination.currentPage = 1
  batchDialogVisible.value = true
  loadBatchData()
}

async function loadBatchData() {
  batchLoading.value = true
  try {
    const res = await getPurchaseListApi({
      pageNum: batchPagination.currentPage,
      pageSize: batchPagination.pageSize,
      param: { productId: String(batchProductId.value) }
    })
    batchList.value = res.data || []
    batchPagination.total = res.total || 0
  } catch { batchList.value = [] }
  finally { batchLoading.value = false }
}

function handleBatchPageChange(page) { batchPagination.currentPage = page; loadBatchData() }
function handleBatchSizeChange(size) { batchPagination.pageSize = size; batchPagination.currentPage = 1; loadBatchData() }

function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar-wrapper">
       <el-form :inline="true" :model="searchForm">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width: 100px;">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成本价">
          <el-input v-model="searchForm.costPriceMin" placeholder="最低" clearable style="width:90px" @keyup.enter="handleSearch" />
          <span style="margin:0 4px;color:#909399">—</span>
          <el-input v-model="searchForm.costPriceMax" placeholder="最高" clearable style="width:90px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="售价">
          <el-input v-model="searchForm.salePriceMin" placeholder="最低" clearable style="width:90px" @keyup.enter="handleSearch" />
          <span style="margin:0 4px;color:#909399">—</span>
          <el-input v-model="searchForm.salePriceMax" placeholder="最高" clearable style="width:90px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px;">
            <el-option v-for="(item, key) in PRODUCT_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      </div>
         <div style="margin-bottom: 10px; ">
           <el-button type="primary" :icon="CirclePlus" @click="handleCreate" v-permission="['product:edit']">新增商品</el-button>
          <el-button :icon="Upload" disabled>导入</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
         </div>
      <el-table v-loading="loading" :data="tableData" stripe >
        <el-table-column prop="name" label="商品名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="code" label="SKU编码" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类"  align="center" />
        <el-table-column prop="brand" label="品牌" align="center" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" align="center" show-overflow-tooltip />
        <el-table-column label="成本价"  align="center">
          <template #default="{ row }">¥{{ row.costPrice }}</template>
        </el-table-column>
        <el-table-column label="售价"  align="center">
          <template #default="{ row }">¥{{ row.salePrice }}</template>
        </el-table-column>
        <el-table-column prop="totalInventory" label="总库存" width="90" align="center" />
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictTag(PRODUCT_STATUS, row.status)" size="small" effect="plain">
              {{ getDictLabel(PRODUCT_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="info" link :icon="View" @click="handleBatchDetail(row)" title="批次"></el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)" title="编辑" v-permission="['product:edit']"></el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" title="删除" v-permission="['product:edit']"></el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" :before-close="handleBeforeClose" @closed="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU编码" prop="code">
              <el-input v-model="formData.code" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="formData.categoryId" placeholder="请选择" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="formData.brand" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="formData.model" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="成本价" prop="costPrice">
              <el-input-number v-model="formData.costPrice" :min="0" :precision="2" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建议售价" prop="salePrice">
              <el-input-number v-model="formData.salePrice" :min="0" :precision="2" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存">
              <el-input-number v-model="formData.safetyStock" :min="0" :step="5" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCancelClick">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批次详情弹窗 -->
    <el-dialog v-model="batchDialogVisible" :title="`「${batchProductName}」进货批次`" width="950px">
      <el-table :data="batchList" stripe size="small" v-loading="batchLoading">
        <el-table-column prop="batchNo" label="批次号" width="150" />
        <el-table-column prop="storeName" label="店铺" min-width="120" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column label="单位成本" width="110" align="center">
          <template #default="{ row }">¥{{ row.unitCost }}</template>
        </el-table-column>
        <el-table-column label="总成本" width="120" align="center">
          <template #default="{ row }">¥{{ row.totalCost }}</template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="100" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="90" align="center" />
        <el-table-column prop="createTime" label="进货时间" width="160" align="center">
          <template #default="{ row }">{{ row.createTime?.substring(0, 19) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination
          v-model:current-page="batchPagination.currentPage"
          v-model:page-size="batchPagination.pageSize"
          :total="batchPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          small
          @current-change="handleBatchPageChange"
          @size-change="handleBatchSizeChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 4px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
