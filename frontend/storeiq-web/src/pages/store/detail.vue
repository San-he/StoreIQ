<script setup>
/**
 * 店铺详情页 — 基本信息 + 库存概览 + 成本分析（ROI）
 */
import { useRoute, useRouter } from 'vue-router'
import { getStoreDetailApi, getStoreInventoryApi } from '@@/apis/storeiq/stores'
import { ArrowLeft, Shop } from '@element-plus/icons-vue'

defineOptions({ name: 'StoreDetail' })

const route = useRoute()
const router = useRouter()
const storeId = computed(() => route.params.id)

const loading = ref(false)
const store = ref({})
const used = ref(0)
const usageRate = ref(0)
const inventoryList = ref([])

// 成本分析
const monthlyCost = computed(() => {
  if (store.value.propertyType === 'lease') return store.value.monthlyRent || 0
  // 自有店铺：按20年摊销
  return store.value.purchaseCost ? Math.round(store.value.purchaseCost / 20 / 12) : 0
})

const propertyLabel = computed(() => store.value.propertyType === 'lease' ? `月租金 ¥${store.value.monthlyRent}` : `购买本金 ¥${(store.value.purchaseCost / 10000).toFixed(1)}万`)
const costLabel = computed(() => store.value.propertyType === 'lease' ? '月租金' : '月摊销(20年)')

async function loadData() {
  loading.value = true
  try {
    const [detailRes, invRes] = await Promise.all([
      getStoreDetailApi(storeId.value),
      getStoreInventoryApi(storeId.value)
    ])
    store.value = detailRes.data?.store || detailRes.data || {}
    used.value = detailRes.data?.used || 0
    usageRate.value = detailRes.data?.usageRate || 0
    inventoryList.value = invRes.data || []
  } finally { loading.value = false }
}

onMounted(() => loadData())

function goBack() { router.push('/store/list') }
</script>

<template>
  <div class="app-container" v-loading="loading">
    <!-- 返回 -->
    <el-button :icon="ArrowLeft" @click="goBack" style="margin-bottom:16px">返回店铺列表</el-button>

    <!-- 基本信息卡片 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><el-icon :size="20"><Shop /></el-icon><span style="font-size:16px;font-weight:600;margin-left:8px">{{ store.name }}</span><el-tag :type="store.propertyType === 'owned' ? 'success' : 'warning'" size="small" effect="plain" style="margin-left:12px">{{ store.propertyType === 'owned' ? '自有产权' : '租赁' }}</el-tag></div>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="店铺编码">{{ store.code }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ store.region }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ store.address }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ store.contactPerson }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ store.contactPhone }}</el-descriptions-item>
            <el-descriptions-item :label="costLabel">¥{{ monthlyCost.toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="产权">{{ propertyLabel }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="store.status === 1 ? 'success' : 'info'" size="small">{{ store.status === 1 ? '运营中' : '已停用' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ store.createTime?.substring(0, 10) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ store.description || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 容量 + 成本分析 -->
      <el-col :span="8">
        <el-card shadow="never" header="容量使用" style="margin-bottom:16px">
          <div style="text-align:center;padding:16px 0">
            <el-progress type="dashboard" :percentage="usageRate" :status="usageRate > 85 ? 'exception' : usageRate > 60 ? 'warning' : 'success'" :stroke-width="16">
              <template #default><span style="font-size:28px;font-weight:700">{{ usageRate }}%</span></template>
            </el-progress>
            <div style="margin-top:12px;color:#909399;font-size:13px">已用 {{ used }} / 总容量 {{ store.capacity }} 件</div>
          </div>
        </el-card>
        <el-card shadow="never" header="成本分析">
          <div style="padding:12px 0">
            <div class="cost-row"><span>月度成本</span><span class="cost-val">¥{{ monthlyCost.toLocaleString() }}</span></div>
            <el-divider style="margin:10px 0" />
            <div class="cost-row"><span>{{ costLabel }}</span><span class="cost-val">¥{{ monthlyCost.toLocaleString() }}</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存明细 -->
    <el-card shadow="never" header="库存明细" style="margin-top:16px">
      <el-table :data="inventoryList" stripe border size="small" max-height="400">
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="quantity" label="库存数量" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.quantity <= 10 ? '#f56c6c' : '', fontWeight: row.quantity <= 10 ? '700' : '' }">{{ row.quantity }}</span>
            <el-tag v-if="row.quantity <= 10" type="danger" size="small" effect="dark" style="margin-left:4px">低</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.card-header { display: flex; align-items: center; }
.cost-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 14px; }
.cost-val { font-weight: 700; font-size: 18px; color: var(--el-color-primary); }
</style>
