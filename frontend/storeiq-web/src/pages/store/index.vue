<script setup>
/**
 * 店铺管理 — 表格 + 搜索 + 增删改
 */
import { getStoreListApi, createStoreApi, updateStoreApi, deleteStoreApi, getStoreRegionsApi } from '@@/apis/storeiq/stores'
import { useRouter } from 'vue-router'
import { Search, Refresh, CirclePlus, Delete, Edit, View, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { PROPERTY_TYPE, STORE_STATUS, getDictLabel, getDictTag } from '@@/constants/dict'

defineOptions({ name: 'StoreManagement' })
const router = useRouter()
function handleDetail(row) { router.push(`/store/detail/${row.id}`) }

// ===== 数据 =====
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增店铺')
const isEdit = ref(false)
const formRef = ref(null)
const regionOptions = ref([])
const originalFormData = ref('')

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 搜索
const searchForm = reactive({ name: '', region: '', propertyType: '', status: '' })

// 表单
const defaultForm = () => ({
  id: null, name: '', code: '', address: '', longitude: null, latitude: null,
  region: '', capacity: 0, propertyType: 'lease',
  monthlyRent: null, purchaseCost: null,
  contactPerson: '', contactPhone: '', description: '', status: 1
})
const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入店铺编码', trigger: 'blur' }],
  region: [{ required: true, message: '请选择区域', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }],
  propertyType: [{ required: true, message: '请选择产权类型', trigger: 'change' }]
}

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const res = await getStoreListApi({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      param: { name: searchForm.name, region: searchForm.region, propertyType: searchForm.propertyType, status: searchForm.status }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

async function loadRegions() {
  try {
    const { data } = await getStoreRegionsApi()
    regionOptions.value = data || []
  } catch { regionOptions.value = [] }
}

onMounted(() => { loadRegions(); loadData() })

// ===== 搜索 =====
function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.name = ''; searchForm.region = ''; searchForm.propertyType = ''; searchForm.status = ''; handleSearch() }

// ===== 新增/编辑 =====
function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增店铺'
  Object.assign(formData, defaultForm())
  originalFormData.value = JSON.stringify(formData)
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑店铺'
  Object.assign(formData, { ...row })
  originalFormData.value = JSON.stringify(formData)
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除店铺「${row.name}」吗？`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await deleteStoreApi(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isEdit.value) {
      await updateStoreApi(formData.id, { ...formData })
    } else {
      await createStoreApi({ ...formData })
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { loading.value = false }
}

function hasFormChanged() {
  return JSON.stringify(formData) !== originalFormData.value
}

async function handleBeforeClose(done) {
  if (hasFormChanged()) {
    try {
      await ElMessageBox.confirm('表单已修改，确定放弃更改吗？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
    } catch {
      return
    }
  }
  done()
}

async function handleCancelClick() {
  if (hasFormChanged()) {
    try {
      await ElMessageBox.confirm('表单已修改，确定放弃更改吗？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
    } catch {
      return
    }
  }
  dialogVisible.value = false
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

// ===== 导出 =====
function handleExport() {
  if (tableData.value.length === 0) { ElMessage.warning('没有可导出的数据'); return }
  const exportData = tableData.value.map(row => ({
    '店铺名称': row.name, '编码': row.code, '区域': row.region,
    '产权': getDictLabel(PROPERTY_TYPE, row.propertyType),
    '容量': row.capacity, '已用': row.used, '使用率': row.usageRate + '%',
    '联系人': row.contactPerson, '电话': row.contactPhone,
    '状态': getDictLabel(STORE_STATUS, row.status)
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '店铺列表')
  XLSX.writeFile(wb, `StoreIQ_店铺列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}

// ===== 分页 =====
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
</script>

<template>
  <div class="app-container">
    <!-- 表格 -->
    <el-card shadow="never">
      <div class="toolbar-wrapper">
     <el-form :inline="true" :model="searchForm">
        <el-form-item label="店铺名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="searchForm.region" placeholder="全部" clearable style="width:160px;">
            <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="产权">
          <el-select v-model="searchForm.propertyType" placeholder="全部" clearable style="width:160px;">
            <el-option v-for="(item, key) in PROPERTY_TYPE" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:160px;">
            <el-option v-for="(item, key) in STORE_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item>
        <el-button type="primary" :icon="CirclePlus" @click="handleCreate" v-permission="['store:edit']">新增店铺</el-button>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-tooltip content="刷新">
          <el-button :icon="Refresh" circle @click="loadData" />
        </el-tooltip>
        </el-form-item>
      </el-form>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="name" label="店铺名称" align="center" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" align="center" />
        <el-table-column prop="region" label="区域" align="center" show-overflow-tooltip />
        <el-table-column label="产权"  align="center">
          <template #default="{ row }">
            <el-tag :type="getDictTag(PROPERTY_TYPE, row.propertyType)" size="small" effect="plain">
              {{ getDictLabel(PROPERTY_TYPE, row.propertyType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="容量使用" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.usageRate" :stroke-width="8" :show-text="false"
              :status="row.usageRate > 85 ? 'exception' : row.usageRate > 60 ? 'warning' : 'success'" />
            <span style="font-size:12px;color:#909399">{{ row.used }}/{{ row.capacity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" align="center" show-overflow-tooltip/>
        <el-table-column prop="contactPhone" label="电话" align="center" />
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictTag(STORE_STATUS, row.status)" size="small" effect="plain" >
              {{ getDictLabel(STORE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="info" link :icon="View" @click="handleDetail(row)" title="详情"></el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)" title="编辑" v-permission="['store:edit']"></el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" title="删除" v-permission="['store:edit']"></el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="40%"
    append-to-body="body"
    :before-close="handleBeforeClose"
    @closed="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="店铺名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="店铺编码" prop="code">
              <el-input v-model="formData.code" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="区域" prop="region">
              <el-select v-model="formData.region" placeholder="请选择" style="width:100%">
                <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量(件)" prop="capacity">
              <el-input-number v-model="formData.capacity" :min="0" :step="10" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="formData.longitude" :precision="7" :step="0.01" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="formData.latitude" :precision="7" :step="0.01" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产权类型" prop="propertyType">
          <el-radio-group v-model="formData.propertyType">
            <el-radio value="lease">租赁</el-radio>
            <el-radio value="owned">购买</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.propertyType === 'lease'" label="月租金">
          <el-input-number v-model="formData.monthlyRent" :min="0" :precision="2" :step="100" style="width:200px" />
          <span style="margin-left:8px;color:#909399">元/月</span>
        </el-form-item>
        <el-form-item v-else label="购买本金">
          <el-input-number v-model="formData.purchaseCost" :min="0" :precision="2" :step="10000" style="width:250px" />
          <span style="margin-left:8px;color:#909399">元</span>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="formData.contactPerson" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCancelClick">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { 
  padding: 16px; 
}
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
