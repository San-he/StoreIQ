<script setup>
/**
 * 数据分析页 — 三Tab：销售概况 / 趋势分析 / 库存分布
 * 图表使用 Element Plus 色系，统一专业风格
 */
import * as echarts from 'echarts'
import {
  getOverviewApi, getSalesMonthlyApi, getSalesQuarterlyApi,
  getSalesByStoreApi, getSalesByCategoryApi, getProfitTrendApi, getInventoryDistributionApi
} from '@@/apis/storeiq/stats'
import { getSaleListApi } from '@@/apis/storeiq/sales'
import { Refresh, Download } from '@element-plus/icons-vue'

defineOptions({ name: 'AnalyticsView' })

// ===== Element Plus 配色 =====
const EP_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#337ecc', '#5daf34', '#cf9236', '#b37feb', '#36cfc9']

// ===== 数据 =====
const loading = ref(false)
const activeTab = ref('sales')
const selectedYear = ref(2026)
const overviewData = reactive({ storeCount: 0, productCount: 0, monthSales: 0, monthProfit: 0 })

// 销售明细表
const saleTableData = ref([])
const salePagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

// 图表实例
let monthlyChart, storeChart, categoryChart, profitChart, inventoryChart

// AI 状态
const aiHotText = ref('')
const aiProfitText = ref('')
const aiRestockText = ref('')
const aiHotLoading = ref(false)
const aiProfitLoading = ref(false)
const aiRestockLoading = ref(false)

// moved to after AI functions

onBeforeUnmount(() => {
  monthlyChart?.dispose(); storeChart?.dispose()
  categoryChart?.dispose(); profitChart?.dispose(); inventoryChart?.dispose()
})

// ===== 数据加载 =====
async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadSaleTable()])
    await renderActiveTab()
  } finally { loading.value = false }
}

async function loadOverview() {
  try { const { data } = await getOverviewApi(); Object.assign(overviewData, data) } catch { /* */ }
}

async function loadSaleTable() {
  try {
    const res = await getSaleListApi({ pageNum: salePagination.currentPage, pageSize: salePagination.pageSize, param: {} })
    saleTableData.value = res.data || []
    salePagination.total = res.total || 0
  } catch { saleTableData.value = [] }
}

// ===== Tab 切换渲染 =====
async function renderActiveTab() {
  if (activeTab.value === 'sales') { await renderSalesTab() }
  else if (activeTab.value === 'trend') { await renderTrendTab() }
  else if (activeTab.value === 'inventory') { await renderInventoryTab() }
}

async function handleTabChange() { loading.value = true; await renderActiveTab(); loading.value = false }

// ===== Tab 1: 销售概况 =====
async function renderSalesTab() {
  // 月度销售额趋势
  try {
    const { data } = await getSalesMonthlyApi(selectedYear.value)
    monthlyChart = initChart('monthly-chart', monthlyChart)
    monthlyChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '利润'], textStyle: { color: '#909399' } },
      grid: { left: 60, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.month), axisLabel: { color: '#909399' } },
      yAxis: { type: 'value', axisLabel: { color: '#909399', formatter: v => `¥${(v/10000).toFixed(0)}万` } },
      series: [
        { name: '销售额', type: 'line', data: data.map(d => d.amount), smooth: true, lineStyle: { color: EP_COLORS[0], width: 3 }, itemStyle: { color: EP_COLORS[0] }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'rgba(64,158,255,0.3)'},{offset:1,color:'rgba(64,158,255,0.02)'}]) } },
        { name: '利润', type: 'line', data: data.map(d => d.profit), smooth: true, lineStyle: { color: EP_COLORS[1], width: 2, type: 'dashed' }, itemStyle: { color: EP_COLORS[1] } }
      ]
    })
  } catch { /* */ }

  // 各店铺销售额对比
  try {
    const { data } = await getSalesByStoreApi()
    storeChart = initChart('store-chart', storeChart)
    storeChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 80, right: 30, top: 20, bottom: 80 },
      xAxis: { type: 'category', data: data.map(d => d.storeName), axisLabel: { rotate: 30, color: '#909399', fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { color: '#909399', formatter: v => `¥${(v/10000).toFixed(0)}万` } },
      series: [
        { name: '销售额', type: 'bar', data: data.map(d => d.amount), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:EP_COLORS[0]},{offset:1,color:'#6cb3f5'}]), borderRadius: [4,4,0,0] }, barMaxWidth: 40 },
        { name: '利润', type: 'bar', data: data.map(d => d.profit), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:EP_COLORS[1]},{offset:1,color:'#95d475'}]), borderRadius: [4,4,0,0] }, barMaxWidth: 40 }
      ]
    })
  } catch { /* */ }
}

