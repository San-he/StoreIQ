<script setup>
/**
 * StoreIQ 地图首页 — Cesium 地球 + 左侧销售面板 + 右侧店铺表格
 * 启用了 KeepAlive 缓存，离开页面不销毁，回来秒开
 */
defineOptions({ name: 'MapView' })
import { ref, watch, onMounted, onBeforeUnmount, onActivated, onDeactivated } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreMapApi, getStoreListApi } from '@@/apis/storeiq/stores'
import { ElMessage } from 'element-plus'
import SalesPanel from './components/SalesPanel.vue'
import StoreTable from './components/StoreTable.vue'
import StoreDetailDialog from './components/StoreDetailDialog.vue'
import StoreEditDialog from './components/StoreEditDialog.vue'

const router = useRouter()

// ===== 数据 =====
const storeList = ref([])           // 全量地图标记
const tableData = ref([])           // 表格分页数据
const tableTotal = ref(0)           // 表格总条数
const tableLoading = ref(false)     // 表格加载状态
const tablePage = ref(1)            // 当前页码
const tablePageSize = ref(10)       // 每页条数
const selectedStore = ref(null)
const cesiumReady = ref(false)
const storeLoading = ref(false)

// 多选过滤：地图上显示的店铺 ID 集合（默认加载后全选）
const checkedStoreIds = ref([])

// 弹窗
const detailVisible = ref(false)
const editVisible = ref(false)
const currentStore = ref(null)

let viewer = null
// storeId → Cesium.Entity 的映射
const storeEntityMap = new Map()
// 当前鼠标悬停的店铺 ID（用于浮动标签）
let hoveredStoreId = null

// ===== Cesium 初始化 =====
function initScene() {
  // 禁用 Cesium Ion 默认底图（避免 401 报错）
  // — imageryProvider: false 阻止 CesiumWidget 内部调用 createWorldImagery
  // — terrainProvider 设为 EllipsoidTerrainProvider 替代默认 Ion 地形
  const t0 = performance.now()
  viewer = new Cesium.Viewer('cesiumContainer', {
    animation: false,
    fullscreenButton: false,
    vrButton: false,
    scene3DOnly: false,
    shouldAnimate: true,
    sceneMode: Cesium.SceneMode.SCENE3D,
    selectionIndicator: false,
    baseLayerPicker: false,
    timeline: false,
    sceneModePicker: true,
    homeButton: false,
    geocoder: false,
    infoBox: false,
    navigationHelpButton: false,
    baseLayer: false,
    imageryProvider: false,
    terrainProvider: new Cesium.EllipsoidTerrainProvider()
  })
  viewer.cesiumWidget.creditContainer.style.display = 'none'

  // 底图
  viewer.imageryLayers.addImageryProvider(
    new Cesium.UrlTemplateImageryProvider({
      url: '/map/{z}/{x}/{y}.jpg',
      credit: new Cesium.Credit(''),
      maximumLevel: 5
    })
  )
  trySwapToOnlineImagery()

  viewer.scene.skyAtmosphere = new Cesium.SkyAtmosphere()
  viewer.scene.globe.baseColor = Cesium.Color.fromCssColorString('#1a3a5c')
  viewer.scene.backgroundColor = Cesium.Color.fromCssColorString('#0a1628')
  viewer.scene.globe.enableLighting = false
  viewer.shadows = false

  try {
    viewer.scene.skyBox = new Cesium.SkyBox({
      sources: {
        positiveX: '/data/images/SkyBox/00h+00.jpg', negativeX: '/data/images/SkyBox/12h+00.jpg',
        positiveY: '/data/images/SkyBox/06h+00.jpg', negativeY: '/data/images/SkyBox/18h+00.jpg',
        positiveZ: '/data/images/SkyBox/06h+90.jpg', negativeZ: '/data/images/SkyBox/06h-90.jpg'
      }
    })
  } catch { /* */ }

  // 点击店铺 → 选中
  const clickHandler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
  clickHandler.setInputAction((click) => {
    const picked = viewer.scene.pick(click.position)
    if (Cesium.defined(picked) && picked.id && picked.id.id?.startsWith('store-')) {
      const sid = parseInt(picked.id.id.replace('store-', ''))
      const store = storeList.value.find(s => s.id === sid)
      if (store) selectedStore.value = store
    }
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)

  // 鼠标悬停 → 显示标签
  setupHoverHandler()

  viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(108, 15, 20000000),
    orientation: { pitch: Cesium.Math.toRadians(-87), heading: 0, roll: 0 }
  })
  viewer.sceneModePicker.viewModel.duration = 0
  cesiumReady.value = true
  console.log('[map] Cesium Viewer 创建耗时:', (performance.now() - t0).toFixed(0) + 'ms')
}

