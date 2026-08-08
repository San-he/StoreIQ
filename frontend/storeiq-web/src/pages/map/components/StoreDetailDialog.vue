<script setup>
/**
 * 店铺详情弹窗 — 表单 + 商品列表 + 柱状图
 */
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { getStoreDetailApi, getStoreInventoryApi } from '@@/apis/storeiq/stores'
import { getProductSalesApi } from '@@/apis/storeiq/stats'
import { getCategoryListApi } from '@@/apis/storeiq/categories'
import { initChart } from '../utils/chart'
import { PROPERTY_TYPE, STORE_STATUS, SELLING_STATUS, getDictLabel, getDictTag } from '@@/constants/dict'
import dayjs from 'dayjs'

const props = defineProps({
  visible: { type: Boolean, default: false },
  store: { type: Object, default: null }
})
const emit = defineEmits(['close'])

// 数据
const storeDetail = ref(null)
const inventoryList = ref([])
const categoryList = ref([])
const productSalesData = ref([])
const loading = ref(false)

// 图表筛选
const chartDateRange = ref([
  dayjs().subtract(3, 'month').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])
const chartCategoryId = ref(null)

let barChart = null

// 格式化
function formatMoney(val) {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

// 加载详情
async function loadDetail() {
  if (!props.store?.id) return
  try {
    const { data } = await getStoreDetailApi(props.store.id)
    storeDetail.value = data
  } catch { /* */ }
}

// 加载库存商品
async function loadInventory() {
  if (!props.store?.id) return
  try {
    const { data } = await getStoreInventoryApi(props.store.id)
    inventoryList.value = data || []
  } catch { inventoryList.value = [] }
}

// 加载分类
async function loadCategories() {
  try {
    const { data } = await getCategoryListApi()
    categoryList.value = data || []
  } catch { categoryList.value = [] }
}

// 加载图表数据
async function loadChartData() {
  if (!props.store?.id) return
  try {
    const params = {
      storeId: props.store.id,
      startDate: chartDateRange.value[0],
      endDate: chartDateRange.value[1]
    }
    if (chartCategoryId.value) {
      params.categoryId = chartCategoryId.value
    }
    const { data } = await getProductSalesApi(params)
    productSalesData.value = data || []
    await nextTick()
    renderChart()
  } catch {
    productSalesData.value = []
    await nextTick()
    renderChart()
  }
}

// 渲染柱状图
function renderChart() {
  barChart = initChart('detail-bar-chart', barChart)
  if (!barChart) return

  // 空数据处理
  if (!productSalesData.value.length) {
    barChart.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#90a8c8', fontSize: 16, fontWeight: 'normal' }
      },
      grid: { left: 50, right: 20, top: 30, bottom: 60 },
      xAxis: { show: false },
      yAxis: { show: false },
      series: []
    }, true)
    return
  }

  const names = productSalesData.value.map(d => d.productName)
  const quantities = productSalesData.value.map(d => d.totalQuantity || 0)
  const amounts = productSalesData.value.map(d => Number(d.totalAmount || 0))

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params[0]?.dataIndex
        if (idx == null) return ''
        return `<b>${names[idx]}</b><br/>
          销量: ${quantities[idx]} 件<br/>
          销售额: ¥${amounts[idx].toLocaleString()}`
      }
    },
    grid: { left: 50, right: 20, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: '#90a8c8', rotate: names.length > 6 ? 30 : 0, fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(64,158,255,.2)' } }
    },
    yAxis: {
      type: 'value',
      name: '销量(件)',
      nameTextStyle: { color: '#90a8c8', fontSize: 11 },
      axisLabel: { color: '#90a8c8' },
      splitLine: { lineStyle: { color: 'rgba(64,158,255,.08)' } }
    },
    series: [{
      type: 'bar',
      data: quantities,
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#1a5ca8' }
          ]
        },
        borderRadius: [4, 4, 0, 0]
      },
      barMaxWidth: 40
    }]
  }, true)
}

function handleChartFilter() {
  loadChartData()
}

// 监听弹窗打开
watch(() => props.visible, async (val) => {
  if (val && props.store?.id) {
    loading.value = true
    await Promise.all([loadDetail(), loadInventory(), loadCategories()])
    await loadChartData()
    loading.value = false
  }
})

