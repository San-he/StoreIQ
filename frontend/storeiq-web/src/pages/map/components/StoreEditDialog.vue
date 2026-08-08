<script setup>
/**
 * 店铺编辑弹窗 — 编辑店铺信息 + 管理商品
 */
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { updateStoreApi, getStoreInventoryApi, addStoreInventoryApi } from '@@/apis/storeiq/stores'
import { updateInventoryStatusApi } from '@@/apis/storeiq/inventory'
import { getProductAllApi } from '@@/apis/storeiq/products'
import { SELLING_STATUS, getDictLabel, getDictTag } from '@@/constants/dict'

const props = defineProps({
  visible: { type: Boolean, default: false },
  store: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const formRef = ref(null)
const loading = ref(false)
const inventoryList = ref([])
const allProducts = ref([])

// 当前添加选择的商品
const selectedProductId = ref(null)
const addQuantity = ref(10)

const formData = ref({
  name: '',
  contactPerson: '',
  propertyType: 'lease',
  status: 1,
  monthlyRent: null,
  purchaseCost: null
})

// 表单是否已修改（未保存提醒用）
const originalData = ref(null)
const isDirty = computed(() => {
  if (!originalData.value) return false
  const cur = formData.value
  const orig = originalData.value
  return cur.name !== orig.name
    || cur.contactPerson !== orig.contactPerson
    || cur.propertyType !== orig.propertyType
    || cur.status !== orig.status
    || cur.monthlyRent !== orig.monthlyRent
    || cur.purchaseCost !== orig.purchaseCost
})

const priceLabel = computed(() => {
  return formData.value.propertyType === 'owned' ? '购买本金' : '月租金'
})

const priceField = computed(() => {
  return formData.value.propertyType === 'owned' ? 'purchaseCost' : 'monthlyRent'
})

// 可添加的商品（排除已在店铺中的）
const availableProducts = computed(() => {
  const existingIds = inventoryList.value.map(i => i.productId)
  return (allProducts.value || []).filter(p => !existingIds.includes(p.id))
})

function resetForm() {
  const data = {
    name: props.store?.name || '',
    contactPerson: props.store?.contactPerson || '',
    propertyType: props.store?.propertyType || 'lease',
    status: props.store?.status ?? 1,
    monthlyRent: props.store?.monthlyRent ?? null,
    purchaseCost: props.store?.purchaseCost ?? null
  }
  formData.value = { ...data }
  originalData.value = { ...data }
}

// 关闭前确认（表单未保存提醒）
async function handleBeforeClose() {
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('表单已修改，确定放弃更改吗？', '未保存', {
        confirmButtonText: '确定放弃',
        cancelButtonText: '继续编辑',
        type: 'warning'
      })
    } catch { return }
  }
  originalData.value = null
  emit('close')
}

async function loadInventory() {
  if (!props.store?.id) return
  try {
    const { data } = await getStoreInventoryApi(props.store.id)
    inventoryList.value = data || []
  } catch { inventoryList.value = [] }
}

async function loadAllProducts() {
  try {
    const { data } = await getProductAllApi()
    allProducts.value = data || []
  } catch { allProducts.value = [] }
}

// 提交编辑
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload = { ...formData.value }
    // 清除不相关的价格字段
    if (payload.propertyType === 'owned') {
      payload.monthlyRent = null
    } else {
      payload.purchaseCost = null
    }

    await updateStoreApi(props.store.id, payload)
    ElMessage.success('店铺信息更新成功')
    originalData.value = null
    emit('saved')
    emit('close')
  } catch {
    ElMessage.error('更新失败')
  } finally {
    loading.value = false
  }
}

// 添加商品到店铺
async function handleAddProduct() {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择商品')
    return
  }
  if (addQuantity.value < 1) {
    ElMessage.warning('数量必须大于0')
    return
  }
  if (addQuantity.value > 99999) {
    ElMessage.warning('数量不能超过99999')
    return
  }
  try {
    await addStoreInventoryApi(props.store.id, {
      productId: selectedProductId.value,
      quantity: addQuantity.value
    })
    ElMessage.success('商品添加成功')
    selectedProductId.value = null
    addQuantity.value = 10
    await loadInventory()
  } catch {
    ElMessage.error('添加失败')
  }
}

// 恢复商品售卖
async function handleRestoreProduct(row) {
  try {
    await updateInventoryStatusApi(row.id, { status: 1 })
    ElMessage.success('已恢复售卖')
    await loadInventory()
  } catch {
    ElMessage.error('恢复失败')
  }
}

