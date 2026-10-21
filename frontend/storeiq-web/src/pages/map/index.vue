<template>
  <div class="dashboard">
    <!-- ====== 顶部标题栏 ====== -->
    <header class="dash-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <!-- 时钟独立子组件：秒级更新只在子组件内重渲染，不再触发整屏模板 diff -->
        <HeaderClock :weather-text="weatherText" :weather-temp="weatherTemp" />
        <div class="header-center"><span class="dash-title">智能数码零售管理系统</span></div>
        <div class="header-right">
          <el-button size="small" type="primary" plain class="logout-btn" @click="router.push('/store/list')">管理后台</el-button>
          <el-button size="small" type="primary" plain @click="handleLogout">退出登录</el-button>
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
          <div class="ind-card"><span class="ind-val">{{ onlineStoreCount }}</span><span class="ind-lbl">在线店铺</span></div>
          <div class="ind-card"><span class="ind-val danger">{{ warnStoreCount }}</span><span class="ind-lbl">库存预警</span></div>
          <div class="ind-card"><span class="ind-val">{{ totalProducts }}</span><span class="ind-lbl">在售商品</span></div>
        </div>
        <div class="map-toolbar">
          <span class="mt-label">显示停运店铺</span>
          <el-switch v-model="showGhosts" size="small" />
        </div>
        <div class="map-wrapper">
          <div v-if="!threeReady" class="map-loading"><span class="loader-ring"></span><p>地图加载中...</p></div>
          <div ref="threeContainerRef" class="three-container"></div>
          <div class="map-legend">
            <div class="lg-item"><span class="lg-icon lg-beam"></span>Top 3 核心营收（全息光柱）</div>
            <div class="lg-item"><span class="lg-icon lg-ring"></span>腰部店铺（雷达波纹）</div>
            <div class="lg-item"><span class="lg-icon lg-dot"></span>普通店铺（微光点）</div>
            <div class="lg-item"><span class="lg-icon lg-ghost"></span>已停运店铺（幽灵片）</div>
          </div>
        </div>
        <div class="bottom-stats">
          <div class="stat-item"><span class="stat-label">同比增长</span><span class="stat-value up">▲ 12.5%</span></div>
          <div class="stat-item"><span class="stat-label">环比增长</span><span class="stat-value up">▲ 3.2%</span></div>
          <div class="stat-item"><span class="stat-label">目标完成率</span><span class="stat-value">87.6%</span></div>
          <div class="stat-item"><span class="stat-label">客单价</span><span class="stat-value">¥{{ formatMoney(avgPrice) }}</span></div>
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
                  <td>{{ a.updateTime ? formatDateTime(a.updateTime, 'YYYY-MM-DD HH:mm') : '' }}</td>
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


<script setup>
/**
 * StoreIQ 首页大屏 — 三栏布局 + Three.js 3D 中国地图 + ECharts 图表
 * 暗蓝色科技风
 */
defineOptions({ name: 'MapView' })
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/pinia/stores/user'
import HeaderClock from './components/HeaderClock.vue'
import { formatDateTime } from '@@/utils/datetime'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getOverviewApi, getSalesMonthlyApi, getSalesByStoreApi, getSalesByCategoryApi, getProductSalesApi, getInventoryAlertsApi } from '@@/apis/storeiq/stats'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'

// ===== Three.js 核心模块 =====
import * as THREE from 'three'
// OrbitControls 用于旋转/缩放/平移场景
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
// 后处理：发光效果（Bloom）
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'
// 宽线/国界流光：Line2 + LineMaterial，流光通过 onBeforeCompile 向 LineMaterial 片元着色器注入代码实现
import { Line2 } from 'three/examples/jsm/lines/Line2.js'
import { LineGeometry } from 'three/examples/jsm/lines/LineGeometry.js'
import { LineMaterial } from 'three/examples/jsm/lines/LineMaterial.js'
// CSS2D：店铺名字标签 + 悬停信息卡（DOM 覆盖层）
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'

const router = useRouter()
const userStore = useUserStore()

function getCssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

// ===== 数据 =====
const storeList = ref([])
const threeReady = ref(false)
const threeContainerRef = ref(null)
const overview = ref({})
const monthlySales = ref([])
const storeSales = ref([])
const categorySales = ref([])
const productSales = ref([])
const inventoryAlerts = ref([])
const alertScrollRef = ref(null)
let alertScrollTimer = null

// ===== Three.js 核心对象 =====
let threeScene = null      // 场景
let threeCamera = null     // 透视相机
let threeRenderer = null   // WebGL 渲染器
let threeControls = null   // 轨道控制器（旋转/缩放/平移）
let threeAnimationId = null // requestAnimationFrame ID
let threeAmbientLight = null
let threeDirLight = null
let threeFillLight = null
let effectComposer = null  // 效果组合器
let bloomPass = null       // 发光通道
// hover 交互
let raycaster = null       // 射线检测器
let hoveredProvince = null // 当前悬停的省份 Group
const mouse = new THREE.Vector2() // 鼠标归一化坐标
let trendChart = null, pieChart = null, barChart = null
let firstLoad = true
let unmounted = false // 卸载标志：异步初始化链路各 await 后检查，防止加载中离开页面

// ===== 鼠标拾取状态（rAF 节流 + 缓存）=====
let pendingMouseEvent = null  // 最近一次 mousemove 事件（渲染循环中每帧至多处理一次）
let canvasRect = null         // 画布 getBoundingClientRect 缓存（resize 时失效）
let provincePickMeshes = null // 省份拾取 mesh 列表（首次拾取时预构建，之后复用）
let lastPickClientX = -1      // 上次拾取的屏幕坐标（位移 <3px 跳过射线检测）
let lastPickClientY = -1

// ===== 店铺标签 / 信息卡（CSS2D）=====
let labelRenderer = null      // CSS2D 渲染器（店铺名标签 + 悬停卡片）
const activePickMeshes = []   // 运营店铺拾取盒（全息柱/脉冲圈/微光点，不可见）
const ghostPickMeshes = []    // 停运幽灵片拾取盒
let visiblePickMeshes = []    // 当前参与拾取的盒子（受"显示停运店铺"开关影响）
const storeLabels = []        // 店铺名标签 CSS2DObject（卸载清理用）
const provinceLabels = []     // 省份名标签 CSS2DObject（卸载清理用）
let storeFlowTexture = null   // 运营中店铺共享的流动光带纹理（渲染循环推进 offset）
const particleSystems = []    // 全息柱内上升粒子（{ points, speeds, baseZ, topZ }）
const radarRings = []         // 全息柱底部雷达扫描环（{ mesh, wave }）
const rippleWaves = []        // 腰部店铺水波纹（每店两圈错相位，{ mesh, wave }）
const ghostGroup = new THREE.Group() // 停运幽灵片组（筛选开关整体显隐）
const showGhosts = ref(true)  // 是否显示停运店铺
let storeTooltip = null       // 悬停信息卡 CSS2DObject
let storeTooltipEl = null     // 信息卡 DOM 元素
let renderedTooltipKey = null // 信息卡已渲染内容标识（内容未变时跳过 DOM 重建）
let hoveredStore = null       // 当前悬停的店铺数据
let pinnedStore = null        // 双击固定显示卡片的店铺数据

/** 重建可见拾取列表（停运店铺开关切换时调用） */
function rebuildVisiblePickMeshes() {
  visiblePickMeshes = [...activePickMeshes]
  if (showGhosts.value) visiblePickMeshes.push(...ghostPickMeshes)
}
watch(showGhosts, (v) => {
  ghostGroup.visible = v
  rebuildVisiblePickMeshes()
})

// ===== 国界流光（最外层边界双层流光）=====
let outlineFlowGroup = null         // 流光 Group
const outlineFlowMaterials = []     // 底光线 LineMaterial（resize 时同步 resolution）
const cometFlowUniforms = []        // 彗星流光 uFlowTime uniform 引用（每帧推进）
const outlineClock = new THREE.Clock() // 流光动画时钟

const OUTLINE_BASE_COLOR = '#8ff4ff' // 底光常亮线（发亮的边界色）
const COMET_COLOR = '#e8fdff'        // 彗星流光头部色（近白青）
const COMET_LINEWIDTH = 3            // 彗星线宽（px）
const COMET_HEAD_WIDTH = 2           // 头部前缘软化宽度（世界单位）
const COMET_TAIL_WIDTH = 22          // 拖尾衰减长度（世界单位）
const FLOW_RING_COUNT = 1            // 只动画最大环（大陆轮廓）
const FLOW_MIN_POINTS = 50           // 过滤碎岛小环
const FLOW_SPEED = 7                 // 流速（世界单位/秒）

// ===== 顶部天气（时钟已拆分到 HeaderClock 子组件，避免秒级更新触发整屏重渲染）=====
const weatherText = ref('加载中...')
const weatherTemp = ref('--')

let weatherAbort = null // 天气请求中止控制器（卸载时取消在途请求）

