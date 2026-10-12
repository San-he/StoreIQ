<script setup>
/**
 * StoreIQ 首页大屏 — 三栏布局 + Cesium 2D 中国地图 + ECharts 图表
 * 暗蓝色科技风
 */
defineOptions({ name: 'MapView' })
import { ref, computed, onMounted, onBeforeUnmount, onActivated, onDeactivated, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/pinia/stores/user'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getOverviewApi, getSalesMonthlyApi, getSalesByStoreApi, getSalesByCategoryApi, getProductSalesApi, getInventoryAlertsApi } from '@@/apis/storeiq/stats'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// ===== 数据 =====
const storeList = ref([])
const cesiumReady = ref(false)
const overview = ref({})
const monthlySales = ref([])
const storeSales = ref([])
const categorySales = ref([])
const productSales = ref([])
const inventoryAlerts = ref([])
const alertScrollRef = ref(null)
let alertScrollTimer = null

let viewer = null
let trendChart = null, pieChart = null, barChart = null
let firstLoad = true

// ===== 顶部时钟/天气 =====
const now = ref(new Date())
const weatherText = ref('加载中...')
const weatherTemp = ref('--')
let clockTimer = null

const dateStr = computed(() => {
  const d = now.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})
const timeStr = computed(() => now.value.toLocaleTimeString('zh-CN', { hour12: false }))
const weekStr = computed(() => ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.value.getDay()])

function startClock() { clockTimer = setInterval(() => { now.value = new Date() }, 1000) }