// 移除商品（软删除）
async function handleRemoveProduct(row) {
  try {
    await ElMessageBox.confirm(
      `确定要暂停「${row.productName}」在该店铺的售卖吗？`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateInventoryStatusApi(row.id, { status: 0 })
    ElMessage.success('已暂停售卖')
    await loadInventory()
  } catch { /* 取消 */ }
}

watch(() => props.visible, async (val) => {
  if (val && props.store?.id) {
    resetForm()
    await Promise.all([loadInventory(), loadAllProducts()])
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`编辑 — ${store?.name || ''}`"
    width="700px"
    top="6vh"
    destroy-on-close
    :before-close="handleBeforeClose"
    class="edit-dialog"
  >
    <div class="edit-body">
      <!-- 店铺信息表单 -->
      <el-form ref="formRef" :model="formData" label-width="90px" size="default">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="店铺名称" prop="name" :rules="[{ required: true, message: '请输入名称' }]">
              <el-input v-model="formData.name" placeholder="店铺名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="contactPerson">
              <el-input v-model="formData.contactPerson" placeholder="负责人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="propertyType">
              <el-radio-group v-model="formData.propertyType">
                <el-radio value="lease">租赁</el-radio>
                <el-radio value="owned">购买</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="priceLabel" prop="price">
              <el-input-number
                v-model="formData[priceField]"
                :min="0"
                :precision="2"
                :step="1000"
                controls-position="right"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运营状态" prop="status">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                active-text="运营中"
                inactive-text="已暂停"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 商品管理 -->
      <div class="product-section">
        <div class="product-header">
          <span class="section-title">售卖商品管理</span>
          <div class="add-product-row">
            <el-select
              v-model="selectedProductId"
              placeholder="选择商品"
              size="small"
              filterable
              style="width:180px"
            >
              <el-option
                v-for="p in availableProducts"
                :key="p.id"
                :label="p.name"
                :value="p.id"
              />
            </el-select>
            <el-input-number v-model="addQuantity" :min="1" :max="99999" size="small" style="width:90px" controls-position="right" />
            <el-button type="primary" size="small" @click="handleAddProduct" :disabled="!selectedProductId">
              添加
            </el-button>
          </div>
        </div>
        <el-table :data="inventoryList" size="small" max-height="240px">
          <el-table-column prop="productName" label="商品名称" min-width="100" show-overflow-tooltip />
          <el-table-column label="成本单价" align="center" width="90">
            <template #default="{ row }">¥{{ row.costPrice ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="销售单价" align="center" width="90">
            <template #default="{ row }">¥{{ row.salePrice ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="库存" align="center" width="60">
            <template #default="{ row }">{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="70">
            <template #default="{ row }">
              <el-tag :type="getDictTag(SELLING_STATUS, row.status)" size="small" effect="dark">
                {{ getDictLabel(SELLING_STATUS, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="70">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                type="danger"
                link
                size="small"
                @click="handleRemoveProduct(row)"
              >
                移除
              </el-button>
              <el-button
                v-else
                type="success"
                link
                size="small"
                @click="handleRestoreProduct(row)"
              >
                恢复
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleBeforeClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.edit-body {
  .product-section {
    margin-top: 16px;
    border: 1px solid rgba(64, 158, 255, .12);
    border-radius: 6px;
    padding: 12px;
    background: rgba(64, 158, 255, .03);
    .product-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      .add-product-row {
        display: flex;
        gap: 6px;
        align-items: center;
      }
    }
  }
  .section-title {
    font-size: 13px;
    font-weight: 600;
    color: #c0d0e8;
  }
  .paused-text { color: #909399; font-size: 11px }
}

:deep(.el-table) {
  background: transparent !important;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(64, 158, 255, .06);
  --el-table-row-hover-bg-color: rgba(76, 163, 251, 0.12);
  --el-table-border-color: rgba(64, 158, 255, .08);
  --el-table-text-color: #d0dae8;
  --el-table-header-text-color: #90a8c8;
  font-size: 11px;
}

:deep(.el-form-item__label) { color: #90a8c8 }
:deep(.el-input__wrapper) { background: rgba(64, 158, 255, .06); box-shadow: 0 0 0 1px rgba(64, 158, 255, .15) inset }
:deep(.el-input__inner) { color: #d0dae8 }
:deep(.el-radio__label) { color: #d0dae8 }
:deep(.el-input-number) { background: rgba(64, 158, 255, .06) }
:deep(.el-input-number__decrease), :deep(.el-input-number__increase) { background: rgba(64, 158, 255, .08); color: #90a8c8 }
</style>

<style lang="scss">
.edit-dialog {
  .el-dialog {
    background: rgba(14, 38, 74, 0.95) !important;
    backdrop-filter: blur(20px);
    border: 1px solid rgba(64, 158, 255, .2);
    border-radius: 12px;
  }
  .el-dialog__header {
    border-bottom: 1px solid rgba(64, 158, 255, .12);
    padding: 14px 20px;
  }
  .el-dialog__title {
    color: #c0d0e8;
    font-size: 15px;
    font-weight: 600;
  }
  .el-dialog__body { padding: 16px 20px }
  .el-dialog__footer {
    border-top: 1px solid rgba(64, 158, 255, .12);
    padding: 12px 20px;
  }
  .el-dialog__close { color: #90a8c8 }
}
</style>