async function fetchWeather() {
  weatherAbort?.abort()
  weatherAbort = new AbortController()
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
    const res = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat.toFixed(1)}&longitude=${lon.toFixed(1)}&current_weather=true`, { signal: weatherAbort.signal })
    const json = await res.json()
    const w = json.current_weather
    const map = { 0: '晴', 1: '晴', 2: '多云', 3: '阴', 45: '雾', 51: '小雨', 53: '小雨', 55: '中雨', 61: '阵雨', 63: '中雨', 65: '大雨', 71: '小雪', 73: '中雪', 75: '大雪', 80: '阵雨', 95: '雷暴' }
    weatherText.value = map[w.weathercode] || '晴'
    weatherTemp.value = `${w.temperature}°C`
  } catch (err) {
    // 主动中止/卸载：静默返回，不写默认值
    if (err?.name === 'AbortError' || unmounted) return
    weatherText.value = '晴'; weatherTemp.value = '22°C'
  }
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
// 模板内联表达式提取为 computed：原先每次模板重渲染都会重新求值
// 在线店铺/库存预警只统计运营中的店铺（/stores/map 已改为返回含停运店铺）
const onlineStoreCount = computed(() => storeList.value.filter((s) => Number(s.status ?? 1) === 1).length)
const warnStoreCount = computed(() => storeList.value.filter((s) => Number(s.status ?? 1) === 1 && s.usageRate > 85).length)
const avgPrice = computed(() => (overview.value?.saleCount ? (overview.value.monthSales || 0) / overview.value.saleCount : 0))
function formatMoney(val) {
  const n = Number(val)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}

// ===== 经纬度 ↔ Three.js 坐标映射 =====
// 中国地理中心约 (104°E, 36°N)，映射到场景原点附近
const CHINA_CENTER_LON = 104
const CHINA_CENTER_LAT = 36
const COORD_SCALE = 0.7 // 每经纬度对应 Three.js 单位长度
const PLATE_DEPTH = 1.2          // 省份板块统一厚度（沿 Z 轴挤出高度）
const JD_LINE_Z = PLATE_DEPTH + 0.02  // 十段线所在高度（略高于板块顶面）
const OUTLINE_Z = PLATE_DEPTH + 0.02  // 国界流光所在高度（略高于板块顶面）
const CAMERA_DIST_SCALE = 0.65   // 相机距离缩放：越小地图越大（0.5≈更大，0.8≈更小）

// ===== 相机视角可调参数（决定初始视角；运行中仍可鼠标旋转/缩放/平移）=====
const CAMERA_ELEVATION_DEG = 52  // 俯角：45=标准斜俯视；调大(如60)更接近垂直俯视，调小(如25)更平视
const CAMERA_AZIMUTH_DEG = 12    // 方位角：从地图正前方(南侧)向西偏的角度；0=正前方，31=西南侧(当前值)
const CAMERA_HEIGHT = 42       // 相机高度：与 CAMERA_DIST_SCALE 共同决定远近，也参与俯角计算

/** 由俯角/方位角/高度计算相机相对地图中心的三维偏移（初始视角与 fitCameraToChina 共用） */
function calcCameraOffset() {
  const elevRad = CAMERA_ELEVATION_DEG * Math.PI / 180
  const azimRad = CAMERA_AZIMUTH_DEG * Math.PI / 180
  const horiz = CAMERA_HEIGHT / Math.tan(elevRad) // 水平距离
  return {
    left: Math.sin(azimRad) * horiz,  // 西向偏移
    front: Math.cos(azimRad) * horiz, // 南向偏移
    height: CAMERA_HEIGHT
  }
}

// ===== 发光/柱体可调参数（手动调整到满意为止）=====
const BLOOM_STRENGTH = 0.4    // Bloom 发光强度：越大光晕越强（影响所有发光元素）
const BLOOM_RADIUS = 0.8      // Bloom 扩散半径：越大光晕越柔和
const BLOOM_THRESHOLD = 0.5   // Bloom 亮度阈值：比它亮的像素才发光（边界线约 0.55 > 此值仍发光；柱顶已调暗到 0.34 不发光）
const MAX_PIXEL_RATIO = 1.5   // 渲染像素比上限：2K 屏全分辨率 + Bloom 后处理开销过大；1.5 视觉差异小、GPU 负担明显降低（EffectComposer 各 RenderTarget 同步按此缩放）
// ===== 店铺分层视觉常量（真实坐标，形态区分层级，详见 loadStorePillars）=====
const TIER2_START = 3          // 第 1 层（全息光柱）数量：利润排名前 3
const TIER3_START = 8          // 第 2 层（脉冲圈）：排名 4~8；其后为第 3 层微光点
const PILLAR_HEIGHT_MIN = 0.5  // 全息柱最低高度（利润为 0 或负数时）
const PILLAR_HEIGHT_MAX = 6    // 全息柱最高高度（柱高与利润线性映射于两者之间）
const BEAM_CORE_RADIUS = 0.07  // 全息柱发光内芯半径（细，控制发光面积）
const BEAM_SHELL_RADIUS = 0.3  // 全息柱外层透明壳半径（贴螺旋光带，基底压暗不泛光）
const BEAM_SHELL_OPACITY = 0.22
const BEAM_PARTICLE_COUNT = 24 // 每根全息柱的上升粒子数（20~30 之间，性能考虑）
const PARTICLE_RISE_SPEED = 2.4 // 粒子上升速度（世界单位/秒）
const RADAR_RING_MAX = 1.1     // 雷达扫描环扩散最大半径（控制在小范围，防止 Top3 相邻时叠圈）
const RADAR_RING_PERIOD = 2.6  // 雷达波扩散周期（秒）
const RING_BAND_WIDTH = 0.085  // 波纹圈环带宽（加粗提升可见度）
const RING_RADIUS_MIN = 0.38   // 波纹圈最小半径（4~8 名半径按营收映射）
const RING_RADIUS_MAX = 1.0    // 波纹圈最大半径
const RIPPLE_PERIOD = 3.2      // 水波纹扩散周期（秒）：从中心向外扩散一圈
const RIPPLE_START = 0.15      // 波纹起始缩放（从中心小圈开始向外扩散）
const CENTER_DOT_SIZE = 0.2    // 波纹中心点尺寸（颜色与波纹一致）
const DOT_SIZE = 0.3           // 微光点尺寸（默认视角下需清晰可见）
const GHOST_RADIUS = 0.16      // 停运幽灵片半径
const FLOW_BAND_SPEED = 0.5    // 赛博光带流速（纹理周期/秒，约 2s 螺旋一周；调大更快更凌厉）

/**
 * 将经纬度转为场景 XY 平面坐标
 * X 轴对应东西方向（经度），Y 轴对应南北方向（纬度）
 * 后续 ExtrudeGeometry 沿 Z 轴挤出高度
 */
function lonLatToXY(lon, lat) {
  return {
    x: (lon - CHINA_CENTER_LON) * COORD_SCALE,
    y: (lat - CHINA_CENTER_LAT) * COORD_SCALE
  }
}

// 存储所有省份的 Group，key 为省份名，供后续 hover 交互使用
const provinceGroupMap = new Map()

// ===== Three.js 场景初始化 =====
function initThreeScene() {
  const container = threeContainerRef.value
  if (!container) return

  // ---- 1. 创建场景 ----
  threeScene = new THREE.Scene()
  const sceneBg = getCssVar('--map-three-bg') || '#061429'
  threeScene.background = new THREE.Color(sceneBg)
  threeScene.fog = new THREE.Fog(sceneBg, 80, 300)

  // ---- 2. 创建透视相机 ----
  const aspect = container.clientWidth / container.clientHeight
  threeCamera = new THREE.PerspectiveCamera(45, aspect, 0.1, 500)
  // 场景以 Z 轴为高度方向，相机 up 也必须设为 Z 轴：
  // 否则 OrbitControls 默认以 +Y（即地图的北方）为"上"来度量 maxPolarAngle，
  // 西南侧机位会被 maxPolarAngle 钳制，强行拉回北侧
  threeCamera.up.set(0, 0, 1)
  // 相机初始视角：由顶部"相机视角可调参数"计算（俯角/方位角/高度）
  const camOff = calcCameraOffset()
  threeCamera.position.set(-camOff.left * CAMERA_DIST_SCALE, -camOff.front * CAMERA_DIST_SCALE, camOff.height * CAMERA_DIST_SCALE)
  threeCamera.lookAt(0, 0, 0)

  // ---- 3. 创建 WebGL 渲染器 ----
  threeRenderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
  threeRenderer.setSize(container.clientWidth, container.clientHeight)
  // 限制像素比，兼顾清晰度与性能（MAX_PIXEL_RATIO 见顶部可调参数）
  threeRenderer.setPixelRatio(Math.min(window.devicePixelRatio, MAX_PIXEL_RATIO))
  container.appendChild(threeRenderer.domElement)

  // ---- 3.5 CSS2D 渲染器（店铺名标签 + 悬停信息卡，DOM 覆盖在画布之上）----
  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(container.clientWidth, container.clientHeight)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0'
  labelRenderer.domElement.style.left = '0'
  // 不拦截鼠标事件：OrbitControls 与射线拾取都监听在 canvas 上
  labelRenderer.domElement.style.pointerEvents = 'none'
  container.appendChild(labelRenderer.domElement)

  // ---- 4. 轨道控制器 ----
  threeControls = new OrbitControls(threeCamera, threeRenderer.domElement)
  // 启用惯性阻尼，拖动更平滑
  threeControls.enableDamping = true
  threeControls.dampingFactor = 0.08
  // 缩放范围
  threeControls.minDistance = 15
  threeControls.maxDistance = 180
  // 限制俯角：相机 up 为 Z 轴，此角从正上方起算，85.7° 即最低只允许平视略偏上，防止翻到地图下方
  threeControls.maxPolarAngle = Math.PI / 2.1
  // 设置旋转中心为场景原点
  threeControls.target.set(0, 0, 0)
  threeControls.update()

  // ---- 5. 灯光 ----
  const ambientColor = getCssVar('--map-three-ambient') || '#3a4d70'
  const dirColor = getCssVar('--map-three-dir') || '#7fa5e0'
  const fillColor = getCssVar('--map-three-fill') || '#5577aa'
  threeAmbientLight = new THREE.AmbientLight(ambientColor, 2.0)
  threeScene.add(threeAmbientLight)
  threeDirLight = new THREE.DirectionalLight(dirColor, 1.5)
  threeDirLight.position.set(20, -20, 40)
  threeScene.add(threeDirLight)
  threeFillLight = new THREE.DirectionalLight(fillColor, 0.5)
  threeFillLight.position.set(-15, 12, 8)
  threeScene.add(threeFillLight)

  // ---- 6. 后处理管线（发光 Bloom） ----
  effectComposer = new EffectComposer(threeRenderer)
  // 基础渲染通道
  const renderPass = new RenderPass(threeScene, threeCamera)
  effectComposer.addPass(renderPass)
  // 发光通道：只有亮度超过阈值的像素才会"溢光"
  bloomPass = new UnrealBloomPass(
    new THREE.Vector2(container.clientWidth, container.clientHeight),
    BLOOM_STRENGTH, // strength：发光强度（见文件顶部常量）
    BLOOM_RADIUS,   // radius：发光扩散半径
    BLOOM_THRESHOLD // threshold：亮度阈值
  )
  effectComposer.addPass(bloomPass)

  // ---- 7. 射线检测器（省份 hover） ----
  raycaster = new THREE.Raycaster()
  // 监听鼠标移动
  threeRenderer.domElement.addEventListener('mousemove', onMouseMove)
  // 双击：固定/取消固定店铺信息卡
  threeRenderer.domElement.addEventListener('dblclick', onDoubleClick)

  // ---- 8. 渲染循环 ----
  function animate() {
    threeAnimationId = requestAnimationFrame(animate)
    threeControls.update()
    // 鼠标拾取：rAF 节流，每帧至多处理一次（与渲染同频）
    processPendingPick()
    const dt = Math.min(outlineClock.getDelta(), 0.1)
    // 国界流光：流动虚线匀速行进
    updateFlowAnimation(dt)
    // 店铺柱体流动光带：运营中柱体共享纹理，光带自下而上推进（停运柱体静止）
    if (storeFlowTexture) storeFlowTexture.offset.y -= FLOW_BAND_SPEED * dt
    // 全息柱内粒子上升
    for (const ps of particleSystems) {
      const attr = ps.points.geometry.attributes.position
      for (let i = 0; i < attr.count; i++) {
        let z = attr.getZ(i) + PARTICLE_RISE_SPEED * ps.speeds[i] * dt
        if (z > ps.topZ) z = ps.baseZ
        attr.setZ(i, z)
      }
      attr.needsUpdate = true
    }
    // 雷达扫描环扩散
    for (const ring of radarRings) {
      ring.wave = (ring.wave + dt / RADAR_RING_PERIOD) % 1
      const s = 0.4 + ring.wave * (RADAR_RING_MAX - 0.4)
      ring.mesh.scale.set(s, s, 1)
      ring.mesh.material.opacity = (1 - ring.wave) * 0.8
    }
    // 腰部店铺水波纹：从中心向外扩散（双圈错相位，店铺间错开节奏）
    for (const w of rippleWaves) {
      w.wave = (w.wave + dt / RIPPLE_PERIOD) % 1
      const s = RIPPLE_START + w.wave * (1 - RIPPLE_START)
      w.mesh.scale.set(s, s, 1)
      w.mesh.material.opacity = (1 - w.wave) * 0.9
    }
    // 使用 EffectComposer 渲染（包含 Bloom 后处理）
    effectComposer.render()
    // CSS2D 标签/信息卡渲染
    labelRenderer?.render(threeScene, threeCamera)
  }
  animate()
}

// ===== 从 GeoJSON 环坐标创建 THREE.Shape =====
/**
 * 将 GeoJSON 的一个环（坐标数组）转为 THREE.Shape
 * 外环为实心区域，holes 为挖空区域（如岛屿中间的湖）
 */
function createShapeFromRing(outerRing, holes = []) {
  if (!outerRing || outerRing.length < 3) return null

  const shape = new THREE.Shape()

  // 绘制外环轮廓
  const first = lonLatToXY(outerRing[0][0], outerRing[0][1])
  shape.moveTo(first.x, first.y)
  for (let i = 1; i < outerRing.length; i++) {
    const pt = lonLatToXY(outerRing[i][0], outerRing[i][1])
    shape.lineTo(pt.x, pt.y)
  }
  // 自动闭合（moveTo + lineTo 构成的路径 ExtrudeGeometry 会自动处理）

  // 添加孔洞（内环）
  holes.forEach((holeRing) => {
    if (holeRing.length < 3) return
    const holePath = new THREE.Path()
    const hFirst = lonLatToXY(holeRing[0][0], holeRing[0][1])
    holePath.moveTo(hFirst.x, hFirst.y)
    for (let i = 1; i < holeRing.length; i++) {
      const pt = lonLatToXY(holeRing[i][0], holeRing[i][1])
      holePath.lineTo(pt.x, pt.y)
    }
    shape.holes.push(holePath)
  })

  return shape
}

// ===== 加载中国地图 GeoJSON 并生成省份 3D 板块 =====
async function loadChinaProvinces() {
  try {
    const response = await fetch('/geo/china.json')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const geoJson = await response.json()

    // 加载期间已卸载：放弃后续构建，避免向已销毁场景添加对象
    if (unmounted || !threeScene) return

    if (!geoJson.features || !Array.isArray(geoJson.features)) {
      throw new Error('GeoJSON 格式错误：缺少 features 数组')
    }

    // 遍历每个省份
    const provColors = getProvinceColors()
    geoJson.features.forEach((feature) => {
      const { name, adcode, center } = feature.properties
      const { type, coordinates } = feature.geometry

      // 十段线（南海断续线）：不挤出板块，按虚线渲染（避免厚度 1.2 后成为丑陋窄条）
      if (String(adcode) === '100000_JD') {
        const jdGroup = new THREE.Group()
        jdGroup.name = 'china-jd-dashed'
        coordinates.forEach((polygon) => {
          const ring = polygon[0] || []
          if (ring.length < 2) return
          const pts = ring.map(([lon, lat]) => {
            const p = lonLatToXY(lon, lat)
            return new THREE.Vector3(p.x, p.y, JD_LINE_Z)
          })
          const line = new THREE.Line(
            new THREE.BufferGeometry().setFromPoints(pts),
            new THREE.LineDashedMaterial({ color: getCssVar('--map-three-jd-color') || '#66ccff', dashSize: 1.2, gapSize: 0.8, transparent: true, opacity: 0.9 })
          )
          line.computeLineDistances() // 必须调用，否则虚线显示为实线
          jdGroup.add(line)
        })
        threeScene.add(jdGroup)
        return // 跳过挤出/边框/hover 集合
      }

      // 每个省份用一个 Group 包裹（方便后续整体 hover 变色）
      const provinceGroup = new THREE.Group()
      provinceGroup.name = name
      provinceGroup.userData = { adcode, provinceName: name }

      if (type === 'MultiPolygon') {
        // MultiPolygon：coordinates 是三维数组 [[[外环], [孔洞]], ...]
        coordinates.forEach((polygon) => {
          const outerRing = polygon[0]
          const holes = polygon.slice(1)
          const shape = createShapeFromRing(outerRing, holes)
          if (!shape) return

          // 沿 Z 轴挤出 3D 板块（depth 控制省份厚度）
          const geometry = new THREE.ExtrudeGeometry(shape, {
            depth: PLATE_DEPTH,
            bevelEnabled: true,
            bevelThickness: 0.03,
            bevelSize: 0.03,
            bevelSegments: 1
          })

          // 板块材质：实心亮蓝（常态即原 hover 样式），后续加发光边框
          const material = new THREE.MeshPhongMaterial({
            color: provColors.normalColor,
            emissive: provColors.normalEmissive,
            emissiveIntensity: provColors.normalEmissiveIntensity,
            shininess: 8
          })

          const mesh = new THREE.Mesh(geometry, material)
          mesh.userData = { provinceName: name, adcode }
          provinceGroup.add(mesh)
        })
      } else if (type === 'Polygon') {
        // Polygon：coordinates 是二维数组 [[外环], [孔洞]]
        const outerRing = coordinates[0]
        const holes = coordinates.slice(1)
        const shape = createShapeFromRing(outerRing, holes)
        if (!shape) return

        const geometry = new THREE.ExtrudeGeometry(shape, {
          depth: PLATE_DEPTH,
          bevelEnabled: true,
          bevelThickness: 0.03,
          bevelSize: 0.03,
          bevelSegments: 1
        })
        const material = new THREE.MeshPhongMaterial({
          color: provColors.normalColor,
          emissive: provColors.normalEmissive,
          emissiveIntensity: provColors.normalEmissiveIntensity,
          shininess: 8
        })
        const mesh = new THREE.Mesh(geometry, material)
        mesh.userData = { provinceName: name, adcode }
        provinceGroup.add(mesh)
      }

      // 添加省份边框线（白色半透明，勾勒轮廓）
      provinceGroup.children.forEach((child) => {
        if (child instanceof THREE.Mesh) {
          const edgesGeo = new THREE.EdgesGeometry(child.geometry, 30)
          const edgesLine = new THREE.LineSegments(
            edgesGeo,
            new THREE.LineBasicMaterial({
              color: provColors.normalBorderColor,
              transparent: true,
              opacity: provColors.normalBorderOpacity
            })
          )
          provinceGroup.add(edgesLine)
        }
      })

      threeScene.add(provinceGroup)
      provinceGroupMap.set(name, provinceGroup)

      // 省份名：Canvas 纹理平面平贴在板块顶面（跟随地图透视，而非屏幕漂浮）
      if (Array.isArray(center)) {
        const p = lonLatToXY(center[0], center[1])
        const tex = createTextTexture(name)
        const labelW = 1.2 * (tex.image.width / tex.image.height)
        const labelMesh = new THREE.Mesh(
          new THREE.PlaneGeometry(labelW, 1.2),
          new THREE.MeshBasicMaterial({ map: tex, transparent: true, depthWrite: false })
        )
        // 每个标签高度递增 0.01：避免相邻标签平面同层重叠时深度冲突（闪烁）
        labelMesh.position.set(p.x, p.y, PLATE_DEPTH + 0.06 + provinceLabels.length * 0.01)
        threeScene.add(labelMesh)
        provinceLabels.push(labelMesh)
      }
    })

    // 调整相机视角完整显示中国地图
    fitCameraToChina()
    threeReady.value = true
  } catch (err) {
    console.error('加载中国地图数据失败:', err)
    // 即使失败也显示空场景，不阻塞整个页面
    threeReady.value = true
  }
}

// ===== 国界最外层轮廓双层流光 =====
/**
 * 加载离线生成的全国外轮廓（public/geo/china-outline.json），
 * 沿大陆边界绘制两层线：常亮底光带 + 匀速流动的亮虚线（跑马灯）
 */
async function loadChinaOutline() {
  try {
    const response = await fetch('/geo/china-outline.json')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const data = await response.json()

    // 加载期间已卸载：放弃后续构建
    if (unmounted || !threeScene) return

    const { clientWidth, clientHeight } = threeContainerRef.value
    const rings = (data.rings || []).filter((r) => r.pointCount >= FLOW_MIN_POINTS)
    if (!rings.length) return

    outlineFlowGroup = new THREE.Group()
    outlineFlowGroup.name = 'china-outline-flow'

    rings.slice(0, FLOW_RING_COUNT).forEach((ring) => {
      // 世界坐标点集 + 环总长（彗星无缝循环依据）
      const pts = ring.points.map(([lon, lat]) => lonLatToXY(lon, lat))
      let totalLength = 0
      for (let i = 0; i < pts.length - 1; i++) {
        totalLength += Math.hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
      }
      const flat = []
      pts.forEach((p) => flat.push(p.x, p.y, OUTLINE_Z))

      // 底光常亮线：Bloom 下呈柔光带，边界始终可见
      const baseGeo = new LineGeometry()
      baseGeo.setPositions(flat)
      const baseMat = new LineMaterial({
        color: getCssVar('--map-three-outline-base') || OUTLINE_BASE_COLOR,
        linewidth: 2,
        opacity: 0.9,
        transparent: true,
        depthWrite: false, // 不写深度：否则会遮挡其上方 0.02 处的彗星流光
        resolution: new THREE.Vector2(clientWidth, clientHeight)
      })
      outlineFlowGroup.add(new Line2(baseGeo, baseMat))
      outlineFlowMaterials.push(baseMat)

      // 彗星流光：明亮头部 + 指数衰减拖尾沿边界连续流动（略高于底光线，避免同层线深度冲突）
      const cometFlat = []
      pts.forEach((p) => cometFlat.push(p.x, p.y, OUTLINE_Z + 0.02))
      const cometGeo = new LineGeometry()
      cometGeo.setPositions(cometFlat)
      // dashed:true 启用 USE_DASH：顶点着色器输出 vLineDistance（线段累计距离），
      // 片元中的原生虚线逻辑会被 onBeforeCompile 注入的流光代码替换
      const cometMat = new LineMaterial({
        color: getCssVar('--map-three-comet') || COMET_COLOR,
        linewidth: COMET_LINEWIDTH,
        dashed: true,
        dashSize: 1,
        gapSize: 1,
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        resolution: new THREE.Vector2(clientWidth, clientHeight)
      })
      // 流光核心（老师方案）：onBeforeCompile 注入代码，用 vLineDistance 计算流光头部位置
      const flowUniform = { value: 0 }
      cometMat.onBeforeCompile = (shader) => {
        // 1) 在全局作用域声明流光 uniform（main 函数内部不能声明 uniform）
        shader.fragmentShader = [
          'uniform float uFlowTime;',
          'uniform float uTotalLength;',
          'uniform float uHeadWidth;',
          'uniform float uTailWidth;',
          ''
        ].join('\n') + shader.fragmentShader
        shader.uniforms.uFlowTime = flowUniform
        shader.uniforms.uTotalLength = { value: totalLength }
        shader.uniforms.uHeadWidth = { value: COMET_HEAD_WIDTH }
        shader.uniforms.uTailWidth = { value: COMET_TAIL_WIDTH }
        // 2) 替换原生虚线丢弃逻辑为彗星流光计算
        const dashDiscard = 'if ( mod( vLineDistance + dashOffset, dashSize + gapSize ) > dashSize ) discard; // todo - FIX'
        if (!shader.fragmentShader.includes(dashDiscard)) {
          console.warn('国界流光注入失败：未找到虚线丢弃代码')
          return
        }
        shader.fragmentShader = shader.fragmentShader.replace(
          dashDiscard,
          [
            '// 彗星流光：d = 该点落后于头部的距离（对环长取模，无缝循环）',
            'float d = mod( uFlowTime - vLineDistance, uTotalLength );',
            'if ( d < 0.0 ) d += uTotalLength;',
            'alpha *= exp( - d / uTailWidth ) * ( 1.0 - smoothstep( - uHeadWidth, 0.0, d ) );'
          ].join('\n')
        )
      }
      const cometLine = new Line2(cometGeo, cometMat)
      cometLine.computeLineDistances() // 创建 instanceDistance 属性（USE_DASH 顶点着色器需要）
      cometLine.renderOrder = 2 // 透明物体后渲染：保证彗星叠加在底光带之上
      outlineFlowGroup.add(cometLine)
      cometFlowUniforms.push(flowUniform)
    })

    threeScene.add(outlineFlowGroup)
  } catch (err) {
    // 轮廓加载失败降级：不影响地图主体
    console.warn('国界轮廓流光加载失败:', err)
  }
}

/** 国界流光动画：彗星头部匀速推进（uFlowTime 增大，光沿边界滑行） */
function updateFlowAnimation(dt) {
  for (const u of cometFlowUniforms) {
    u.value += FLOW_SPEED * dt
  }
}

// ===== 调整相机使中国地图完整可见 =====
function fitCameraToChina() {
  // 中国边界映射后的坐标
  const minX = (73 - CHINA_CENTER_LON) * COORD_SCALE
  const maxX = (135 - CHINA_CENTER_LON) * COORD_SCALE
  const minY = (18 - CHINA_CENTER_LAT) * COORD_SCALE
  const maxY = (54 - CHINA_CENTER_LAT) * COORD_SCALE
  const cx = (minX + maxX) / 2
  const cy = (minY + maxY) / 2

  threeControls.target.set(cx, cy, 0)
  // 相机视角由顶部常量计算：俯角 45°、西南侧方位，可看到顶面/南侧正面/西侧左侧面
  const camOff = calcCameraOffset()
  threeCamera.position.set(
    cx - camOff.left * CAMERA_DIST_SCALE,
    cy - camOff.front * CAMERA_DIST_SCALE,
    camOff.height * CAMERA_DIST_SCALE
  )
  threeCamera.lookAt(cx, cy, 0)
  threeControls.update()
}

// ===== 店铺利润渐变柱 =====
/**
 * 生成省份名 Canvas 纹理（平贴在地图板块顶面）
 */
function createTextTexture(text) {
  const canvas = document.createElement('canvas')
  canvas.height = 40
  const ctx = canvas.getContext('2d')
  const font = '300 16px "Microsoft YaHei", sans-serif'
  ctx.font = font
  const textW = ctx.measureText(text).width
  canvas.width = Math.max(40, Math.ceil(textW + 16))
  // canvas.width 赋值会重置上下文状态，需重新设置
  ctx.font = font
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillStyle = getCssVar('--map-three-label-text') || 'rgba(160, 168, 184, 0.95)'
  ctx.fillText(text, canvas.width / 2, canvas.height / 2)
  const tex = new THREE.CanvasTexture(canvas)
  // 关闭 mipmap：平贴地面的文字在斜视角度下 mip 层级切换会产生闪烁
  tex.minFilter = THREE.LinearFilter
  tex.generateMipmaps = false
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

/** 生成径向渐变小光点纹理（全息柱内上升粒子 / 微光点 Sprite 共用） */
function createDotTexture(innerColor = 'rgba(255,255,255,1)', outerColor = 'rgba(140,240,255,0.4)') {
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')
  const g = ctx.createRadialGradient(16, 16, 0, 16, 16, 16)
  g.addColorStop(0, innerColor)
  g.addColorStop(0.35, outerColor)
  g.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 32, 32)
  return new THREE.CanvasTexture(canvas)
}

/**
 * 生成运营中店铺的"赛博光带"纹理（附着柱面、青色螺旋上升）：
 * - 基底：青→蓝纵向渐变（低调偏暗，衬托光带）
 * - 静态纵向金属反光（不随 offset 移动，营造金属柱面质感）
 * - 斜向亮带（全宽对角线 + wrapS/wrapT 无缝衔接）：offset.y 推进时光带环绕柱体螺旋上升，
 *   白芯 + 青色光晕 + 后方拖影，快速锐利
 */
function createFlowTexture() {
  const canvas = document.createElement('canvas')
  canvas.width = 64
  canvas.height = 256
  const ctx = canvas.getContext('2d')

  // 1) 基底：青→蓝纵向渐变（比静态纹理更暗，突出光带）
  const gradient = ctx.createLinearGradient(0, 0, 0, 256)
  gradient.addColorStop(0, getCssVar('--map-three-gradient-0') || '#052342')
  gradient.addColorStop(0.4, getCssVar('--map-three-gradient-1') || '#0a3d70')
  gradient.addColorStop(0.75, getCssVar('--map-three-gradient-2') || '#0d5e92')
  gradient.addColorStop(1, getCssVar('--map-three-gradient-3') || '#12a5c6')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 64, 256)

  // 2) 金属反光：静态纵向高光（不随 offset 移动）
  const sheen = ctx.createLinearGradient(0, 0, 64, 0)
  sheen.addColorStop(0, 'rgba(255,255,255,0)')
  sheen.addColorStop(0.18, 'rgba(170,235,255,0.10)')
  sheen.addColorStop(0.26, 'rgba(255,255,255,0.20)')
  sheen.addColorStop(0.34, 'rgba(170,235,255,0.05)')
  sheen.addColorStop(0.72, 'rgba(130,220,255,0.05)')
  sheen.addColorStop(0.86, 'rgba(255,255,255,0.10)')
  sheen.addColorStop(1, 'rgba(255,255,255,0)')
  ctx.fillStyle = sheen
  ctx.fillRect(0, 0, 64, 256)

  // 3) 拖影：主光带后方的青色尾迹（向下偏移 24px，循环时从顶部无缝衔接）
  ctx.strokeStyle = 'rgba(70,215,255,0.22)'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.moveTo(0, -24)
  ctx.lineTo(64, 232)
  ctx.stroke()

  // 4) 主光带：外发光 → 主体 → 白色核心（同一路径三次描边）
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(64, 256)
  ctx.strokeStyle = 'rgba(50,210,255,0.35)'
  ctx.lineWidth = 10
  ctx.stroke()
  ctx.strokeStyle = 'rgba(110,240,255,0.9)'
  ctx.lineWidth = 3.2
  ctx.stroke()
  ctx.strokeStyle = 'rgba(255,255,255,0.95)'
  ctx.lineWidth = 1.2
  ctx.stroke()

  const tex = new THREE.CanvasTexture(canvas)
  tex.wrapS = THREE.RepeatWrapping
  tex.wrapT = THREE.RepeatWrapping
  return tex
}

/**
 * 店铺分层渲染（真实坐标，不做错位），形态区分层级：
 * 第 1 层（利润 Top3）：全息光柱 = 细发光内芯 + 透明外层壳(螺旋光带) + 上升粒子 + 底部雷达扫描环
 * 第 2 层（4~8 名）：扁平脉冲圈，半径映射营收，alpha 呼吸动画（不泛光）
 * 第 3 层（9 名起）：微光点，低透明辅助色，融入底盘
 * 停运店铺：幽灵片，深灰半透明贴地圆片，无任何发光（hover 高亮边框 + 停运提示）
 */
function loadStorePillars() {
  // 加载期间已卸载：不创建任何资源（否则创建的对象会绕过卸载清理）
  if (unmounted || !threeScene) return
  if (!storeList.value.length) return

  // 过滤出有坐标的店铺
  const validStores = storeList.value.filter(s => s.longitude && s.latitude)
  if (!validStores.length) return

  storeFlowTexture = createFlowTexture() // 全息柱外层壳共享纹理（渲染循环推进 offset）
  const particleTex = createDotTexture('rgba(255,255,255,1)', 'rgba(150,240,255,0.5)')
  const dotTex = createDotTexture('rgba(255,255,255,1)', 'rgba(255,244,214,0.35)') // 亮白核心 + 淡黄光晕

  const revenueOf = (s) => Number(s.profit ?? s.totalSales ?? 0)
  const running = validStores.filter(s => Number(s.status ?? 1) === 1)
  const stopped = validStores.filter(s => Number(s.status ?? 1) !== 1)
  // 按利润从大到小分档
  const sorted = [...running].sort((a, b) => revenueOf(b) - revenueOf(a))
  const tier1 = sorted.slice(0, TIER2_START)
  const tier2 = sorted.slice(TIER2_START, TIER3_START)
  const tier3 = sorted.slice(TIER3_START)

  const tier1Revenues = tier1.map(revenueOf)
  const minRev = Math.min(...tier1Revenues, 0)
  const maxRev = Math.max(...tier1Revenues, 1)
  const heightRange = maxRev - minRev || 1

  ghostGroup.name = 'stopped-store-ghosts'
  threeScene.add(ghostGroup)

  // ===== 第 1 层：全息光柱（Top 3）=====
  tier1.forEach((store, i) => {
    const pos = lonLatToXY(store.longitude, store.latitude)
    const revenue = revenueOf(store)
    const height = PILLAR_HEIGHT_MIN + ((revenue - minRev) / heightRange) * (PILLAR_HEIGHT_MAX - PILLAR_HEIGHT_MIN)
    const warn = Number(store.usageRate) > 85
    const baseZ = PLATE_DEPTH
    const topZ = baseZ + height

    // 内芯：细发光核心（预警变红）
    const core = new THREE.Mesh(
      new THREE.CylinderGeometry(BEAM_CORE_RADIUS, BEAM_CORE_RADIUS, height, 8),
      new THREE.MeshBasicMaterial({ color: warn ? '#ff7a6b' : '#7ffbff', transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    core.rotation.x = -Math.PI / 2
    core.position.set(pos.x, pos.y, baseZ + height / 2)
    threeScene.add(core)

    // 外层透明壳：贴螺旋光带纹理；基底压暗不触发泛光，仅光带自身发光
    const shell = new THREE.Mesh(
      new THREE.CylinderGeometry(BEAM_SHELL_RADIUS, BEAM_SHELL_RADIUS, height, 20),
      new THREE.MeshBasicMaterial({ map: storeFlowTexture, transparent: true, opacity: BEAM_SHELL_OPACITY, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    shell.rotation.x = -Math.PI / 2
    shell.position.copy(core.position)
    threeScene.add(shell)

    // 上升粒子（每柱 20~30 个，控制性能）
    const pCount = BEAM_PARTICLE_COUNT
    const pPositions = new Float32Array(pCount * 3)
    const speeds = []
    for (let p = 0; p < pCount; p++) {
      pPositions[p * 3] = pos.x + (Math.random() - 0.5) * (BEAM_SHELL_RADIUS * 1.2)
      pPositions[p * 3 + 1] = pos.y + (Math.random() - 0.5) * (BEAM_SHELL_RADIUS * 1.2)
      pPositions[p * 3 + 2] = baseZ + Math.random() * height
      speeds.push(0.6 + Math.random() * 0.8) // 速度系数 0.6~1.4
    }
    const pGeo = new THREE.BufferGeometry()
    pGeo.setAttribute('position', new THREE.BufferAttribute(pPositions, 3))
    const points = new THREE.Points(pGeo, new THREE.PointsMaterial({
      size: 0.11, map: particleTex, color: warn ? '#ffb0a0' : '#bffcff',
      transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending, depthWrite: false
    }))
    threeScene.add(points)
    particleSystems.push({ points, speeds, baseZ, topZ })

    // 底部雷达扫描环（RingGeometry 默认即在 XY 平面平躺，勿旋转；预警变红）
    const radar = new THREE.Mesh(
      new THREE.RingGeometry(0.34, 0.37, 48),
      new THREE.MeshBasicMaterial({ color: warn ? '#ff8a7a' : '#6ff4ff', transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false })
    )
    radar.position.set(pos.x, pos.y, baseZ + 0.08)
    threeScene.add(radar)
    radarRings.push({ mesh: radar, wave: i * 0.33 })

    // 店铺名标签（仅第 1 层）
    if (labelRenderer) {
      const labelEl = document.createElement('div')
      labelEl.className = 'store-label'
      labelEl.textContent = store.name || ''
      const label = new CSS2DObject(labelEl)
      label.position.set(pos.x, pos.y, topZ + 1.4)
      label.userData = { storeId: store.id }
      threeScene.add(label)
      storeLabels.push(label)
    }

    const d = { storeId: store.id, storeName: store.name, revenue, usageRate: store.usageRate, status: store.status, running: true, warn, kind: 'beam', x: pos.x, y: pos.y, zAnchor: topZ + 1.2 }
    const hitbox = new THREE.Mesh(new THREE.CylinderGeometry(BEAM_SHELL_RADIUS + 0.15, BEAM_SHELL_RADIUS + 0.15, height + 2, 8), new THREE.MeshBasicMaterial({ visible: false }))
    hitbox.rotation.x = -Math.PI / 2
    hitbox.position.copy(core.position)
    hitbox.userData.d = d
    threeScene.add(hitbox)
    activePickMeshes.push(hitbox)
  })

  // ===== 第 2 层：扁平脉冲圈（4~8 名）=====
  const ringRevs = tier2.map(revenueOf)
  const ringMin = Math.min(...ringRevs, 0)
  const ringMax = Math.max(...ringRevs, 1)
  const ringRange = ringMax - ringMin || 1
  tier2.forEach((store, i) => {
    const pos = lonLatToXY(store.longitude, store.latitude)
    const revenue = revenueOf(store)
    const warn = Number(store.usageRate) > 85
    const r = RING_RADIUS_MIN + ((revenue - ringMin) / ringRange) * (RING_RADIUS_MAX - RING_RADIUS_MIN)
    // 水波纹：以点为中心向外扩散的两圈波纹（双圈错开半周期，店铺之间再错相位）
    for (let k = 0; k < 2; k++) {
      const ripple = new THREE.Mesh(
        new THREE.RingGeometry(r - RING_BAND_WIDTH, r, 48),
        // 叠加混合 + 亮青色：细环在 Bloom 下泛光，蓝底地图上更醒目（环面积小，无糊白风险）
        new THREE.MeshBasicMaterial({ color: warn ? '#ff7a6b' : '#7ffbff', transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false })
      )
      // RingGeometry 默认即在 XY 平面平躺，勿旋转
      ripple.position.set(pos.x, pos.y, PLATE_DEPTH + 0.06 + k * 0.02)
      threeScene.add(ripple)
      rippleWaves.push({ mesh: ripple, wave: k * 0.5 + (i % 5) * 0.13 })
    }
    // 波纹中心点：颜色与波纹一致（预警同变红）
    const centerDot = new THREE.Sprite(new THREE.SpriteMaterial({
      map: dotTex, color: warn ? '#ff7a6b' : '#7ffbff', transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending, depthWrite: false
    }))
    centerDot.scale.set(CENTER_DOT_SIZE, CENTER_DOT_SIZE, 1)
    centerDot.position.set(pos.x, pos.y, PLATE_DEPTH + 0.14)
    threeScene.add(centerDot)

    const d = { storeId: store.id, storeName: store.name, revenue, usageRate: store.usageRate, status: store.status, running: true, warn, kind: 'ring', x: pos.x, y: pos.y, zAnchor: PLATE_DEPTH + 0.8 }
    const hitbox = new THREE.Mesh(new THREE.CylinderGeometry(r + 0.18, r + 0.18, 1.2, 12), new THREE.MeshBasicMaterial({ visible: false }))
    hitbox.rotation.x = -Math.PI / 2
    hitbox.position.set(pos.x, pos.y, PLATE_DEPTH + 0.6)
    hitbox.userData.d = d
    threeScene.add(hitbox)
    activePickMeshes.push(hitbox)
  })

  // ===== 第 3 层：微光点（9 名起）=====
  tier3.forEach((store) => {
    const pos = lonLatToXY(store.longitude, store.latitude)
    const warn = Number(store.usageRate) > 85
    const dot = new THREE.Sprite(new THREE.SpriteMaterial({
      map: dotTex, color: warn ? '#ff9a8a' : '#ffffff', transparent: true, opacity: 0.95, depthWrite: false
    }))
    dot.scale.set(DOT_SIZE, DOT_SIZE, 1)
    dot.position.set(pos.x, pos.y, PLATE_DEPTH + 0.25)
    threeScene.add(dot)

    const d = { storeId: store.id, storeName: store.name, revenue: revenueOf(store), usageRate: store.usageRate, status: store.status, running: true, warn, kind: 'dot', x: pos.x, y: pos.y, zAnchor: PLATE_DEPTH + 0.9 }
    const hitbox = new THREE.Mesh(new THREE.SphereGeometry(0.2, 8, 8), new THREE.MeshBasicMaterial({ visible: false }))
    hitbox.position.set(pos.x, pos.y, PLATE_DEPTH + 0.25)
    hitbox.userData.d = d
    threeScene.add(hitbox)
    activePickMeshes.push(hitbox)
  })

  // ===== 停运店铺：幽灵片（贴地深灰圆片，无发光）=====
  stopped.forEach((store) => {
    const pos = lonLatToXY(store.longitude, store.latitude)
    const ghost = new THREE.Mesh(
      new THREE.CircleGeometry(GHOST_RADIUS, 24),
      new THREE.MeshBasicMaterial({ color: '#555555', transparent: true, opacity: 0.4, depthWrite: false })
    )
    // CircleGeometry 默认即在 XY 平面平躺，勿旋转
    ghost.position.set(pos.x, pos.y, PLATE_DEPTH + 0.05)
    ghostGroup.add(ghost)
    // hover 高亮边框（默认隐藏）
    const outline = new THREE.Mesh(
      new THREE.RingGeometry(GHOST_RADIUS, GHOST_RADIUS + 0.035, 32),
      new THREE.MeshBasicMaterial({ color: '#ffd166', transparent: true, opacity: 0.85, depthWrite: false, visible: false })
    )
    outline.position.set(pos.x, pos.y, PLATE_DEPTH + 0.07)
    ghostGroup.add(outline)

    const d = {
      storeId: store.id, storeName: store.name, revenue: revenueOf(store), usageRate: store.usageRate,
      status: store.status, running: false, warn: false, kind: 'ghost', x: pos.x, y: pos.y, zAnchor: PLATE_DEPTH + 0.6,
      updateTime: store.updateTime, ghostMesh: ghost, ghostOutline: outline
    }
    const hitbox = new THREE.Mesh(new THREE.SphereGeometry(GHOST_RADIUS + 0.12, 8, 8), new THREE.MeshBasicMaterial({ visible: false }))
    hitbox.position.set(pos.x, pos.y, PLATE_DEPTH + 0.2)
    hitbox.userData.d = d
    threeScene.add(hitbox)
    ghostPickMeshes.push(hitbox)
  })

  rebuildVisiblePickMeshes()

  // 悬停信息卡（单例，内容随悬停店铺更新；外层 wrap 由 CSS2D 定位，内层做过渡动画）
  if (!storeTooltip) {
    const wrapEl = document.createElement('div')
    wrapEl.className = 'store-card-wrap'
    storeTooltipEl = document.createElement('div')
    storeTooltipEl.className = 'store-card' // 初始 opacity:0，靠 .show 类淡入
    wrapEl.appendChild(storeTooltipEl)
    storeTooltip = new CSS2DObject(wrapEl)
    threeScene.add(storeTooltip)
  }
}

// 说明：原"库存预警警示环"动画已移除——预警状态改为各层级统一变红（全息柱内芯/雷达环变红、脉冲圈红呼吸、微光点红点），
// 由 loadStorePillars 内按 usageRate > 85 直接设置材质颜色实现，无需补间动画

// ===== 省份 Hover 交互 =====
let cachedProvinceColors = null

/**
 * 省份配色（读取 CSS 变量，getComputedStyle 会强制样式计算，必须缓存）：
 * 首次调用读取一次；主题切换时由 applyThemeToThree 失效重建。
 * 注意：setProvinceHover / resetProvinceHover 在 mousemove 高频路径上调用。
 */
function getProvinceColors() {
  if (!cachedProvinceColors) {
    cachedProvinceColors = {
      normalColor: getCssVar('--map-three-province-color') || '#3a75ad',
      normalEmissive: getCssVar('--map-three-province-emissive') || '#1a5588',
      normalEmissiveIntensity: Number(getCssVar('--map-three-emissive-intensity') || '0.8'),
      normalBorderColor: getCssVar('--map-three-province-border') || '#88ccff',
      normalBorderOpacity: 0.9,
      hoverColor: getCssVar('--map-three-hover-color') || '#5cb8ff',
      hoverEmissive: getCssVar('--map-three-hover-emissive') || '#2a9fd6',
      hoverEmissiveIntensity: Number(getCssVar('--map-three-hover-emissive-intensity') || '1.0'),
      hoverBorderColor: getCssVar('--map-three-hover-border') || '#bff0ff',
      hoverBorderOpacity: 1.0,
    }
  }
  return cachedProvinceColors
}

function onMouseMove(event) {
  // 只缓存事件：实际射线检测由渲染循环中的 processPendingPick 每帧至多处理一次，
  // 不再随 mousemove 高频触发（原先每个像素移动都做一次全量省份射线检测）
  pendingMouseEvent = event
}

/** 渲染循环中调用：处理每帧至多一次的鼠标拾取 */
function processPendingPick() {
  if (!pendingMouseEvent) return
  const event = pendingMouseEvent
  pendingMouseEvent = null

  // 位移 <3px 跳过：对高精度省份几何体做射线检测开销大，微小位移无需重查
  if (Math.abs(event.clientX - lastPickClientX) < 3 && Math.abs(event.clientY - lastPickClientY) < 3) return
  lastPickClientX = event.clientX
  lastPickClientY = event.clientY

  // rect 缓存：getBoundingClientRect 会强制布局，仅在 resize 后失效重取
  if (!canvasRect) canvasRect = threeRenderer.domElement.getBoundingClientRect()
  const rect = canvasRect
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  // 从相机发出射线
  raycaster.setFromCamera(mouse, threeCamera)

  // 优先拾取店铺（全息柱/脉冲圈/微光点/幽灵片），命中则显示信息卡
  const storeHits = raycaster.intersectObjects(visiblePickMeshes, false)
  if (storeHits.length > 0) {
    const d = storeHits[0].object.userData.d
    threeRenderer.domElement.style.cursor = 'pointer'
    if (!pinnedStore) {
      // 未固定：信息卡跟随鼠标悬停的店铺
      if (hoveredStore !== d) {
        setStoreHover(hoveredStore, false)
        hoveredStore = d
        setStoreHover(d, true)
        updateLabelVisibility()
      }
      showStoreTooltip(d, false)
    }
    resetProvinceHover()
    hoveredProvince = null
    return
  }
  threeRenderer.domElement.style.cursor = ''
  if (!pinnedStore) {
    // 未固定：移开店铺即隐藏卡片
    if (hoveredStore) {
      setStoreHover(hoveredStore, false)
      hoveredStore = null
      updateLabelVisibility()
    }
    hideStoreTooltip()
  }

  // 省份拾取列表首次使用时预构建一次，之后复用（省份 Group 加载完成后不再变化）
  if (!provincePickMeshes) buildProvincePickList()

  const intersects = raycaster.intersectObjects(provincePickMeshes, false)
  if (intersects.length > 0) {
    // 命中省份 mesh，取其所属的 Group
    const hitMesh = intersects[0].object
    const provinceGroup = hitMesh.parent
    if (provinceGroup && provinceGroup !== hoveredProvince) {
      // 还原上一个高亮
      resetProvinceHover()
      // 高亮当前省份
      setProvinceHover(provinceGroup)
      hoveredProvince = provinceGroup
    }
  } else {
    // 未命中任何省份
    resetProvinceHover()
    hoveredProvince = null
  }
}

/** 预构建省份拾取 mesh 列表（原来每次 mousemove 都重建） */
function buildProvincePickList() {
  const meshes = []
  provinceGroupMap.forEach((group) => {
    group.children.forEach((child) => {
      if (child instanceof THREE.Mesh) meshes.push(child)
    })
  })
  provincePickMeshes = meshes
}

/** 显示店铺悬停信息卡（d 为店铺数据；幽灵片显示停运提示，其余显示利润/库存/营业状态） */
function showStoreTooltip(d, pinned = false) {
  if (!storeTooltip || !storeTooltipEl) return
  const key = `${d.storeId}-${d.kind}-${pinned ? 'p' : 'h'}`
  // 内容未变时跳过 DOM 重建
  if (key !== renderedTooltipKey) {
    renderedTooltipKey = key
    storeTooltipEl.innerHTML = ''
    const title = document.createElement('div')
    title.className = 'sc-title'
    title.textContent = d.storeName || '店铺'
    if (pinned) {
      const pinBadge = document.createElement('span')
      pinBadge.className = 'sc-pin'
      pinBadge.textContent = '已固定'
      title.append(pinBadge)
    }
    storeTooltipEl.append(title)
    if (d.kind === 'ghost') {
      const tip = document.createElement('div')
      tip.className = 'sc-row'
      tip.innerHTML = '<span style="color:#ffd166">该店铺已停运</span>'
      storeTooltipEl.append(tip)
      if (d.updateTime) {
        const row = document.createElement('div')
        row.className = 'sc-row'
        row.innerHTML = `<span>停运时间</span><b>${formatDateTime(d.updateTime, 'YYYY-MM-DD HH:mm')}</b>`
        storeTooltipEl.append(row)
      }
    } else {
      const warn = d.warn
      const row1 = document.createElement('div')
      row1.className = 'sc-row'
      row1.innerHTML = `<span>利润</span><b>¥${(d.revenue ?? 0).toLocaleString()}</b>`
      const row2 = document.createElement('div')
      row2.className = 'sc-row'
      row2.innerHTML = `<span>库存状态</span><b class="${warn ? 'sc-warn' : 'sc-ok'}">${warn ? '库存预警' : '正常'}</b>`
      const row3 = document.createElement('div')
      row3.className = 'sc-row'
      row3.innerHTML = `<span>营业状态</span><b class="${d.running ? 'sc-ok' : 'sc-warn'}">${d.running ? '运营中' : '已停运'}</b>`
      storeTooltipEl.append(row1, row2, row3)
    }
  }
  storeTooltipEl.classList.add('show') // 触发淡入过渡（已显示时重复添加无副作用）
  // 信息卡锚定在店铺上方
  storeTooltip.position.set(d.x, d.y, d.zAnchor)
}

/** 隐藏店铺悬停信息卡（淡出过渡） */
function hideStoreTooltip() {
  if (storeTooltipEl) storeTooltipEl.classList.remove('show')
}

/** 悬停/固定店铺的名字标签隐藏，其余显示（仅第 1 层有标签） */
function updateLabelVisibility() {
  const hoveredId = hoveredStore?.storeId
  const pinnedId = pinnedStore?.storeId
  storeLabels.forEach((label) => {
    const el = label.element
    const sid = label.userData?.storeId
    if (!el || sid == null) return
    el.style.display = (sid === hoveredId || sid === pinnedId) ? 'none' : ''
  })
}

/** 幽灵片 hover 高亮：显示边框 + 提亮本体 */
function setStoreHover(d, on) {
  if (d?.kind !== 'ghost') return
  if (d.ghostOutline) d.ghostOutline.visible = on
  if (d.ghostMesh) d.ghostMesh.material.opacity = on ? 0.7 : 0.4
}

/** 双击店铺：固定/取消固定信息卡；双击空白处：取消固定 */
function onDoubleClick(event) {
  if (!threeRenderer) return
  const rect = threeRenderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouse, threeCamera)
  const hits = raycaster.intersectObjects(visiblePickMeshes, false)
  if (hits.length > 0) {
    const d = hits[0].object.userData.d
    if (pinnedStore === d) {
      // 再次双击同一店铺：取消固定
      pinnedStore = null
      updateLabelVisibility()
      hideStoreTooltip()
    } else {
      // 固定到该店铺
      pinnedStore = d
      updateLabelVisibility()
      showStoreTooltip(d, true)
    }
  } else if (pinnedStore) {
    // 双击空白处：取消固定
    pinnedStore = null
    updateLabelVisibility()
    hideStoreTooltip()
  }
}

function setProvinceHover(group) {
  const colors = getProvinceColors()
  group.children.forEach((child) => {
    if (child instanceof THREE.Mesh && child.material.isMeshPhongMaterial) {
      child.userData._origColor = child.material.color.getHex()
      child.userData._origEmissive = child.material.emissive.getHex()
      child.userData._origEmissiveIntensity = child.material.emissiveIntensity
      child.material.color.set(colors.hoverColor)
      child.material.emissive.set(colors.hoverEmissive)
      child.material.emissiveIntensity = colors.hoverEmissiveIntensity
    }
    if (child instanceof THREE.LineSegments) {
      child.userData._origBorderColor = child.material.color.getHex()
      child.userData._origOpacity = child.material.opacity
      child.material.color.set(colors.hoverBorderColor)
      child.material.opacity = colors.hoverBorderOpacity
    }
  })
}

function resetProvinceHover() {
  if (!hoveredProvince) return
  const colors = getProvinceColors()
  hoveredProvince.children.forEach((child) => {
    if (child instanceof THREE.Mesh && child.material.isMeshPhongMaterial) {
      if (child.userData._origColor !== undefined) {
        child.material.color.setHex(child.userData._origColor)
      }
      if (child.userData._origEmissive !== undefined) {
        child.material.emissive.setHex(child.userData._origEmissive)
      }
      child.material.emissiveIntensity = child.userData._origEmissiveIntensity ?? colors.normalEmissiveIntensity
    }
    if (child instanceof THREE.LineSegments) {
      if (child.userData._origBorderColor !== undefined) {
        child.material.color.setHex(child.userData._origBorderColor)
      }
      if (child.userData._origOpacity !== undefined) {
        child.material.opacity = child.userData._origOpacity
      }
    }
  })
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
  const primary = getCssVar('--map-primary') || '#409eff'
  const success = getCssVar('--map-success') || '#67c23a'
  const textSub = getCssVar('--map-text-sub') || 'rgba(224, 230, 240, 0.45)'
  const text = getCssVar('--map-text') || '#e0e6f0'
  const cardBg = getCssVar('--map-card-bg') || 'rgba(10, 30, 60, 0.9)'
  const cardBorder = getCssVar('--map-card-border') || 'rgba(64,158,255,0.3)'
  const axisLine = getCssVar('--map-table-border') || 'rgba(64,158,255,0.2)'
  trendChart.setOption({
    // 留出坐标轴名称空间（x 轴"月份"、y 轴"金额"）
    grid: { left: 44, right: 24, top: 22, bottom: 0 },
    // 悬停提示：显示月份对应的销售额/利润具体数值
    tooltip: {
      trigger: 'axis',
      backgroundColor: cardBg,
      borderColor: cardBorder,
      textStyle: { color: text, fontSize: 12 }
    },
    xAxis: {
      type: 'category', data: months,
      name: '月份',
      nameTextStyle: { color: textSub, fontSize: 10 },
      axisLine: { lineStyle: { color: axisLine } },
      axisLabel: { color: textSub, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: '金额',
      nameTextStyle: { color: textSub, fontSize: 10 },
      splitLine: { lineStyle: { color: axisLine } },
      axisLabel: { color: textSub, fontSize: 10, formatter: v => v >= 10000 ? (v / 10000).toFixed(0) + 'w' : v }
    },
    series: [
      { name: '销售额', type: 'line', data: amounts, smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { width: 2, color: primary }, itemStyle: { color: primary }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: primary + '59' }, { offset: 1, color: primary + '05' }]) } },
      { name: '利润', type: 'line', data: profits, smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { width: 2, color: success }, itemStyle: { color: success }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: success + '4d' }, { offset: 1, color: success + '05' }]) } }
    ]
  })
}

function updatePieChart() {
  if (!pieChart) return
  // 按销售额降序取前 5，过滤 0 金额分类：
  // 后端返回 product_category 表全部分类（含无销售分类），原逻辑未排序直接截前 5，可能漏掉大额分类
  const data = categorySales.value
    .map(item => ({ name: item.categoryName || item.category, value: Number(item.amount) }))
    .filter(item => item.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
  const textSub = getCssVar('--map-text-sub') || 'rgba(224, 230, 240, 0.45)'
  const text = getCssVar('--map-text') || '#e0e6f0'
  const cardBg = getCssVar('--map-card-bg') || 'rgba(10, 30, 60, 0.9)'
  const cardBorder = getCssVar('--map-card-border') || 'rgba(64,158,255,0.3)'
  const mapBg = getCssVar('--map-bg') || '#061429'
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: cardBg, borderColor: cardBorder, textStyle: { color: text, fontSize: 12 } },
    legend: { bottom: 0, textStyle: { color: textSub, fontSize: 10 }, itemWidth: 8, itemHeight: 8 },
    series: [{ type: 'pie', radius: ['50%', '72%'], center: ['50%', '45%'], label: { color: textSub, fontSize: 10 }, labelLine: { lineStyle: { color: textSub } }, itemStyle: { borderColor: mapBg, borderWidth: 2 }, data }]
  })
}

function updateBarChart() {
  if (!barChart) return
  const primary = getCssVar('--map-primary') || '#409eff'
  const success = getCssVar('--map-success') || '#67c23a'
  const danger = getCssVar('--map-danger') || '#f56c6c'
  const purple = getCssVar('--map-purple') || '#a855f7'
  const warning = getCssVar('--map-warning') || '#e6a23c'
  const colors = [success, primary, danger, purple, warning]
  const textSub = getCssVar('--map-text-sub') || 'rgba(224, 230, 240, 0.45)'
  const text = getCssVar('--map-text') || '#e0e6f0'
  const axisLine = getCssVar('--map-table-border') || 'rgba(64,158,255,0.2)'
  const top5 = productSales.value.slice(0, 5)
  barChart.setOption({
    grid: { left: 72, right: 24, top: 4, bottom: 0 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: axisLine } }, axisLabel: { color: textSub, fontSize: 10 } },
    yAxis: { type: 'category', inverse: true, data: top5.map(p => p.productName || p.name || '-'), axisLabel: { color: text, fontSize: 11 }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', data: top5.map((p, i) => ({ value: Number(p.totalQuantity || p.quantity || 0), itemStyle: { color: colors[i % colors.length], borderRadius: [0, 4, 4, 0] } })),
      barWidth: 12,
      label: { show: true, position: 'right', color: textSub, fontSize: 10, formatter: '{c}' }
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
    // 请求期间已卸载：不再写 ref、不再更新已销毁的图表
    if (unmounted) return
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
  } catch { if (!unmounted) ElMessage.warning('数据加载失败') }
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

// ===== 主题切换适配 =====
let themeObserver = null

function applyThemeToThree() {
  if (!threeScene) return
  cachedProvinceColors = null // 主题变量已切换，失效省份配色缓存
  const bg = getCssVar('--map-three-bg') || '#061429'
  threeScene.background.set(bg)
  if (threeScene.fog) threeScene.fog.color.set(bg)
  if (threeAmbientLight) threeAmbientLight.color.set(getCssVar('--map-three-ambient') || '#3a4d70')
  if (threeDirLight) threeDirLight.color.set(getCssVar('--map-three-dir') || '#7fa5e0')
  if (threeFillLight) threeFillLight.color.set(getCssVar('--map-three-fill') || '#5577aa')
  if (bloomPass) {
    bloomPass.threshold = Number(getCssVar('--map-three-bloom-threshold') || '0.5')
    bloomPass.strength = Number(getCssVar('--map-three-bloom-strength') || '0.4')
  }
  const colors = getProvinceColors()
  provinceGroupMap.forEach((group) => {
    group.children.forEach((child) => {
      if (child instanceof THREE.Mesh && child.material.isMeshPhongMaterial && !(child.userData._origColor !== undefined && hoveredProvince === group)) {
        child.material.color.set(colors.normalColor)
        child.material.emissive.set(colors.normalEmissive)
        child.material.emissiveIntensity = colors.normalEmissiveIntensity
      }
      if (child instanceof THREE.LineSegments && !(child.userData._origBorderColor !== undefined && hoveredProvince === group)) {
        child.material.color.set(colors.normalBorderColor)
        child.material.opacity = colors.normalBorderOpacity
      }
    })
  })
  updateTrendChart()
  updatePieChart()
  updateBarChart()
}

// 主题 class 名称（与 useTheme.ts 的 themeList 保持一致）
const THEME_CLASSES = ['dark-blue', 'dark', 'light']

/** 从 html class 字符串中提取当前主题名（无主题时返回空串） */
function getThemeClass(classStr) {
  return classStr?.split(/\s+/).find((c) => THEME_CLASSES.includes(c)) || ''
}

function setupThemeObserver() {
  themeObserver = new MutationObserver((mutations) => {
    for (const m of mutations) {
      // 仅当主题 class 实际变化才重刷：灰色模式/色弱模式切换等无关 class 变化不再触发全套主题重绘
      if (m.attributeName !== 'class') continue
      if (getThemeClass(m.oldValue) === getThemeClass(m.target.getAttribute('class'))) continue
      nextTick(() => applyThemeToThree())
      break
    }
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeOldValue: true })
}

// ===== 生命周期 =====
// ===== resize 节流 =====
let resizeRafId = null // resize 的 rAF 合并 ID（拖拽窗口时事件每秒几十次，每帧只执行一次）
let lastResizeW = 0    // 上次已同步的画布尺寸（尺寸未变则跳过，避免重复重建 RenderTarget）
let lastResizeH = 0

function handleResize() {
  if (resizeRafId !== null) return // 本帧已有待执行的 resize，合并
  resizeRafId = requestAnimationFrame(() => {
    resizeRafId = null
    if (!threeRenderer || !threeCamera || !threeContainerRef.value) return
    const { clientWidth, clientHeight } = threeContainerRef.value
    if (!clientWidth || !clientHeight) return
    // 尺寸未变跳过：EffectComposer.setSize 会销毁并重建全部 Bloom RenderTarget，代价高
    if (clientWidth === lastResizeW && clientHeight === lastResizeH) return
    lastResizeW = clientWidth
    lastResizeH = clientHeight

    canvasRect = null // 画布尺寸变化，射线检测用的 rect 缓存失效
    threeRenderer.setSize(clientWidth, clientHeight)
    threeCamera.aspect = clientWidth / clientHeight
    threeCamera.updateProjectionMatrix()
    // 同步更新后处理管线
    if (effectComposer) effectComposer.setSize(clientWidth, clientHeight)
    // 同步流光线的 resolution（LineMaterial 线宽按像素计算）
    outlineFlowMaterials.forEach((m) => m.resolution.set(clientWidth, clientHeight))
    // 同步 CSS2D 标签层尺寸
    labelRenderer?.setSize(clientWidth, clientHeight)

    trendChart?.resize()
    pieChart?.resize()
    barChart?.resize()
  })
}

onMounted(async () => {
  if (firstLoad) {
    firstLoad = false
    fetchWeather()
    initThreeScene()
    // 第二步：加载省份 3D 板块 + 国界流光轮廓（并行）
    await Promise.all([loadChinaProvinces(), loadChinaOutline()])
    // 以下每一步 await 后都可能已卸载（用户加载中切走），必须中止后续初始化
    if (unmounted) return
    await nextTick()
    if (unmounted) return
    initCharts()
    await loadAllData()
    if (unmounted) return
    loadStorePillars()         // 第四步：店铺分层渲染（全息柱/脉冲圈/微光点/幽灵片）
    if (unmounted) return
    startAlertScroll()
  }
  window.addEventListener('resize', handleResize)
  setupThemeObserver()
})

// 说明：本页面（/map）不在 AppMain 的 keep-alive 中（独立全屏路由），
// 组件每次进出都完整挂载/卸载，无需 activated/deactivated 钩子

// ===== 资源释放工具 =====
/** 释放单个材质及其引用的纹理（map / emissiveMap 等） */
function disposeMaterial(mat) {
  for (const value of Object.values(mat)) {
    if (value && value.isTexture) value.dispose()
  }
  mat.dispose()
}

/**
 * 递归释放对象及其子树的 GPU 资源（几何体 / 材质 / 纹理）。
 * scene.clear() 只是把对象从场景图摘除，不调用 dispose，显存不会回收；
 * 必须先 dispose，renderer 收到 dispose 事件后才会真正释放 GPU 缓冲。
 */
function disposeObjectResources(root) {
  if (!root) return
  root.traverse((obj) => {
    if (obj.geometry) obj.geometry.dispose()
    if (Array.isArray(obj.material)) {
      obj.material.forEach(disposeMaterial)
    } else if (obj.material) {
      disposeMaterial(obj.material)
    }
  })
}

onBeforeUnmount(() => {
  unmounted = true // 先置标志：在途异步初始化链路检查到后立即中止
  weatherAbort?.abort() // 取消在途天气请求
  window.removeEventListener('resize', handleResize)
  if (resizeRafId !== null) { cancelAnimationFrame(resizeRafId); resizeRafId = null } // 取消待执行的 resize 合并回调
  if (themeObserver) { themeObserver.disconnect(); themeObserver = null }
  // 移除 mousemove / dblclick 监听
  if (threeRenderer?.domElement) {
    threeRenderer.domElement.removeEventListener('mousemove', onMouseMove)
    threeRenderer.domElement.removeEventListener('dblclick', onDoubleClick)
  }
  stopAlertScroll()
  trendChart?.dispose(); pieChart?.dispose(); barChart?.dispose()
  // 清理 Three.js 资源
  if (threeAnimationId !== null) cancelAnimationFrame(threeAnimationId)
  if (threeControls) { threeControls.dispose(); threeControls = null }
  if (effectComposer) { effectComposer.dispose(); effectComposer = null }
  // 清理国界流光资源
  if (outlineFlowGroup) {
    outlineFlowGroup.traverse((o) => { o.geometry?.dispose(); o.material?.dispose() })
    outlineFlowGroup = null
  }
  outlineFlowMaterials.length = 0
  cometFlowUniforms.length = 0
  // 清理店铺标签/信息卡（CSS2D）
  if (labelRenderer) {
    labelRenderer.domElement.remove()
    labelRenderer = null
  }
  activePickMeshes.length = 0
  ghostPickMeshes.length = 0
  visiblePickMeshes = []
  storeLabels.length = 0
  particleSystems.length = 0
  radarRings.length = 0
  rippleWaves.length = 0
  ghostGroup.clear()
  provincePickMeshes = null
  pendingMouseEvent = null
  canvasRect = null
  // 省份标签的网格/材质/纹理由下方 disposeObjectResources 统一释放
  provinceLabels.length = 0
  storeTooltip = null
  storeTooltipEl = null
  renderedTooltipKey = null
  storeFlowTexture = null
  hoveredStore = null
  pinnedStore = null
  bloomPass = null; raycaster = null; hoveredProvince = null
  // 完整释放场景 GPU 资源（省份板块/柱体/拾取盒/标签/警示环等全部几何体与材质），
  // 必须在 renderer.dispose() 之前执行，dispose 事件才会被 renderer 处理、显存才会回收
  if (threeScene) {
    disposeObjectResources(threeScene)
    threeScene.clear()
    threeScene = null
  }
  // provinceGroupMap 持有全部省份 Group 引用，必须清空
  provinceGroupMap.clear()
  if (threeRenderer) { threeRenderer.dispose(); threeRenderer = null }
})
</script>


<style lang="scss" scoped>
.dashboard {
  width: 100vw; height: 100vh;
  background: var(--map-bg);
  background-image: radial-gradient(ellipse at 50% 0%, var(--map-bg-glow1) 0%, transparent 60%), radial-gradient(ellipse at 80% 20%, var(--map-bg-glow2) 0%, transparent 50%);
  display: flex; flex-direction: column; overflow: hidden;
  font-family: 'Microsoft YaHei', sans-serif;
  :deep(.el-button--primary.is-plain) {
    --el-button-bg-color: var(--map-btn-bg); --el-button-border-color: var(--map-btn-border); --el-button-text-color: var(--map-btn-text);
    --el-button-hover-bg-color: var(--map-btn-hover-bg); --el-button-hover-border-color: var(--map-btn-hover-border); --el-button-hover-text-color: var(--map-btn-hover-text);
  }
  .logout-btn { color: var(--map-header-text);}
}

// Header
.dash-header {
  height: 56px; position: relative; z-index: 20; flex-shrink: 0;
  .header-bg { position: absolute; inset: 0; background: url('/UI/0847.png') no-repeat center bottom; background-size: 100% 100%; opacity: 0.75; }
  .header-content { position: absolute; inset: 0; display: flex; align-items: flex-end; padding: 0 24px 8px; }
  // .header-left 及 .hl-* 样式已随时钟一起拆入 HeaderClock.vue
  .header-center { position: absolute; left: 50%; transform: translateX(-50%); bottom: 8px;
    .dash-title { font-size: 26px; font-weight: 500; letter-spacing: 4px; color: var(--map-title-text); text-shadow: 0 0 20px var(--map-title-glow); }
  }
  .header-right { display: flex; align-items: flex-end; gap: 2px; margin-left: auto;margin-bottom: -10px;}
}

// Body
.dash-body { flex: 1; display: grid; grid-template-columns: 1fr 2.2fr 1fr; gap: 10px; padding: 10px; overflow: hidden; min-height: 0; }
.dash-left, .dash-right { display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.dash-center { display: flex; flex-direction: column; gap: 8px; overflow: hidden; }

// Card
.dash-card {
  background: var(--map-card-bg); backdrop-filter: blur(12px); border: 1px solid var(--map-card-border);
  border-radius: 8px; padding: 10px 12px; box-shadow: var(--map-card-shadow);
  .card-title { font-size: 13px; font-weight: 500; color: var(--map-card-title); margin-bottom: 8px; display: flex; align-items: center; gap: 8px;
    .title-icon { display: inline-block; width: 3px; height: 14px; background: linear-gradient(180deg, var(--map-primary), var(--map-success)); border-radius: 2px; }
    .title-icon.alert { background: linear-gradient(180deg, var(--map-warning), var(--map-danger)); }
    .title-icon.log { background: linear-gradient(180deg, var(--map-info), var(--map-success)); }
  }
}
.card-panel { display: flex; flex-direction: column; overflow: hidden; }
.card-overview { flex-shrink: 0; }
.flex-2 { flex: 2; min-height: 0; }
.flex-3 { flex: 3; min-height: 0; }

// Overview
.card-overview .overview-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  .ov-item { background: var(--map-item-bg); border-radius: 6px; padding: 10px; text-align: center;
    .ov-val { display: block; font-size: 22px; font-weight: 600; color: var(--map-primary);
      &.green { color: var(--map-success); } &.blue { color: var(--map-primary); }
    }
    .ov-lbl { font-size: 11px; color: var(--map-text-sub); margin-top: 4px; display: block; }
  }
}

// Chart box
.chart-box { width: 100%; flex: 1; min-height: 0; }

// Rank table
.table-wrap {
  flex: 1; overflow: auto; min-height: 0;
  // 细窄主题色滚动条：默认灰色滚动条与大屏风格格格不入
  &::-webkit-scrollbar { width: 4px; height: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--map-table-border); border-radius: 2px; }
  &::-webkit-scrollbar-thumb:hover { background: var(--map-primary); }
  &::-webkit-scrollbar-track { background: transparent; }
  scrollbar-width: thin; /* Firefox */
  scrollbar-color: var(--map-table-border) transparent;
}
.rank-table { width: 100%; border-collapse: collapse; font-size: 11px;
  thead tr { border-bottom: 1px solid var(--map-table-border); }
  th { padding: 4px 4px; color: var(--map-text-sub); font-weight: 400; text-align: left; white-space: nowrap; }
  td { padding: 3px 4px; color: var(--map-text); }
  .td-name { max-width: 70px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-prod { max-width: 60px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--map-text-sub); font-size: 10px; }
  .td-profit { color: var(--map-success); font-weight: 500; }
  .td-yoy { font-size: 10px;
    &.up { color: var(--map-danger); }
    &.down { color: var(--map-success); }
  }
  .rk-num { display: inline-block; width: 18px; height: 18px; border-radius: 3px; background: var(--map-rank-bg); text-align: center; line-height: 18px; font-size: 10px; font-weight: 600; color: var(--map-rank-text);
    &.rk-1 { background: var(--map-danger); color: #fff; }
    &.rk-2 { background: var(--map-warning); color: #fff; }
    &.rk-3 { background: var(--map-primary); color: #fff; }
  }
}

// Indicators
.indicator-row { display: flex; gap: 10px; flex-shrink: 0;
  .ind-card { flex: 1; background: var(--map-ind-bg); border: 1px solid var(--map-ind-border); border-radius: 8px; padding: 8px 12px; text-align: center;
    .ind-val { display: block; font-size: 24px; font-weight: 700; color: var(--map-primary);
      &.danger { color: var(--map-danger); }
    }
    .ind-lbl { font-size: 11px; color: var(--map-text-sub); margin-top: 2px; display: block; }
  }
}

// Map
.map-toolbar {
  display: flex; align-items: center; justify-content: flex-end; gap: 8px;
  flex-shrink: 0;
  .mt-label { font-size: 12px; color: var(--map-text-sub); }
}
.map-wrapper { flex: 1; min-height: 0; position: relative; border-radius: 8px; border: 1px solid var(--map-map-border); overflow: hidden; background: var(--map-three-bg);
  .three-container { width: 100%; height: 100%; transform: translateY(-10px); }
  // 图例（左下角浮层）
  .map-legend {
    position: absolute; left: 12px; bottom: 12px; z-index: 5;
    display: flex; flex-direction: column; gap: 6px;
    padding: 8px 12px; border-radius: 6px;
    background: rgba(6, 20, 41, 0.55); border: 1px solid var(--map-card-border);
    font-size: 11px; color: var(--map-text-sub); pointer-events: none;
    .lg-item { display: flex; align-items: center; gap: 8px; }
    .lg-icon { display: inline-block; flex-shrink: 0; }
    .lg-beam { width: 4px; height: 14px; border-radius: 2px; background: linear-gradient(180deg, #7ffbff, #0a3d70); box-shadow: 0 0 6px rgba(110, 240, 255, 0.8); }
    .lg-ring { width: 10px; height: 10px; border-radius: 50%; border: 2px solid #4fc8ff; background: transparent; }
    .lg-dot { width: 7px; height: 7px; border-radius: 50%; background: #8fb8d6; opacity: 0.8; }
    .lg-ghost { width: 9px; height: 9px; border-radius: 50%; background: #555555; opacity: 0.6; }
  }
}
.map-loading { position: absolute; inset: 0; z-index: 10; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--map-loading-bg); color: var(--map-loading-text);
  p { margin-top: 12px; font-size: 14px; }
}
.loader-ring { width: 40px; height: 40px; border: 3px solid var(--map-loader-border); border-top-color: var(--map-primary); border-radius: 50%; animation: dash-spin 1s linear infinite; }
@keyframes dash-spin { to { transform: rotate(360deg) } }

// Bottom stats
.bottom-stats { display: flex; gap: 10px; flex-shrink: 0;
  .stat-item { flex: 1; background: var(--map-stat-bg); border: 1px solid var(--map-stat-border); border-radius: 6px; padding: 6px 12px; text-align: center;
    .stat-label { font-size: 11px; color: var(--map-text-sub); display: block; }
    .stat-value { font-size: 14px; font-weight: 600; color: var(--map-text); margin-top: 2px; display: block;
      &.up { color: var(--map-success); }
    }
  }
}

// Alert table
.alert-table-wrap { flex: 1; overflow: auto; min-height: 0;
  &::-webkit-scrollbar { display: none; }
  -ms-overflow-style: none; scrollbar-width: none;
}
.alert-table { width: 100%; border-collapse: collapse; font-size: 10px;
  thead tr { border-bottom: 1px solid var(--map-alert-border); }
  th { padding: 2px 3px; color: var(--map-text-sub); font-weight: 400; text-align: left; white-space: nowrap; }
  td { padding: 2px 3px; color: var(--map-text); }
  .td-prod { max-width: 55px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-qty { color: var(--map-danger); font-weight: 500; }
  .alert-tag { display: inline-block; padding: 1px 4px; border-radius: 2px; font-size: 9px;
    &.tag-urgent { background: color-mix(in srgb, var(--map-danger) 20%, transparent); color: var(--map-danger); }
    &.tag-warn { background: color-mix(in srgb, var(--map-warning) 20%, transparent); color: var(--map-warning); }
  }
}
</style>

<style>
.store-label {
  color: var(--map-label-color);
  font-size: 11px;
  white-space: nowrap;
  text-shadow: var(--map-label-shadow);
  pointer-events: none;
}
.store-card-wrap {
  pointer-events: none;
  margin-top: -46px;
}
.store-card {
  min-width: 140px;
  padding: 8px 10px;
  background: var(--map-tooltip-bg);
  border: 1px solid var(--map-tooltip-border);
  border-radius: 6px;
  box-shadow: var(--map-tooltip-shadow);
  color: var(--map-tooltip-text);
  font-size: 12px;
  pointer-events: none;
  opacity: 0;
  transform: translateY(10px) scale(0.9);
  transform-origin: bottom center;
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.store-card.show {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.store-card .sc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--map-tooltip-title);
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--map-tooltip-border-bottom);
  white-space: nowrap;
}
.store-card .sc-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 20px;
  color: var(--map-tooltip-row-text);
  white-space: nowrap;
}
.store-card .sc-row b { color: var(--map-tooltip-row-value); font-weight: 600; }
.store-card .sc-warn { color: var(--map-danger) !important; }
.store-card .sc-ok { color: var(--map-success) !important; }
.store-card .sc-pin {
  float: right;
  font-size: 10px;
  font-weight: 400;
  color: var(--map-tooltip-pin-color);
  border: 1px solid var(--map-tooltip-pin-border);
  border-radius: 3px;
  padding: 0 4px;
  line-height: 16px;
}
</style>