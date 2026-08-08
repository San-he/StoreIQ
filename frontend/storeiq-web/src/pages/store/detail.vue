<script setup>
/**
 * 店铺详情页 — 基本信息 + 库存概览 + 成本分析（ROI）
 */
import { useRoute, useRouter } from 'vue-router'
import { getStoreDetailApi, getStoreInventoryApi, getStoreSalesTrendApi, getStoreInventoryPurchaseDatesApi } from '@@/apis/storeiq/stores'
import { ArrowLeft, Shop } from '@element-plus/icons-vue'
import { PROPERTY_TYPE, STORE_STATUS, getDictLabel, getDictTag } from '@@/constants/dict'
import * as echarts from 'echarts'

defineOptions({ name: 'StoreDetail' })

const route = useRoute()
const router = useRouter()
const storeId = computed(() => route.params.id)

const loading = ref(false)
const store = ref({})
const used = ref(0)
const usageRate = ref(0)
const inventoryList = ref([])
const purchaseDates = ref({})

// 销售趋势
const chartRef = ref(null)
let chartInstance = null
const currentYear = new Date().getFullYear()
const chartYear = ref(currentYear)
const salesTrendData = ref([])
const salesTrendYears = ref([])
const selectedProducts = ref([])

// 成本分析
const monthlyCost = computed(() => {
  if (store.value.propertyType === 'lease') return store.value.monthlyRent || 0
  return store.value.purchaseCost ? Math.round(store.value.purchaseCost / 20 / 12) : 0
})

function formatDate(str) {
  if (!str) return '-'
  return str.substring(0, 10)
}

async function loadData() {
  loading.value = true
  try {
    const [detailRes, invRes, purchaseRes, trendRes] = await Promise.all([
      getStoreDetailApi(storeId.value),
      getStoreInventoryApi(storeId.value),
      getStoreInventoryPurchaseDatesApi(storeId.value),
      getStoreSalesTrendApi(storeId.value, chartYear.value)
    ])
    store.value = detailRes.data?.store || detailRes.data || {}
    used.value = detailRes.data?.used || 0
    usageRate.value = detailRes.data?.usageRate || 0
    inventoryList.value = invRes.data || []
    purchaseDates.value = purchaseRes.data || {}
    salesTrendData.value = trendRes.data?.data || []
    salesTrendYears.value = trendRes.data?.years || []
    nextTick(() => renderChart())
  } finally { loading.value = false }
}

async function handleYearChange(year) {
  chartYear.value = year
  try {
    const res = await getStoreSalesTrendApi(storeId.value, year)
    salesTrendData.value = res.data?.data || []
    nextTick(() => renderChart())
  } catch { salesTrendData.value = [] }
}

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  const nowYear = new Date().getFullYear()
  const nowMonth = new Date().getMonth() + 1
  const maxMonth = chartYear.value < nowYear ? 12 : nowMonth
  const months = Array.from({ length: maxMonth }, (_, i) => `${i + 1}月`)

  // 1) 总销售趋势（按月汇总所有商品）
  const totalMap = {}
  salesTrendData.value.forEach(item => {
    totalMap[item.month] = (totalMap[item.month] || 0) + (item.quantity || 0)
  })
  const totalData = Array.from({ length: maxMonth }, (_, i) => totalMap[i + 1] || 0)

  const series = [{
    name: '总销售', type: 'line', smooth: true,
    data: totalData,
    lineStyle: { width: 3 },
    itemStyle: { color: '#409eff' }
  }]

  // 2) 勾选的商品 — 各一条独立线
  const selectedNames = new Set(selectedProducts.value.map(r => r.productName).filter(Boolean))
  if (selectedNames.size > 0) {
    const productMap = {}
    salesTrendData.value.forEach(item => {
      const name = item.product_name || '未知商品'
      if (!selectedNames.has(name)) return
      if (!productMap[name]) productMap[name] = {}
      productMap[name][item.month] = (productMap[name][item.month] || 0) + (item.quantity || 0)
    })
    const colors = ['#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9', '#ff85c0']
    let colorIdx = 0
    // 按 inventoryList 顺序保持颜色稳定
    selectedProducts.value.forEach(row => {
      const name = row.productName
      if (!name || !productMap[name]) return
      const monthData = productMap[name]
      series.push({
        name, type: 'line', smooth: true,
        data: Array.from({ length: maxMonth }, (_, i) => monthData[i + 1] || 0),
        lineStyle: { width: 2 },
        itemStyle: { color: colors[colorIdx % colors.length] }
      })
      colorIdx++
    })
  }

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, type: 'scroll' },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value', name: '销售数量(件)', minInterval: 1 },
    series
  })
}

