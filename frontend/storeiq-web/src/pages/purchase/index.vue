<script setup>
/**
 * 进货管理 — 记录表格 + 新增入库弹窗（联动库存）
 */
import { getPurchaseListApi, createPurchaseApi } from '@@/apis/storeiq/purchases'
import { getProductAllApi } from '@@/apis/storeiq/products'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getSupplierAllApi } from '@@/apis/storeiq/suppliers'
import { Search, Refresh, CirclePlus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'PurchaseManagement' })

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const productOptions = ref([])
const storeOptions = ref([])
const supplierOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ productId: '', storeId: '' })

const defaultForm = () => ({ productId: null, storeId: null, supplierId: null, quantity: 1, unitCost: null, remark: '' })
const formData = reactive(defaultForm())
const formRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择店铺', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitCost: [{ required: true, message: '请输入进货单价', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPurchaseListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { productId: searchForm.productId, storeId: searchForm.storeId }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

async function loadOptions() {
  try {
    const [prods, stores, supps] = await Promise.all([getProductAllApi(), getStoreMapApi(), getSupplierAllApi()])
    productOptions.value = prods.data || []
    storeOptions.value = stores.data || []
    supplierOptions.value = supps.data || []
  } catch { /* ignore */ }
}

onMounted(() => { loadOptions(); loadData() })

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.productId = ''; searchForm.storeId = ''; handleSearch() }

function handleCreate() {
  Object.assign(formData, defaultForm()); dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await createPurchaseApi({ ...formData })
    ElMessage.success('入库成功，库存已自动更新')
    dialogVisible.value = false; loadData()
  } finally { loading.value = false }
}

function handleDialogClose() { formRef.value?.resetFields() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }

// 计算总成本
const totalCost = computed(() => {
  if (formData.quantity && formData.unitCost) {
    return (formData.quantity * formData.unitCost).toFixed(2)
  }
  return '0.00'
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="商品">
          <el-select v-model="searchForm.productId" placeholder="全部" clearable style="width:180px">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:180px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
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
        <el-button type="primary" :icon="CirclePlus" @click="handleCreate">新增入库</el-button>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column prop="batchNo" label="批次号" width="160" />
        <el-table-column prop="productName" label="商品" min-width="160" show-overflow-tooltip />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="storeName" label="入库店铺" width="130" />
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">
            <span style="color:#67c23a;font-weight:600">+{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">¥{{ row.unitCost }}</template>
        </el-table-column>
        <el-table-column label="总成本" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalCost }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" width="120" show-overflow-tooltip />
        <el-table-column label="入库时间" width="160">
          <template #default="{ row }">{{ row.createTime?.substring(0,16) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 新增入库弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增入库" width="520px" @closed="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="选择商品" prop="productId">
          <el-select v-model="formData.productId" placeholder="请选择" filterable style="width:100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.name}（${p.code}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库店铺" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="请选择" style="width:100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="formData.supplierId" placeholder="请选择（选填）" clearable style="width:100%">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="formData.quantity" :min="1" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进货单价" prop="unitCost">
              <el-input-number v-model="formData.unitCost" :min="0" :precision="2" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="总成本">
          <span style="font-size:18px;font-weight:700;color:#409eff">¥{{ totalCost }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
