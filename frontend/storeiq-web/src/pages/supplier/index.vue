<script setup>
/**
 * 供应商管理 — 简单 CRUD
 */
import { getSupplierListApi, createSupplierApi, updateSupplierApi, deleteSupplierApi } from '@@/apis/storeiq/suppliers'
import { Search, Refresh, CirclePlus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'SupplierManagement' })

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增供应商')
const isEdit = ref(false)
const formRef = ref(null)

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ name: '' })

const defaultForm = () => ({ id: null, name: '', contactPerson: '', contactPhone: '', address: '', description: '' })
const formData = reactive(defaultForm())
const formRules = { name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res = await getSupplierListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize,
      param: { name: searchForm.name }
    })
    tableData.value = res.data
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

onMounted(() => loadData())

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.name = ''; handleSearch() }

function handleCreate() {
  isEdit.value = false; dialogTitle.value = '新增供应商'
  Object.assign(formData, defaultForm()); dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true; dialogTitle.value = '编辑供应商'
  Object.assign(formData, { ...row }); dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除供应商「${row.name}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteSupplierApi(row.id); ElMessage.success('删除成功'); loadData()
  }).catch(() => {})
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isEdit.value) { await updateSupplierApi(formData.id, { ...formData }) }
    else { await createSupplierApi({ ...formData }) }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false; loadData()
  } finally { loading.value = false }
}

function handleDialogClose() { formRef.value?.resetFields() }
function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar-wrapper">
         <el-form :inline="true" :model="searchForm">
        <el-form-item label="供应商名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-form-item>
        <el-button type="primary" :icon="CirclePlus" @click="handleCreate" v-permission="['supplier:edit']">新增供应商</el-button>
        <el-tooltip content="刷新"><el-button :icon="Refresh" circle @click="loadData" /></el-tooltip>
      </el-form-item>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="name" label="供应商名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" min-width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)" v-permission="['supplier:edit']">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" v-permission="['supplier:edit']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-wrapper">
        <el-pagination v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @closed="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="formData.contactPerson" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="formData.contactPhone" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="formData.address" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.search-wrapper { margin-bottom: 16px; :deep(.el-card__body) { padding-bottom: 2px; } }
.toolbar-wrapper { display: flex; justify-content: space-between; margin-bottom: 16px; align-items: center; }
.pager-wrapper { display: flex; justify-content: flex-end; margin-top: 16px;margin-bottom: 50px; }
</style>