onBeforeUnmount(() => {
  barChart?.dispose()
  barChart = null
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`${store?.name || ''} — 店铺详情`"
    width="960px"
    top="4vh"
    destroy-on-close
    @close="emit('close')"
    class="detail-dialog"
  >
    <div v-loading="loading" class="dialog-body">
      <!-- 上半部分：左表单 + 右商品列表 -->
      <div class="top-section">
        <!-- 左侧表单 2/3 -->
        <div class="left-form">
          <el-descriptions :column="2" border size="small" title="基本信息">
            <el-descriptions-item label="店铺名称" :span="2">{{ storeDetail?.store?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="店铺编码">{{ storeDetail?.store?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属区域">{{ storeDetail?.store?.region || '-' }}</el-descriptions-item>
            <el-descriptions-item label="店铺地址" :span="2">{{ storeDetail?.store?.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ storeDetail?.store?.contactPerson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ storeDetail?.store?.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">
              <el-tag :type="getDictTag(PROPERTY_TYPE, storeDetail?.store?.propertyType)" size="small" effect="dark">
                {{ getDictLabel(PROPERTY_TYPE, storeDetail?.store?.propertyType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="运营状态">
              <el-tag :type="getDictTag(STORE_STATUS, storeDetail?.store?.status)" size="small" effect="dark">
                {{ getDictLabel(STORE_STATUS, storeDetail?.store?.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="容量(件)">{{ storeDetail?.store?.capacity || '-' }}</el-descriptions-item>
            <el-descriptions-item label="已用容量">{{ storeDetail?.used || 0 }} 件</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDate(storeDetail?.store?.createTime) }}</el-descriptions-item>
          </el-descriptions>

          <!-- 销售汇总 -->
          <el-descriptions :column="2" border size="small" title="销售汇总" style="margin-top:12px">
            <el-descriptions-item label="总销量">
              <span class="highlight-num">{{ storeDetail?.totalSales ?? '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="总收入">
              <span class="highlight-num">¥{{ formatMoney(storeDetail?.totalRevenue) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 右侧商品列表 1/3 -->
        <div class="right-products">
          <div class="section-title">售卖商品</div>
          <el-table :data="inventoryList" size="small" max-height="280px">
            <el-table-column prop="productName" label="商品名称" min-width="80" show-overflow-tooltip />
            <el-table-column label="成本单价" align="center" width="80">
              <template #default="{ row }">¥{{ row.costPrice ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="销售单价" align="center" width="80">
              <template #default="{ row }">¥{{ row.salePrice ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="在售" align="center" width="65">
              <template #default="{ row }">
                <el-tag :type="getDictTag(SELLING_STATUS, row.status)" size="small" effect="dark">
                  {{ getDictLabel(SELLING_STATUS, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 下半部分：柱状图 -->
      <div class="bottom-section">
        <div class="chart-header">
          <span class="section-title">商品销售统计</span>
          <div class="chart-filters">
            <el-date-picker
              v-model="chartDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              size="small"
              value-format="YYYY-MM-DD"
              style="width:220px"
            />
            <el-select v-model="chartCategoryId" placeholder="全部种类" size="small" clearable style="width:130px">
              <el-option
                v-for="cat in categoryList"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
            <el-button type="primary" size="small" @click="handleChartFilter">查询</el-button>
          </div>
        </div>
        <div id="detail-bar-chart" class="chart-container"></div>
      </div>
    </div>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dialog-body {
  min-height: 500px;
}

.top-section {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;

  .left-form {
    flex: 1;
    min-width: 0;
  }

  .right-products {
    flex: 1;
    min-width: 0;
    border: 1px solid rgba(64, 158, 255, .12);
    border-radius: 6px;
    padding: 10px;
    background: rgba(64, 158, 255, .03);
  }
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #c0d0e8;
  margin-bottom: 8px;
}

.highlight-num {
  color: #409eff;
  font-weight: 700;
  font-size: 15px;
}

.bottom-section {
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    .chart-filters {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  }
  .chart-container {
    width: 100%;
    height: 280px;
    border: 1px solid rgba(30, 133, 236, 0.35);
    border-radius: 6px;
    background: rgba(64, 158, 255, .02);
  }
}

:deep(.el-descriptions) {
  --el-descriptions-table-border: 1px solid rgba(64, 158, 255, .12);
  --el-descriptions-item-bordered-label-background: rgba(64, 158, 255, .06);
  --el-descriptions-text-color: #d0dae8;
  --el-descriptions-label-text-color: #90a8c8;
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
</style>

<style lang="scss">
.detail-dialog {
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
  .el-dialog__body {
    padding: 16px 20px;
  }
  .el-dialog__close { color: #90a8c8 }
}
</style>