async function trySwapToOnlineImagery() {
  try {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 2000)
    const res = await fetch('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/1/0/0', { signal: controller.signal })
    clearTimeout(timeoutId)
    if (!res.ok || !viewer) return
    const layers = viewer.imageryLayers
    const base = layers.get(0)
    if (base) layers.remove(base)
    layers.addImageryProvider(new Cesium.UrlTemplateImageryProvider({
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      tilingScheme: new Cesium.WebMercatorTilingScheme(), credit: new Cesium.Credit(''), maximumLevel: 18
    }), 0)
  } catch { /* */ }
}

// ===== 鼠标悬停：显示/隐藏标签（节流 100ms，减少 Cesium pick 开销）=====
function setupHoverHandler() {
  const canvas = viewer.scene.canvas
  const handler = new Cesium.ScreenSpaceEventHandler(canvas)
  const DISTANCE_LABEL_VISIBLE = 3000000
  let lastHoverTime = 0

  handler.setInputAction((move) => {
    const now = performance.now()
    if (now - lastHoverTime < 80) return
    lastHoverTime = now

    const picked = viewer.scene.pick(move.endPosition)
    if (hoveredStoreId && storeEntityMap.has(hoveredStoreId)) {
      const prev = storeEntityMap.get(hoveredStoreId)
      prev.label.distanceDisplayCondition = new Cesium.DistanceDisplayCondition(0, DISTANCE_LABEL_VISIBLE)
      hoveredStoreId = null
    }
    if (Cesium.defined(picked) && picked.id && picked.id.id?.startsWith('store-')) {
      const sid = parseInt(picked.id.id.replace('store-', ''))
      if (storeEntityMap.has(sid)) {
        const entity = storeEntityMap.get(sid)
        entity.label.distanceDisplayCondition = new Cesium.DistanceDisplayCondition(0, Infinity)
        hoveredStoreId = sid
      }
    }
    canvas.style.cursor = hoveredStoreId ? 'pointer' : 'default'
  }, Cesium.ScreenSpaceEventType.MOUSE_MOVE)
}

// ===== 店铺标记 =====
async function loadStoreMarkers() {
  storeLoading.value = true
  try {
    const t0 = performance.now()
    const { data } = await getStoreMapApi()
    console.log('[map] /stores/map 接口耗时:', (performance.now() - t0).toFixed(0) + 'ms')
    storeList.value = data

    // 默认全选
    if (checkedStoreIds.value.length === 0) {
      checkedStoreIds.value = data.map(s => s.id)
    }

    // 清除旧标记
    storeEntityMap.forEach(e => viewer.entities.remove(e))
    storeEntityMap.clear()
    hoveredStoreId = null

    const checkedSet = new Set(checkedStoreIds.value)

    data.forEach(store => {
      if (!store.longitude || !store.latitude) return
      const rate = store.usageRate || 0
      let color = Cesium.Color.fromCssColorString('#67c23a')
      if (rate > 85) color = Cesium.Color.fromCssColorString('#f56c6c')
      else if (rate > 60) color = Cesium.Color.fromCssColorString('#e6a23c')

      const entity = viewer.entities.add({
        id: `store-${store.id}`,
        show: checkedSet.has(store.id),
        position: Cesium.Cartesian3.fromDegrees(store.longitude, store.latitude),
        point: {
          pixelSize: 14,
          color,
          outlineColor: Cesium.Color.WHITE,
          outlineWidth: 2,
          heightReference: Cesium.HeightReference.CLAMP_TO_GROUND
        },
        label: {
          text: store.name,
          font: '16pt sans-serif ',
          style: Cesium.LabelStyle.FILL_AND_OUTLINE,
          outlineWidth: 3,
          fillColor: Cesium.Color.WHITE,
          outlineColor: Cesium.Color.fromCssColorString('#1a3a5c'),
          verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          pixelOffset: new Cesium.Cartesian2(0, -16),
          scale: 0.75,
          // 5,000km 内自动显示标签
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 3000000)
        }
      })
      storeEntityMap.set(store.id, entity)
    })
    syncMarkerVisibility()
  } catch { ElMessage.error('加载店铺数据失败') } finally {
    storeLoading.value = false
  }
}

// 根据勾选状态刷新标记显隐（不重新拉数据）
function syncMarkerVisibility() {
  const checkedSet = new Set(checkedStoreIds.value)
  storeEntityMap.forEach((entity, storeId) => {
    entity.show = checkedSet.has(storeId)
  })
  viewer?.scene?.requestRender()
}

