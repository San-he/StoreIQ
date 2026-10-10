<script setup>
/**
 * 用户管理 — 仅总经理（superadmin）可访问
 * 支持创建/编辑/删除用户，修改角色和启停状态
 */
import { getUserListApi, getUserRolesApi, createUserApi, updateUserApi, deleteUserApi } from '@@/apis/users'
import { Search, Refresh, CirclePlus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'UserManagement' })

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const formRef = ref(null)
const roleOptions = ref([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ username: '', nickname: '', role: '', status: '' })

const defaultForm = () => ({ id: null, username: '', password: '', nickname: '', phone: '', email: '', role: '', status: 1 })
const formData = reactive(defaultForm())
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 角色映射
const roleLabelMap = { superadmin: '总经理', admin: '管理账号', manager: '普通账号' }
const roleTagTypeMap = { superadmin: 'danger', admin: 'warning', manager: 'info' }

async function loadData() {
  loading.value = true
  try {
    const res = await getUserListApi({
      pageNum: pagination.currentPage, pageSize: pagination.pageSize, param: { ...searchForm }
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

onMounted(async () => {
  try { const { data } = await getUserRolesApi(); roleOptions.value = data || [] } catch { /* */ }
  loadData()
})

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() { searchForm.username = ''; searchForm.nickname = ''; searchForm.role = ''; searchForm.status = ''; handleSearch() }

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(formData, defaultForm())
  // 编辑时密码非必填
  formRules.password[0].required = true
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  formRules.password[0].required = false
  // 从行数据填充（不含密码）
  Object.assign(formData, {
    id: row.id, username: row.username, password: '', nickname: row.nickname || '',
    phone: row.phone || '', email: row.email || '', role: row.role, status: row.status
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '删除确认', { type: 'warning' })
    await deleteUserApi(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* 取消 */ }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }
  try {
    const payload = { ...formData }
    delete payload.id
    if (isEdit.value) {
      await updateUserApi(formData.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createUserApi(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { /* */ }
}

function handlePageChange(page) { pagination.currentPage = page; loadData() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.currentPage = 1; loadData() }
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <!-- 搜索行 -->
      <el-form :inline="true" :model="searchForm" class="search-row">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="模糊搜索" clearable size="small" style="width:140px" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="searchForm.nickname" placeholder="模糊搜索" clearable size="small" style="width:140px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable size="small" style="width:120px">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable size="small" style="width:100px">
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" size="small" type="primary" @click="handleSearch">查询</el-button>
          <el-button size="small" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作行 -->
      <div class="toolbar">
        <el-button :icon="CirclePlus" size="small" type="primary" @click="handleCreate">新增用户</el-button>
        <el-button :icon="Refresh" size="small" @click="loadData">刷新</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" stripe size="small" v-loading="loading" style="width:100%">
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="nickname" label="昵称" min-width="100">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="150">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagTypeMap[row.role] || 'info'" size="small">{{ roleLabelMap[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ row.createTime?.substring(0, 19) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage" v-model:page-size="pagination.pageSize"
        :total="pagination.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        background small @current-change="handlePageChange" @size-change="handleSizeChange"
        style="justify-content:flex-end;margin-top:16px"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'" show-password maxlength="50" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.search-row { margin-bottom: 0; }
.toolbar { margin-bottom: 12px; }
</style>