async function fetchWeather() {
  try {
    let lat = 39.9, lon = 116.4
    if (navigator.geolocation) {
      try {
        const pos = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 3000 })
        })
        lat = pos.coords.latitude; lon = pos.coords.longitude
      } catch { /* */ }
    }
    const res = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat.toFixed(1)}&longitude=${lon.toFixed(1)}&current_weather=true`)
    const json = await res.json()
    const w = json.current_weather
    const map = { 0: '晴', 1: '晴', 2: '多云', 3: '阴', 45: '雾', 51: '小雨', 53: '小雨', 55: '中雨', 61: '阵雨', 63: '中雨', 65: '大雨', 71: '小雪', 73: '中雪', 75: '大雪', 80: '阵雨', 95: '雷暴' }
    weatherText.value = map[w.weathercode] || '晴'
    weatherTemp.value = `${w.temperature}°C`
  } catch { weatherText.value = '晴'; weatherTemp.value = '22°C' }
}

// ===== 退出登录 =====
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    userStore.logout()
  } catch { /* */ }
}

// ===== 计算 =====
const totalStores = computed(() => overview.value?.storeCount || storeList.value.length || 0)
const totalProducts = computed(() => overview.value?.productCount || 0)
const monthAmount = computed(() => formatMoney(overview.value?.monthSales || 0))
const monthProfit = computed(() => formatMoney(overview.value?.monthProfit || 0))
function formatMoney(val) {
  const n = Number(val)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}

// ===== Cesium 2D 初始化 =====
function initScene() {
  viewer = new Cesium.Viewer('cesiumContainer', {
    animation: false, fullscreenButton: false, vrButton: false, shouldAnimate: true,
    sceneMode: Cesium.SceneMode.SCENE2D,
    selectionIndicator: false, baseLayerPicker: false, timeline: false, sceneModePicker: false,
    homeButton: false, geocoder: false, infoBox: false, navigationHelpButton: false,
    baseLayer: false, imageryProvider: false,
    terrainProvider: new Cesium.EllipsoidTerrainProvider()
  })
  viewer.cesiumWidget.creditContainer.style.display = 'none'
  const osmLayer = viewer.imageryLayers.addImageryProvider(
    new Cesium.UrlTemplateImageryProvider({ url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', credit: new Cesium.Credit(''), maximumLevel: 18 })
  )
  osmLayer.alpha = 0.35
  viewer.scene.globe.baseColor = Cesium.Color.fromCssColorString('#0b1d3a')
  viewer.scene.backgroundColor = Cesium.Color.fromCssColorString('#061429')
  trySwapToDarkImagery()
  viewer.camera.setView({
    destination: Cesium.Rectangle.fromDegrees(70, 15, 135, 55),
    orientation: { heading: 0, roll: 0 }
  })
  viewer.scene.screenSpaceCameraController.minimumZoomDistance = 500000
  viewer.scene.screenSpaceCameraController.maximumZoomDistance = 20000000
  cesiumReady.value = true
}

async function trySwapToDarkImagery() {
  if (!viewer) return
  const urls = [
    { url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', subdomains: ['a', 'b', 'c', 'd'], credit: 'CartoDB' },
    { url: 'https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', subdomains: [], credit: 'Esri' }
  ]
  for (const cfg of urls) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 3000)
      const testUrl = cfg.subdomains.length ? cfg.url.replace('{s}', cfg.subdomains[0]).replace('{z}', '1').replace('{x}', '0').replace('{y}', '0').replace('{r}', '') : cfg.url.replace('{z}', '1').replace('{x}', '0').replace('{y}', '0')
      const resp = await fetch(testUrl, { signal: controller.signal, cache: 'no-cache' })
      clearTimeout(timeoutId)
      if (resp.ok && viewer && !viewer.isDestroyed()) {
        if (viewer.imageryLayers.length > 0) viewer.imageryLayers.remove(viewer.imageryLayers.get(0))
        viewer.imageryLayers.addImageryProvider(new Cesium.UrlTemplateImageryProvider({ url: cfg.url, subdomains: cfg.subdomains, credit: new Cesium.Credit(''), maximumLevel: 18 }))
        return
      }
    } catch { /* */ }
  }
}

function loadStoreMarkers() {
  viewer.entities.removeAll()
  storeList.value.forEach(store => {
    if (!store.longitude || !store.latitude) return
    const size = Math.max(8, Math.min(22, 8 + Math.log10((store.salesRevenue || 0) + 1) * 3))
    viewer.entities.add({
      id: `store-${store.id}`,
      position: Cesium.Cartesian3.fromDegrees(store.longitude, store.latitude),
      point: { pixelSize: size, color: Cesium.Color.fromCssColorString('#00d4ff'), outlineColor: Cesium.Color.WHITE, outlineWidth: 1, heightReference: Cesium.HeightReference.CLAMP_TO_GROUND },
      label: { text: store.name, font: '14px sans-serif', fillColor: Cesium.Color.WHITE, outlineColor: Cesium.Color.fromCssColorString('#0a1e3d'), outlineWidth: 3, verticalOrigin: Cesium.VerticalOrigin.BOTTOM, pixelOffset: new Cesium.Cartesian2(0, -size - 4), distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 8000000) }
    })
  })
}

function setupClickHandler() {
  new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas).setInputAction((click) => {
    const picked = viewer.scene.pick(click.position)
    if (Cesium.defined(picked) && picked.id?.id?.startsWith('store-')) {
      const sid = parseInt(picked.id.id.replace('store-', ''))
      const store = storeList.value.find(s => s.id === sid)
      if (store) router.push(`/store/detail/${store.id}`)
    }
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
}

// ===== ECharts =====
function initCharts() {
  trendChart = echarts.init(document.getElementById('trendChart'))
  pieChart = echarts.init(document.getElementById('pieChart'))
  barChart = echarts.init(document.getElementById('barChart'))
}

function updateTrendChart() {
  if (!trendChart) return
  const months = monthlySales.value.map(i => i.month)
  const amounts = monthlySales.value.map(i => Number(i.amount))
  const profits = monthlySales.value.map(i => Number(i.profit))
  trendChart.setOption({
    grid: { left: 12, right: 24, top: 8, bottom: 0 },
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }, axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } }, axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10, formatter: v => v >= 10000 ? (v / 10000).toFixed(0) + 'w' : v } },
    series: [
      { name: '销售额', type: 'line', data: amounts, smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { width: 2, color: '#409eff' }, itemStyle: { color: '#409eff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,0.35)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }]) } },
      { name: '利润', type: 'line', data: profits, smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { width: 2, color: '#67c23a' }, itemStyle: { color: '#67c23a' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(103,194,58,0.3)' }, { offset: 1, color: 'rgba(103,194,58,0.02)' }]) } }
    ]
  })
}

function updatePieChart() {
  if (!pieChart) return
  const data = categorySales.value.map(item => ({ name: item.categoryName || item.category, value: Number(item.amount) })).slice(0, 5)
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(10,30,60,0.9)', borderColor: 'rgba(64,158,255,0.3)', textStyle: { color: '#e0e6f0', fontSize: 12 } },
    legend: { bottom: 0, textStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 }, itemWidth: 8, itemHeight: 8 },
    series: [{ type: 'pie', radius: ['50%', '72%'], center: ['50%', '45%'], label: { color: 'rgba(255,255,255,0.6)', fontSize: 10 }, labelLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }, itemStyle: { borderColor: 'rgba(6,20,41,0.9)', borderWidth: 2 }, data }]
  })
}

function updateBarChart() {
  if (!barChart) return
  const colors = ['#67c23a', '#409eff', '#f56c6c', '#a855f7', '#e6a23c']
  const top5 = productSales.value.slice(0, 5)
  barChart.setOption({
    grid: { left: 72, right: 24, top: 4, bottom: 0 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } }, axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 10 } },
    yAxis: { type: 'category', inverse: true, data: top5.map(p => p.productName || p.name || '-'), axisLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11 }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', data: top5.map((p, i) => ({ value: Number(p.totalQuantity || p.quantity || 0), itemStyle: { color: colors[i % colors.length], borderRadius: [0, 4, 4, 0] } })),
      barWidth: 12,
      label: { show: true, position: 'right', color: 'rgba(255,255,255,0.6)', fontSize: 10, formatter: '{c}' }
    }]
  })
}

// ===== 数据加载 =====
async function loadAllData() {
  try {
    const [overviewRes, monthlyRes, storeRes, categoryRes, productRes, mapRes, alertsRes] = await Promise.all([
      getOverviewApi(), getSalesMonthlyApi(), getSalesByStoreApi(), getSalesByCategoryApi(),
      getProductSalesApi({ pageSize: 5 }), getStoreMapApi(), getInventoryAlertsApi()
    ])
    overview.value = overviewRes.data || {}
    monthlySales.value = monthlyRes.data || []
    storeSales.value = (storeRes.data || []).sort((a, b) => (Number(b.profit) || 0) - (Number(a.profit) || 0))
    categorySales.value = categoryRes.data || []
    productSales.value = (productRes.data?.records || productRes.data || [])
    storeList.value = mapRes.data || []
    inventoryAlerts.value = alertsRes.data || []

    updateTrendChart()
    updatePieChart()
    updateBarChart()
  } catch { ElMessage.warning('数据加载失败') }
}

// ===== 告警自动滚动 =====
function startAlertScroll() {
  stopAlertScroll()
  alertScrollTimer = setInterval(() => {
    const el = alertScrollRef.value
    if (!el) return
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 2) {
      el.scrollTop = 0
    } else {
      el.scrollTop += 1
    }
  }, 80)
}
function stopAlertScroll() {
  if (alertScrollTimer) { clearInterval(alertScrollTimer); alertScrollTimer = null }
}

// ===== 生命周期 =====
function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(async () => {
  if (firstLoad) {
    firstLoad = false
    startClock()
    fetchWeather()
    initScene()
    setupClickHandler()
    await nextTick()
    initCharts()
    await loadAllData()
    loadStoreMarkers()
    startAlertScroll()
  }
  window.addEventListener('resize', handleResize)
})

onActivated(() => {
  if (viewer && !viewer.isDestroyed()) { viewer.shouldAnimate = true; viewer.scene.requestRender() }
  nextTick(() => { trendChart?.resize(); pieChart?.resize(); barChart?.resize() })
})

onDeactivated(() => { if (viewer && !viewer.isDestroyed()) viewer.shouldAnimate = false })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (clockTimer) clearInterval(clockTimer)
  stopAlertScroll()
  trendChart?.dispose(); pieChart?.dispose(); barChart?.dispose()
  if (viewer && !viewer.isDestroyed()) { viewer.destroy(); viewer = null }
})
</script>

<template>
  <div class="dashboard">
    <!-- ====== 顶部标题栏 ====== -->
    <header class="dash-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="header-left">
          <span class="hl-date">{{ dateStr }}</span><span class="hl-divider">|</span>
          <span class="hl-week">{{ weekStr }}</span><span class="hl-divider">|</span>
          <span class="hl-time">{{ timeStr }}</span><span class="hl-divider">|</span>
          <span class="hl-weather">{{ weatherText }} {{ weatherTemp }}</span>
        </div>
        <div class="header-center"><span class="dash-title">智能数码零售管理系统</span></div>
        <div class="header-right">
          <el-button size="small" type="primary" plain @click="router.push('/store/list')">管理后台</el-button>
          <el-button size="small" text style="color:rgba(255,255,255,0.7)" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </header>

    <!-- ====== 三栏主体 ====== -->
    <div class="dash-body">
      <!-- ========== 左栏 ========== -->
      <aside class="dash-left">
        <!-- 经营总览 -->
        <div class="dash-card card-overview">
          <div class="card-title"><span class="title-icon"></span>经营总览</div>
          <div class="overview-grid">
            <div class="ov-item"><span class="ov-val">{{ totalStores }}</span><span class="ov-lbl">店铺总数</span></div>
            <div class="ov-item"><span class="ov-val">{{ totalProducts }}</span><span class="ov-lbl">商品种类</span></div>
            <div class="ov-item"><span class="ov-val green">{{ monthAmount }}</span><span class="ov-lbl">本月销售额</span></div>
            <div class="ov-item"><span class="ov-val blue">{{ monthProfit }}</span><span class="ov-lbl">本月利润</span></div>
          </div>
        </div>
        <!-- 销售趋势 -->
        <div class="dash-card card-panel flex-2">
          <div class="card-title"><span class="title-icon"></span>销售额 / 利润趋势</div>
          <div id="trendChart" class="chart-box"></div>
        </div>
        <!-- 门店排名表格 -->
        <div class="dash-card card-panel flex-3">
          <div class="card-title"><span class="title-icon"></span>门店营收排名</div>
          <div class="table-wrap">
            <table class="rank-table">
              <thead>
                <tr><th>#</th><th>店名</th><th>畅销商品</th><th>利润</th><th>同比</th></tr>
              </thead>
              <tbody>
                <tr v-for="(s, idx) in storeSales.slice(0, 8)" :key="idx">
                  <td><span class="rk-num" :class="'rk-' + (idx + 1)">{{ idx + 1 }}</span></td>
                  <td class="td-name">{{ s.storeName }}</td>
                  <td class="td-prod">{{ s.bestProductName || '-' }}</td>
                  <td class="td-profit">¥{{ formatMoney(s.profit) }}</td>
                  <td class="td-yoy" :class="{ up: (s.yoy || '').startsWith('+') || s.yoy === '新增', down: (s.yoy || '').startsWith('-') }">{{ s.yoy || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </aside>

      <!-- ========== 中栏 ========== -->
      <main class="dash-center">
        <div class="indicator-row">
          <div class="ind-card"><span class="ind-val">{{ overview.saleCount || 0 }}</span><span class="ind-lbl">交易笔数</span></div>
          <div class="ind-card"><span class="ind-val">{{ storeList.length }}</span><span class="ind-lbl">在线店铺</span></div>
          <div class="ind-card"><span class="ind-val danger">{{ storeList.filter(s => s.usageRate > 85).length }}</span><span class="ind-lbl">库存预警</span></div>
          <div class="ind-card"><span class="ind-val">{{ totalProducts }}</span><span class="ind-lbl">在售商品</span></div>
        </div>
        <div class="map-wrapper">
          <div v-if="!cesiumReady" class="map-loading"><span class="loader-ring"></span><p>地图加载中...</p></div>
          <div id="cesiumContainer"></div>
        </div>
        <div class="bottom-stats">
          <div class="stat-item"><span class="stat-label">同比增长</span><span class="stat-value up">▲ 12.5%</span></div>
          <div class="stat-item"><span class="stat-label">环比增长</span><span class="stat-value up">▲ 3.2%</span></div>
          <div class="stat-item"><span class="stat-label">目标完成率</span><span class="stat-value">87.6%</span></div>
          <div class="stat-item"><span class="stat-label">客单价</span><span class="stat-value">¥{{ formatMoney(overview.saleCount ? ((overview.monthSales || 0) / overview.saleCount) : 0) }}</span></div>
        </div>
      </main>

      <!-- ========== 右栏 ========== -->
      <aside class="dash-right">
        <!-- 营收结构 -->
        <div class="dash-card card-panel flex-2">
          <div class="card-title"><span class="title-icon"></span>营收结构</div>
          <div id="pieChart" class="chart-box"></div>
        </div>
        <!-- 畅销商品条形图 -->
        <div class="dash-card card-panel flex-2">
          <div class="card-title"><span class="title-icon"></span>畅销商品 TOP5</div>
          <div id="barChart" class="chart-box"></div>
        </div>
        <!-- 库存告警 -->
        <div class="dash-card card-panel flex-3">
          <div class="card-title"><span class="title-icon alert"></span>库存告警</div>
          <div class="alert-table-wrap" ref="alertScrollRef" @mouseenter="stopAlertScroll" @mouseleave="startAlertScroll">
            <table class="alert-table">
              <thead><tr><th>时间</th><th>店铺</th><th>商品</th><th>库存</th><th>类型</th></tr></thead>
              <tbody>
                <tr v-for="a in inventoryAlerts" :key="a.productId + '-' + a.storeId">
                  <td>{{ (a.updateTime || '').substring(0, 16) }}</td>
                  <td>{{ a.storeName }}</td>
                  <td class="td-prod">{{ a.productName }}</td>
                  <td class="td-qty">{{ a.quantity }}</td>
                  <td><span class="alert-tag" :class="a.alertType === 'urgent' ? 'tag-urgent' : 'tag-warn'">{{ a.alertType === 'urgent' ? '紧急' : '提醒' }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$dash-card-bg: rgba(10, 35, 70, 0.55);
$dash-card-border: rgba(64, 158, 255, 0.18);
$dash-text: #e0e6f0;
$dash-text-sub: rgba(224, 230, 240, 0.45);

.dashboard {
  width: 100vw; height: 100vh;
  background: #061429;
  background-image: radial-gradient(ellipse at 50% 0%, rgba(64,158,255,0.08) 0%, transparent 60%), radial-gradient(ellipse at 80% 20%, rgba(0,100,200,0.05) 0%, transparent 50%);
  display: flex; flex-direction: column; overflow: hidden;
  font-family: 'Microsoft YaHei', sans-serif;
  :deep(.el-button--primary.is-plain) {
    --el-button-bg-color: rgba(64,158,255,0.15); --el-button-border-color: rgba(64,158,255,0.4); --el-button-text-color: #409eff;
    --el-button-hover-bg-color: rgba(64,158,255,0.25); --el-button-hover-border-color: #409eff; --el-button-hover-text-color: #fff;
  }
}

// Header
.dash-header {
  height: 56px; position: relative; z-index: 20; flex-shrink: 0;
  .header-bg { position: absolute; inset: 0; background: url('/UI/0847.png') no-repeat center bottom; background-size: 100% 100%; opacity: 0.75; }
  .header-content { position: absolute; inset: 0; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
  .header-left { display: flex; align-items: center; gap: 8px; font-size: 13px; color: rgba(224,234,250,0.8);
    .hl-divider { color: rgba(255,255,255,0.25); }
    .hl-time { font-weight: 600; color: #409eff; letter-spacing: 1px; }
  }
  .header-center .dash-title { font-size: 26px; font-weight: 500; letter-spacing: 4px; color: rgba(252,253,254,0.92); text-shadow: 0 0 20px rgba(64,158,255,0.6); }
  .header-right { display: flex; align-items: center; gap: 12px; }
}

// Body
.dash-body { flex: 1; display: grid; grid-template-columns: 1fr 2.2fr 1fr; gap: 10px; padding: 10px; overflow: hidden; min-height: 0; }
.dash-left, .dash-right { display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.dash-center { display: flex; flex-direction: column; gap: 8px; overflow: hidden; }

// Card
.dash-card {
  background: $dash-card-bg; backdrop-filter: blur(12px); border: 1px solid $dash-card-border;
  border-radius: 8px; padding: 10px 12px; box-shadow: 0 2px 16px rgba(0,0,0,0.3);
  .card-title { font-size: 13px; font-weight: 500; color: rgba(200,215,240,0.85); margin-bottom: 8px; display: flex; align-items: center; gap: 8px;
    .title-icon { display: inline-block; width: 3px; height: 14px; background: linear-gradient(180deg, #409eff, #67c23a); border-radius: 2px; }
    .title-icon.alert { background: linear-gradient(180deg, #e6a23c, #f56c6c); }
    .title-icon.log { background: linear-gradient(180deg, #909399, #67c23a); }
  }
}
.card-panel { display: flex; flex-direction: column; overflow: hidden; }
.card-overview { flex-shrink: 0; }
.flex-2 { flex: 2; min-height: 0; }
.flex-3 { flex: 3; min-height: 0; }

// Overview
.card-overview .overview-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  .ov-item { background: rgba(0,60,120,0.18); border-radius: 6px; padding: 10px; text-align: center;
    .ov-val { display: block; font-size: 22px; font-weight: 600; color: #409eff;
      &.green { color: #67c23a; } &.blue { color: #409eff; }
    }
    .ov-lbl { font-size: 11px; color: $dash-text-sub; margin-top: 4px; display: block; }
  }
}

// Chart box
.chart-box { width: 100%; flex: 1; min-height: 0; }

// Rank table
.table-wrap { flex: 1; overflow: auto; min-height: 0; }
.rank-table { width: 100%; border-collapse: collapse; font-size: 11px;
  thead tr { border-bottom: 1px solid rgba(64,158,255,0.2); }
  th { padding: 4px 4px; color: $dash-text-sub; font-weight: 400; text-align: left; white-space: nowrap; }
  td { padding: 3px 4px; color: $dash-text; }
  .td-name { max-width: 70px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-prod { max-width: 60px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $dash-text-sub; font-size: 10px; }
  .td-profit { color: #67c23a; font-weight: 500; }
  .td-yoy { font-size: 10px;
    &.up { color: #f56c6c; }
    &.down { color: #67c23a; }
  }
  .rk-num { display: inline-block; width: 18px; height: 18px; border-radius: 3px; background: rgba(255,255,255,0.08); text-align: center; line-height: 18px; font-size: 10px; font-weight: 600; color: rgba(255,255,255,0.5);
    &.rk-1 { background: #f56c6c; color: #fff; }
    &.rk-2 { background: #e6a23c; color: #fff; }
    &.rk-3 { background: #409eff; color: #fff; }
  }
}

// Indicators
.indicator-row { display: flex; gap: 10px; flex-shrink: 0;
  .ind-card { flex: 1; background: rgba(10,35,70,0.5); border: 1px solid rgba(64,158,255,0.15); border-radius: 8px; padding: 8px 12px; text-align: center;
    .ind-val { display: block; font-size: 24px; font-weight: 700; color: #409eff;
      &.danger { color: #f56c6c; }
    }
    .ind-lbl { font-size: 11px; color: $dash-text-sub; margin-top: 2px; display: block; }
  }
}

// Map
.map-wrapper { flex: 1; min-height: 0; position: relative; border-radius: 8px; border: 1px solid rgba(64,158,255,0.15); overflow: hidden; background: #061429;
  #cesiumContainer { width: 100%; height: 100%;
    canvas { filter: brightness(0.8) saturate(0.9); }
  }
}
.map-loading { position: absolute; inset: 0; z-index: 10; display: flex; flex-direction: column; align-items: center; justify-content: center; background: rgba(6,20,41,0.95); color: #c9d1d9;
  p { margin-top: 12px; font-size: 14px; }
}
.loader-ring { width: 40px; height: 40px; border: 3px solid rgba(64,158,255,0.2); border-top-color: #409eff; border-radius: 50%; animation: dash-spin 1s linear infinite; }
@keyframes dash-spin { to { transform: rotate(360deg) } }

// Bottom stats
.bottom-stats { display: flex; gap: 10px; flex-shrink: 0;
  .stat-item { flex: 1; background: rgba(10,35,70,0.45); border: 1px solid rgba(64,158,255,0.12); border-radius: 6px; padding: 6px 12px; text-align: center;
    .stat-label { font-size: 11px; color: $dash-text-sub; display: block; }
    .stat-value { font-size: 14px; font-weight: 600; color: $dash-text; margin-top: 2px; display: block;
      &.up { color: #67c23a; }
    }
  }
}

// Alert table
.alert-table-wrap { flex: 1; overflow: auto; min-height: 0;
  // 隐藏滚动条
  &::-webkit-scrollbar { display: none; }
  -ms-overflow-style: none; scrollbar-width: none;
}
.alert-table { width: 100%; border-collapse: collapse; font-size: 10px;
  thead tr { border-bottom: 1px solid rgba(230,162,60,0.2); }
  th { padding: 2px 3px; color: $dash-text-sub; font-weight: 400; text-align: left; white-space: nowrap; }
  td { padding: 2px 3px; color: $dash-text; }
  .td-prod { max-width: 55px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-qty { color: #f56c6c; font-weight: 500; }
  .alert-tag { display: inline-block; padding: 1px 4px; border-radius: 2px; font-size: 9px;
    &.tag-urgent { background: rgba(245,108,108,0.2); color: #f56c6c; }
    &.tag-warn { background: rgba(230,162,60,0.2); color: #e6a23c; }
  }
}
</style>