// ===== Tab 2: 趋势分析 =====
async function renderTrendTab() {
  // 分类销量占比
  try {
    const { data } = await getSalesByCategoryApi()
    const filtered = data.filter(d => d.amount > 0)
    categoryChart = initChart('category-chart', categoryChart)
    categoryChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#909399' } },
      series: [{
        type: 'pie', radius: ['45%', '75%'], center: ['35%', '50%'],
        data: filtered.map((d, i) => ({ name: d.categoryName, value: d.amount })),
        color: EP_COLORS,
        label: { color: '#909399', formatter: '{b}\n{d}%' },
        emphasis: { label: { fontSize: 16, fontWeight: 'bold' } }
      }]
    })
  } catch { /* */ }

  // 利润趋势（折线+柱状混合）
  try {
    const { data } = await getProfitTrendApi(selectedYear.value)
    profitChart = initChart('profit-chart', profitChart)
    profitChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '利润', '利润率'], textStyle: { color: '#909399' } },
      grid: { left: 60, right: 60, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.month), axisLabel: { color: '#909399' } },
      yAxis: [
        { type: 'value', name: '金额', axisLabel: { color: '#909399', formatter: v => `¥${(v/10000).toFixed(0)}万` } },
        { type: 'value', name: '利润率%', axisLabel: { color: '#909399', formatter: '{value}%' } }
      ],
      series: [
        { name: '销售额', type: 'bar', data: data.map(d => d.amount), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:EP_COLORS[0]},{offset:1,color:'#6cb3f5'}]), borderRadius: [4,4,0,0] }, barMaxWidth: 30, barGap: '30%' },
        { name: '利润', type: 'bar', data: data.map(d => d.profit), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:EP_COLORS[1]},{offset:1,color:'#95d475'}]), borderRadius: [4,4,0,0] }, barMaxWidth: 30 },
        { name: '利润率', type: 'line', yAxisIndex: 1, data: data.map(d => d.profitRate), smooth: true, lineStyle: { color: EP_COLORS[3], width: 2.5 }, itemStyle: { color: EP_COLORS[3] }, symbol: 'circle', symbolSize: 6 }
      ]
    })
  } catch { /* */ }
}

// ===== Tab 3: 库存分布 =====
async function renderInventoryTab() {
  try {
    const { data } = await getInventoryDistributionApi()
    const categories = data.categories || []
    const chartData = data.data || []

    inventoryChart = initChart('inventory-chart', inventoryChart)
    inventoryChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: categories, textStyle: { color: '#909399' }, top: 0 },
      grid: { left: 80, right: 20, top: 40, bottom: 80 },
      xAxis: { type: 'category', data: chartData.map(d => d.storeName), axisLabel: { rotate: 30, color: '#909399', fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { color: '#909399' } },
      series: categories.map((cat, i) => ({
        name: cat, type: 'bar', stack: 'total', data: chartData.map(d => d[cat] || 0),
        itemStyle: { color: EP_COLORS[i % EP_COLORS.length] }, barMaxWidth: 50,
        emphasis: { focus: 'series' }
      }))
    })
  } catch { /* */ }
}

// ===== 工具 =====
function initChart(domId, instance) {
  instance?.dispose()
  const dom = document.getElementById(domId)
  if (!dom) return null
  const chart = echarts.init(dom)
  const observer = new ResizeObserver(() => chart.resize())
  observer.observe(dom)
  return chart
}

async function handleRefresh() { loading.value = true; await loadAll(); loading.value = false }

// 销售明细分页
function handleSalePageChange(page) { salePagination.currentPage = page; loadSaleTable() }
function handleSaleSizeChange(size) { salePagination.pageSize = size; salePagination.currentPage = 1; loadSaleTable() }

// watch tab
watch(activeTab, handleTabChange)
</script>