// ===== 表格后端分页 =====
async function loadTableData() {
  tableLoading.value = true
  try {
    const t0 = performance.now()
    const res = await getStoreListApi({
      pageNum: tablePage.value,
      pageSize: tablePageSize.value
    })
    console.log('[map] /stores/list 接口耗时:', (performance.now() - t0).toFixed(0) + 'ms')
    // 后端返回 { code: 200, data: [...records], total: 41 }
    tableData.value = res?.data || []
    tableTotal.value = res?.total || 0
  } catch {
    tableData.value = []
    tableTotal.value = 0
  } finally {
    tableLoading.value = false
  }
}

function handleTablePageChange({ page, pageSize }) {
  tablePage.value = page
  tablePageSize.value = pageSize
  loadTableData()
}

function handleTableSizeChange({ page, pageSize }) {
  tablePage.value = page
  tablePageSize.value = pageSize
  loadTableData()
}

async function refreshStores() {
  storeLoading.value = true
  try {
    await Promise.all([loadStoreMarkers(), loadTableData()])
    ElMessage({ message: '刷新成功', type: 'success', customClass: 'msg-at-bottom' })
  } finally {
    storeLoading.value = false
  }
}

function flyToStore(store) {
  if (!store.longitude || !store.latitude) return
  viewer.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(store.longitude, store.latitude, 8000000),
    orientation: { pitch: Cesium.Math.toRadians(-85), heading: 0, roll: 0 }, duration: 1.5
  })
  selectedStore.value = store
}

// ===== 表格事件 =====
function handleTableSelect(store) {
  flyToStore(store)
}

function handleDetail(store) {
  currentStore.value = store
  detailVisible.value = true
}

function handleEdit(store) {
  currentStore.value = store
  editVisible.value = true
}

async function handleSaved() {
  await loadStoreMarkers()
}

// ===== 多选过滤：监听 checkedStoreIds 变化，自动同步地图显隐 =====
watch(checkedStoreIds, () => {
  syncMarkerVisibility()
}, { deep: true })

// 左侧面板：显示全部
function handleShowAll() {
  selectedStore.value = null
}

const goToDashboard = () => router.push('/store/list')

// ===== 生命周期 =====
// 是否首次加载（KeepAlive 缓存后不再重新初始化 Cesium）
let firstLoad = true

onMounted(async () => {
  if (firstLoad) {
    firstLoad = false
    console.time('[map] 总加载耗时')
    console.time('[map] Cesium 初始化')
    initScene()
    console.timeEnd('[map] Cesium 初始化')
    console.time('[map] 数据接口')
    await Promise.all([loadStoreMarkers(), loadTableData()])
    console.timeEnd('[map] 数据接口')
    console.timeEnd('[map] 总加载耗时')
  }
  console.log('[map] onMounted 触发 (firstLoad=' + firstLoad + ', KeepAlive=' + (firstLoad ? '首次' : '缓存命中') + ')')
})

// KeepAlive 隐藏时暂停动画渲染，避免后台消耗 GPU
onDeactivated(() => {
  if (viewer && !viewer.isDestroyed()) {
    viewer.shouldAnimate = false
  }
})

// KeepAlive 恢复时重新渲染
onActivated(() => {
  console.log('[map] onActivated — KeepAlive 缓存恢复，无需重新初始化')
  if (viewer && !viewer.isDestroyed()) {
    viewer.shouldAnimate = true
    viewer.scene.requestRender()
  }
})

// 组件真正销毁时（关闭标签页）才释放 Cesium
onBeforeUnmount(() => {
  if (viewer && !viewer.isDestroyed()) {
    viewer.destroy()
    viewer = null
  }
})
</script>