onMounted(() => loadData())
onUnmounted(() => { chartInstance?.dispose() })

function handleSelectionChange(rows) {
  selectedProducts.value = rows
  nextTick(() => renderChart())
}

function goBack() { router.push('/store/list') }
</script>

<template>
  <div class="app-container" v-loading="loading">
    <!-- 返回 -->
    <el-button :icon="ArrowLeft" @click="goBack" style="margin-bottom:16px">返回店铺列表</el-button>

    <!-- 基本信息卡片 -->
    <el-row :gutter="16">
      <el-col :span="18">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><el-icon :size="20"><Shop /></el-icon><span style="font-size:16px;font-weight:600;margin-left:8px">{{ store.name }}</span><el-tag :type="getDictTag(PROPERTY_TYPE, store.propertyType)" size="small" effect="plain" style="margin-left:12px">{{ getDictLabel(PROPERTY_TYPE, store.propertyType) }}</el-tag></div>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="店铺编码">{{ store.code }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ store.region }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ store.address }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ store.contactPerson }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ store.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getDictTag(STORE_STATUS, store.status)" size="small">{{ getDictLabel(STORE_STATUS, store.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ store.createTime?.substring(0, 10) }}</el-descriptions-item>
            <el-descriptions-item label="产权类型">{{ getDictLabel(PROPERTY_TYPE, store.propertyType) }}</el-descriptions-item>
            <el-descriptions-item label="月度成本">¥{{ monthlyCost.toLocaleString() }}
              <span style="color:#909399;font-size:12px;margin-left:4px">
                ({{ store.propertyType === 'lease' ? '月租金' : '月摊销 / 20年' }})
              </span>
            </el-descriptions-item>
            <el-descriptions-item v-if="store.propertyType === 'lease'" label="月租金">¥{{ (store.monthlyRent || 0).toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item v-else label="购买本金">¥{{ ((store.purchaseCost || 0) / 10000).toFixed(1) }}万</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ store.description || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 容量仪表盘 -->
      <el-col :span="6">
        <el-card shadow="never" header="容量使用">
          <div style="text-align:center;padding:16px 0">
            <el-progress type="dashboard" :percentage="usageRate" :status="usageRate > 85 ? 'exception' : usageRate > 60 ? 'warning' : 'success'" :stroke-width="16">
              <template #default><span style="font-size:28px;font-weight:700">{{ usageRate }}%</span></template>
            </el-progress>
            <div style="margin-top:12px;color:#909399;font-size:13px">已用 {{ used }} / 总容量 {{ store.capacity }} 件</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售趋势 -->
    <el-card shadow="never" header="销售趋势" style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>销售趋势</span>
          <el-select v-model="chartYear" placeholder="年份" style="width:120px" @change="handleYearChange">
            <el-option v-for="y in salesTrendYears" :key="y" :label="y" :value="y" />
          </el-select>
        </div>
      </template>
      <div ref="chartRef" style="width:100%;height:420px"></div>
    </el-card>

        <!-- 库存明细 -->
    <el-card shadow="never" header="库存明细" style="margin-top:16px">
      <el-table :data="inventoryList" stripe size="small" max-height="400" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="productName" label="商品名称"  />
        <el-table-column prop="productCode" label="SKU"  />
                <el-table-column label="最早购入"  align="center">
          <template #default="{ row }">
            {{ formatDate(purchaseDates[row.productId]?.earliest) }}
          </template>
        </el-table-column>
        <el-table-column label="最晚购入"  align="center">
          <template #default="{ row }">
            {{ formatDate(purchaseDates[row.productId]?.latest) }}
          </template>
        </el-table-column>
                <el-table-column prop="quantity" label="库存数量"  align="center">
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
</style>
