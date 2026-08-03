<script setup>
/**
 * 销售管理 — 记录表格 + 出库弹窗 + 扫码录入 + 利润计算
 */
import { getSaleListApi, createSaleApi, scanProductApi } from '@@/apis/storeiq/sales'
import { getProductAllApi } from '@@/apis/storeiq/products'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { Search, Refresh, CirclePlus, Camera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'SaleManagement' })

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const scanCode = ref('')
const productOptions = ref([])
const storeOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ productId: '', storeId: '' })

const defaultForm = () => ({ productId: null, storeId: null, quantity: 1, unitPrice: null, customerName: '', remark: '' })
const formData = reactive(defaultForm())
const formRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择店铺', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSaleListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { productId: searchForm.productId, storeId: searchForm.storeId }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

onMounted(async () => {
  try {
    const [prods, stores] = await Promise.all([getProductAllApi(), getStoreMapApi()])
    productOptions.value = prods.data || []
    storeOptions.value = stores.data || []
  } catch { /* ignore */ }
  loadData()
})

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.productId = ''; searchForm.storeId = ''; handleSearch() }

function handleCreate() {
  Object.assign(formData, defaultForm()); scanCode.value = ''; dialogVisible.value = true
}

// 扫码录入
async function handleScan() {
  if (!scanCode.value.trim()) return
  try {
    const { data } = await scanProductApi(scanCode.value.trim())
    formData.productId = data.id
    formData.unitPrice = data.salePrice
    ElMessage.success(`已识别: ${data.name}`)
  } catch {
    ElMessage.error('未找到该商品，请检查条码')
  }
}

// 选择商品时自动填入参考售价
function onProductChange(id) {
  const p = productOptions.value.find(x => x.id === id)
  if (p && !formData.unitPrice) formData.unitPrice = p.salePrice
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await createSaleApi({ ...formData })
    ElMessage.success('出库成功，库存已自动扣减')
    dialogVisible.value = false; loadData()
  } catch (e) {
    // 库存不足等错误由后端返回
  } finally { loading.value = false }
}

function handleDialogClose() { formRef.value?.resetFields() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }

// 预估利润
const estimatedProfit = computed(() => {
  if (!formData.quantity || !formData.unitPrice) return null
  const product = productOptions.value.find(x => x.id === formData.productId)
  if (!product?.costPrice) return null
  return ((formData.unitPrice - product.costPrice) * formData.quantity).toFixed(2)
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
        <el-button type="primary" :icon="CirclePlus" @click="handleCreate">新增出库</el-button>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column prop="productName" label="商品" min-width="150" show-overflow-tooltip />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="storeName" label="出库店铺" width="130" />
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:600">-{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column label="销售额" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="利润" width="110" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.profit > 0 ? '#67c23a' : '#f56c6c', fontWeight:'600' }">
              ¥{{ row.profit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="90" />
        <el-table-column label="销售时间" width="160">
          <template #default="{ row }">{{ row.createTime?.substring(0,16) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 新增出库弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增出库" width="550px" @closed="handleDialogClose">
      <!-- 扫码录入区 -->
      <div class="scan-area">
        <el-input v-model="scanCode" placeholder="扫描/输入商品条码..." :prefix-icon="Camera"
          @keyup.enter="handleScan" clearable style="flex:1">
          <template #append>
            <el-button @click="handleScan">识别</el-button>
          </template>
        </el-input>
      </div>
      <el-divider style="margin:12px 0">或手动选择</el-divider>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="选择商品" prop="productId">
          <el-select v-model="formData.productId" placeholder="请选择" filterable style="width:100%"
            @change="onProductChange">
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.name}（¥${p.salePrice}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库店铺" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="请选择" style="width:100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="formData.quantity" :min="1" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价" prop="unitPrice">
              <el-input-number v-model="formData.unitPrice" :min="0" :precision="2" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="estimatedProfit !== null" label="预估利润">
          <span :style="{ color: estimatedProfit > 0 ? '#67c23a' : '#f56c6c', fontSize:'16px', fontWeight:'700' }">
            ¥{{ estimatedProfit }}
          </span>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户名称">
              <el-input v-model="formData.customerName" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.scan-area { display: flex; gap: 8px; }
</style>