<template>
  <div class="map-page">
    <!-- ====== 蓝曲线顶栏 ====== -->
    <div class="top-bar">
      <div class="top-bar-bg"></div>
      <div class="top-bar-content">
        <span class="top-title">数码智能管理系统</span>
      </div>
    </div>

    <!-- ====== Cesium 地球 ====== -->
    <div class="cesium-wrapper">
      <div v-if="!cesiumReady" class="cesium-loading">
        <span class="loader-spin"></span>
        <p>正在加载 3D 地球...</p>
      </div>
      <div id="cesiumContainer"></div>
    </div>

    <!-- ====== 左侧销售数据面板 ====== -->
    <div class="float-left">
      <SalesPanel :selected-store="selectedStore" @show-all="handleShowAll" />
    </div>

    <!-- ====== 右侧数据面板 ====== -->
    <div class="float-right">
      <div class="panel-header">
        <div class="stat-row">
          <span class="stat-num">{{ storeList.length }}</span><span class="stat-lbl">店铺</span>
          <span class="stat-num" style="margin-left:16px">{{ storeList.reduce((s,i) => s+(i.used||0), 0) }}</span><span class="stat-lbl">库存件</span>
        </div>
        <div class="header-actions">
          <el-button size="small" text @click="refreshStores" :loading="storeLoading">刷新</el-button>
          <el-button type="primary" size="small" round @click="goToDashboard">管理后台</el-button>
        </div>
      </div>
      <!-- 图例 -->
      <div class="legend-row">
        <span><i class="dot" style="background:#67c23a"></i>充足</span>
        <span><i class="dot" style="background:#e6a23c"></i>预警</span>
        <span><i class="dot" style="background:#f56c6c"></i>满仓</span>
      </div>
      <!-- 表格 -->
      <StoreTable
        :store-list="tableData"
        :all-store-ids="storeList.map(s => s.id)"
        :total="tableTotal"
        :loading="tableLoading"
        :selected-store="selectedStore"
        v-model:checked-ids="checkedStoreIds"
        @select="handleTableSelect"
        @detail="handleDetail"
        @edit="handleEdit"
        @page-change="handleTablePageChange"
        @size-change="handleTableSizeChange"
      />
    </div>

    <!-- ====== 弹窗 ====== -->
    <StoreDetailDialog
      :visible="detailVisible"
      :store="currentStore"
      @close="detailVisible = false"
    />
    <StoreEditDialog
      :visible="editVisible"
      :store="currentStore"
      @close="editVisible = false"
      @saved="handleSaved"
    />
  </div>
</template>

<style lang="scss" scoped>
.map-page {
  width:100vw;
  height:100vh;
  position:relative;
  overflow:hidden;
  background:#0a1628;
}

// ====== 蓝曲线顶栏 ======
.top-bar {
  position: absolute; top: 0; left: 0; right: 0; height: 56px; z-index: 15;
  .top-bar-bg {
    width: 100%; height: 100%;
    background: url('/UI/0847.png') no-repeat center bottom;
    background-size: 100% 100%;
    opacity: 0.75;
  }
  .top-bar-content {
    position: absolute; top: 10px; left: 0; right: 0;
    display: flex; justify-content: center; padding-top: 6px;
    .top-title {
      font-size: 26px; font-weight: 500; letter-spacing: 4px;
      color: rgba(252, 253, 254, 0.92);
      text-shadow: 0 0 20px rgba(64,158,255,0.6);
      font-family: 'Microsoft YaHei', sans-serif;
    }
  }
}

// Cesium
.cesium-wrapper {
  position:absolute;
  inset:0; z-index:0;

  #cesiumContainer{
    width:100%;
    height:100%
  }
}

.cesium-loading {
  position:absolute;
  inset:0; z-index:10;
  display:flex;
  flex-direction:column;
  align-items:center;
  justify-content:center;
  background:#0a1628;
  color:#c9d1d9;

  p{
    margin-top:12px;
    font-size:14px
  }
}
.loader-spin {
  width:40px;
  height:40px;
  border:3px solid rgba(64,158,255,.2);
  border-top-color:#409eff;
  border-radius:50%;
  animation:spin 1s linear infinite
}

@keyframes spin {
  to{
    transform:rotate(360deg)
  }
}

// ====== 左侧销售面板 ======
.float-left {
  position: absolute;
  left: 16px;
  top: 76px;
  width: 22vw;
  min-height: 420px;
  max-height: calc(100vh - 92px);
  z-index: 10;
  background: rgba(14, 38, 74, 0.52);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(64,158,255,0.2);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 32px rgba(0,0,0,0.45);
}

// ====== 右侧数据面板 ======
.float-right {
  position: absolute;
  right: 16px;
  top: 76px;
  width: 24vw;
  min-height: 500px;
  max-height: calc(100vh - 92px);
  z-index: 10;
  background: rgba(14, 38, 74, 0.42);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(64,158,255,0.2);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 32px rgba(0,0,0,0.45);
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(64,158,255,0.12);
    .stat-row {
      display:flex;
      align-items:baseline;
      gap:2px;
      .stat-num{
        font-size:18px;
        font-weight:700;
        color:#409eff
      }
      .stat-lbl{
        font-size:11px;
        color:rgba(224,230,240,.5);
        margin-right:4px
      }
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
  .legend-row {
    display:flex;
    gap:14px;
    padding:6px 14px;
    font-size:11px;
    color:rgba(224,230,240,.45);
    .dot{
      display:inline-block;
      width:7px;
      height:7px;
      border-radius:50%;
      margin-right:4px
    }
  }
}
</style>

<style lang="scss">
.msg-at-bottom {
  top: auto !important;
  bottom: 24px !important;
}
</style>
