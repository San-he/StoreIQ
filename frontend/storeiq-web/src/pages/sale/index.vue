<script setup>
/**
 * 销售管理 — 记录表格 + 出库弹窗 + 扫码录入 + 利润计算
 */
import { getSaleListApi, createSaleApi, createSaleBatchApi, scanProductApi } from '@@/apis/storeiq/sales'
import { formatDateTime } from '@@/utils/datetime'
import { getProductAllApi } from '@@/apis/storeiq/products'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { Search, Refresh, CirclePlus, Camera, Upload, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'

defineOptions({ name: 'SaleManagement' })

const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const importFileRef = ref(null)
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

// ====== 导入/导出 ======

/** 导出选中的数据（未选中则导出全部） */
function handleExport() {
  const rows = selectedRows.value.length ? selectedRows.value : tableData.value
  if (!rows.length) { ElMessage.warning('没有可导出的数据'); return }
  const data = rows.map((row) => ({
    '商品': row.productName || '',
    'SKU': row.productCode || '',
    '出库店铺': row.storeName || '',
    '数量': row.quantity,
    '售价': row.unitPrice,
    '销售额': row.totalAmount,
    '利润': row.profit,
    '客户': row.customerName || '',
    '备注': row.remark || '',
    '销售时间': row.createTime ? formatDateTime(row.createTime, 'YYYY-MM-DD HH:mm') : ''
  }))
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '销售记录')
  XLSX.writeFile(wb, `销售记录_${new Date().toISOString().substring(0, 10)}.xlsx`)
  ElMessage.success(`已导出 ${data.length} 条记录`)
}

/** 导入 Excel */
function handleImport(input) {
  const file = input?.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const wb = XLSX.read(e.target.result, { type: 'array' })
      const ws = wb.Sheets[wb.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(ws, { header: 1 })
      if (rows.length < 2) { ElMessage.warning('文件中没有数据'); return }
      // 第一行为表头，从第二行开始解析
      const items = rows.slice(1).filter((row) => row[0] && row[1] && row[2] && row[3]).map((row) => ({
        productCode: String(row[0] || '').trim(),
        storeName: String(row[1] || '').trim(),
        quantity: Number(row[2]) || 0,
        unitPrice: Number(row[3]) || 0,
        customerName: String(row[4] || '').trim(),
        remark: String(row[5] || '').trim()
      }))
      if (!items.length) { ElMessage.warning('没有解析到有效数据行'); return }
      doBatchImport(items)
    } catch { ElMessage.error('文件解析失败') }
  }
  reader.readAsArrayBuffer(file)
  // 重置 input 以便重复选择同一文件
  input.value = ''
}

async function doBatchImport(items) {
  loading.value = true
  try {
    const res = await createSaleBatchApi(items)
    const { success, fail, errors } = res.data || {}
    let msg = `导入完成: 成功 ${success ?? 0} 条，失败 ${fail ?? 0} 条`
    if (errors && errors.length) {
      msg += '\n' + errors.slice(0, 5).join('\n')
      if (errors.length > 5) msg += `\n...等 ${errors.length} 条错误`
    }
    ElMessageBox.alert(msg, '导入结果', { confirmButtonText: '确定', type: fail > 0 ? 'warning' : 'success' })
    loadData()
  } catch { ElMessage.error('导入失败') }
  finally { loading.value = false }
}

/** 下载导入模板 */
function handleDownloadTemplate() {
  const headers = ['商品SKU（必填）', '店铺名称（必填）', '数量（必填）', '售价（必填）', '客户名称', '备注']
  const example = ['PROD001', '总店', 2, 99.00, '张三', '']
  const ws = XLSX.utils.aoa_to_sheet([headers, example])
  ws['!cols'] = headers.map(() => ({ wch: 18 }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '销售导入模板')
  XLSX.writeFile(wb, '销售导入模板.xlsx')
}

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


    <el-card shadow="never">
      <div class="toolbar-wrapper">
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
        <el-form-item>
          <el-button type="primary" :icon="CirclePlus" @click="handleCreate" v-permission="['sale:edit']">新增出库</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
          <el-button :icon="Upload" @click="importFileRef?.click()" v-permission="['sale:edit']">导入</el-button>
          <el-button @click="handleDownloadTemplate">下载模板</el-button>
          <input ref="importFileRef" type="file" accept=".xlsx,.xls" hidden @change="handleImport($event.target)" />
        </el-form-item>
      </el-form>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe row-key="id" @selection-change="(rows) => selectedRows = rows">
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column prop="productName" label="商品" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="productCode" label="SKU" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="storeName" label="出库店铺" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column label="数量" min-width="80" align="center">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:600">-{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售价" min-width="100" align="center">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column label="销售额" min-width="110" align="center">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="利润" min-width="110" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.profit > 0 ? '#67c23a' : '#f56c6c', fontWeight:'600' }">
              ¥{{ row.profit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column label="销售时间" min-width="160" align="center">
          <template #default="{ row }">{{ row.createTime ? formatDateTime(row.createTime, 'YYYY-MM-DD HH:mm') : '' }}</template>
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
.toolbar-left { display: flex; gap: 8px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.scan-area { display: flex; gap: 8px; }
</style>