<template>
  <div class="app-container">
    <!-- 顶部概览卡片 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(64,158,255,.12)"><el-icon :size="24" color="#409eff"><Shop /></el-icon></div>
          <div class="stat-body"><span class="stat-num">{{ overviewData.storeCount }}</span><span class="stat-lbl">店铺总数</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(103,194,58,.12)"><el-icon :size="24" color="#67c23a"><Goods /></el-icon></div>
          <div class="stat-body"><span class="stat-num">{{ overviewData.productCount }}</span><span class="stat-lbl">商品种类</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(230,162,60,.12)"><el-icon :size="24" color="#e6a23c"><Coin /></el-icon></div>
          <div class="stat-body"><span class="stat-num">¥{{ (overviewData.monthSales / 10000).toFixed(1) }}万</span><span class="stat-lbl">本月销售额</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background:rgba(245,108,108,.12)"><el-icon :size="24" color="#f56c6c"><TrendCharts /></el-icon></div>
          <div class="stat-body"><span class="stat-num">¥{{ (overviewData.monthProfit / 10000).toFixed(1) }}万</span><span class="stat-lbl">本月利润</span></div>
        </div>
      </el-col>
    </el-row>

    <!-- Tab 导航 -->
    <el-card shadow="never">
      <div class="tab-header">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="销售概况" name="sales" />
          <el-tab-pane label="趋势分析" name="trend" />
          <el-tab-pane label="库存分布" name="inventory" />
        </el-tabs>
        <div class="tab-actions">
          <span style="margin-right:12px;color:#909399;font-size:13px">年份</span>
          <el-select v-model="selectedYear" size="small" style="width:100px" @change="handleRefresh">
            <el-option :value="2026" label="2026" />
            <el-option :value="2025" label="2025" />
          </el-select>
          <el-button :icon="Refresh" size="small" style="margin-left:8px" @click="handleRefresh">刷新</el-button>
          <el-button :icon="Download" size="small">导出</el-button>
        </div>
      </div>

      <!-- ====== Tab 1: 销售概况 ====== -->
      <div v-show="activeTab === 'sales'" v-loading="loading">
        <!-- 销售明细表 -->
        <el-table :data="saleTableData" stripe border size="small" max-height="300" style="margin-bottom:20px">
          <el-table-column prop="productName" label="商品" min-width="140" show-overflow-tooltip />
          <el-table-column prop="productCode" label="SKU" width="120" />
          <el-table-column prop="storeName" label="店铺" width="120" />
          <el-table-column prop="quantity" label="销量" width="70" align="center">
            <template #default="{ row }">{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="销售额" width="110" align="right">
            <template #default="{ row }">¥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="利润" width="100" align="right">
            <template #default="{ row }"><span :style="{ color: row.profit > 0 ? '#67c23a' : '#f56c6c' }">¥{{ row.profit }}</span></template>
          </el-table-column>
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ row.createTime?.substring(0, 10) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="salePagination.currentPage" v-model:page-size="salePagination.pageSize"
          :total="salePagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background small
          @current-change="handleSalePageChange" @size-change="handleSaleSizeChange"
          style="justify-content:flex-end;margin-bottom:20px"
        />

        <!-- 图表区 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover" header="月度销售额趋势">
              <div id="monthly-chart" style="height:320px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" header="各店铺销售额对比">
              <div id="store-chart" style="height:320px"></div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- ====== Tab 2: 趋势分析 ====== -->
      <div v-show="activeTab === 'trend'" v-loading="loading">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover" header="商品分类销量占比">
              <div id="category-chart" style="height:380px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" header="利润趋势分析">
              <div id="profit-chart" style="height:380px"></div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- ====== Tab 3: 库存分布 ====== -->
      <div v-show="activeTab === 'inventory'" v-loading="loading">
        <el-card shadow="hover" header="各店铺商品库存分布">
          <div id="inventory-chart" style="height:420px"></div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.overview-row { margin-bottom: 16px; }

.stat-card {
  display: flex; align-items: center; padding: 16px;
  background: var(--el-bg-color); border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  .stat-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-right: 14px; }
  .stat-body { .stat-num { display: block; font-size: 22px; font-weight: 700; color: var(--el-text-color-primary); } .stat-lbl { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; } }
}

.tab-header { display: flex; justify-content: space-between; align-items: center; }
.tab-actions { display: flex; align-items: center; }
</style>
