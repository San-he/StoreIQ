<script setup>
/**
 * StoreIQ 地图首页 — 蓝曲线顶栏 + 左AI洞察 + 右数据面板
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreMapApi } from '@@/apis/storeiq/stores'
import { getAutoHotProductsApi, getAutoRestockApi } from '@@/apis/storeiq/ai'
import { ElMessage } from 'element-plus'

const router = useRouter()

// ===== 数据 =====
const storeList = ref([])
const selectedStore = ref(null)
const cesiumReady = ref(false)
const aiHotText = ref('')
const aiRestockText = ref('')
let viewer = null
let storeEntities = []

// ===== Cesium 初始化 =====
function initScene() {
  viewer = new Cesium.Viewer('cesiumContainer', {
    animation: false, fullscreenButton: false, vrButton: false, scene3DOnly: false,
    shouldAnimate: true, sceneMode: Cesium.SceneMode.SCENE3D,
    selectionIndicator: false, baseLayerPicker: false, timeline: false,
    sceneModePicker: true, homeButton: false, geocoder: false, infoBox: false,
    navigationHelpButton: false, baseLayer: false,
    terrainProvider: new Cesium.EllipsoidTerrainProvider()
  })
  viewer.cesiumWidget.creditContainer.style.display = 'none'

  // 底图
  viewer.imageryLayers.addImageryProvider(
    new Cesium.UrlTemplateImageryProvider({ url: '/map/{z}/{x}/{y}.jpg', credit: new Cesium.Credit(''), maximumLevel: 5 })
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

  viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(108, 34, 7800000),
    orientation: { pitch: Cesium.Math.toRadians(-70), heading: 0, roll: 0 }
  })
  viewer.sceneModePicker.viewModel.duration = 0
  cesiumReady.value = true
}

async function trySwapToOnlineImagery() {
  try {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 5000)
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

// ===== 店铺标记 =====
async function loadStoreMarkers() {
  try {
    const { data } = await getStoreMapApi()
    storeList.value = data
    storeEntities.forEach(e => viewer.entities.remove(e))
    storeEntities = []
    data.forEach(store => {
      if (!store.longitude || !store.latitude) return
      const rate = store.usageRate || 0
      let color = Cesium.Color.fromCssColorString('#67c23a')
      if (rate > 85) color = Cesium.Color.fromCssColorString('#f56c6c')
      else if (rate > 60) color = Cesium.Color.fromCssColorString('#e6a23c')
      const entity = viewer.entities.add({
        id: `store-${store.id}`,
        position: Cesium.Cartesian3.fromDegrees(store.longitude, store.latitude),
        point: { pixelSize: 14, color, outlineColor: Cesium.Color.WHITE, outlineWidth: 2, heightReference: Cesium.HeightReference.CLAMP_TO_GROUND },
        label: { text: store.name, font: '12pt sans-serif', style: Cesium.LabelStyle.FILL_AND_OUTLINE, outlineWidth: 3, fillColor: Cesium.Color.WHITE, outlineColor: Cesium.Color.fromCssColorString('#1a3a5c'), verticalOrigin: Cesium.VerticalOrigin.BOTTOM, pixelOffset: new Cesium.Cartesian2(0, -16), scale: 0.75 },
        properties: { id: store.id, name: store.name, region: store.region, used: store.used, capacity: store.capacity, usageRate: rate, propertyType: store.propertyType }
      })
      storeEntities.push(entity)
    })
    const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
    handler.setInputAction((click) => {
      const picked = viewer.scene.pick(click.position)
      if (Cesium.defined(picked) && picked.id?.properties) selectedStore.value = picked.id.properties.getValue()
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
  } catch { ElMessage.error('加载店铺数据失败') }
}

function flyToStore(store) {
  if (!store.longitude || !store.latitude) return
  viewer.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(store.longitude, store.latitude, 800),
    orientation: { pitch: Cesium.Math.toRadians(-45), heading: 0, roll: 0 }, duration: 1.5
  })
  selectedStore.value = store
}

// ===== AI 加载 =====
async function loadAi() {
  try { const { data } = await getAutoHotProductsApi(); aiHotText.value = data.analysis } catch { aiHotText.value = '数据加载中...' }
  try { const { data } = await getAutoRestockApi(); aiRestockText.value = data.analysis } catch { aiRestockText.value = '数据加载中...' }
}

// ===== 导航 =====
const goToDashboard = () => router.push('/store/list')

// ===== 生命周期 =====
onMounted(async () => {
  initScene()
  await Promise.all([loadStoreMarkers(), loadAi()])
})
onBeforeUnmount(() => { viewer?.destroy(); viewer = null })
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

    <!-- ====== 左侧 AI 洞察卡片 ====== -->
    <div class="float-left">
      <el-card shadow="never" class="ai-card" header="🤖 AI 热销分析">
        <div class="ai-content">{{ aiHotText || '加载中...' }}</div>
      </el-card>
      <el-card shadow="never" class="ai-card" header="📦 AI 补货建议" style="margin-top:12px">
        <div class="ai-content">{{ aiRestockText || '加载中...' }}</div>
      </el-card>
    </div>

    <!-- ====== 右侧数据面板 ====== -->
    <div class="float-right">
      <div class="panel-header">
        <div class="stat-row">
          <span class="stat-num">{{ storeList.length }}</span><span class="stat-lbl">店铺</span>
          <span class="stat-num" style="margin-left:16px">{{ storeList.reduce((s,i) => s+(i.used||0), 0) }}</span><span class="stat-lbl">库存件</span>
        </div>
        <el-button type="primary" size="small" round @click="goToDashboard">管理后台</el-button>
      </div>
      <!-- 图例 -->
      <div class="legend-row">
        <span><i class="dot" style="background:#67c23a"></i>充足</span>
        <span><i class="dot" style="background:#e6a23c"></i>预警</span>
        <span><i class="dot" style="background:#f56c6c"></i>满仓</span>
      </div>
      <!-- 表格 -->
      <div class="table-area">
        <el-table :data="storeList" size="small" stripe highlight-current-row
          @row-click="flyToStore" max-height="calc(100vh - 240px)"
          :row-class-name="({row}) => selectedStore?.id === row.id ? 'selected-row' : ''">
          <el-table-column prop="name" label="店铺名称" min-width="110" show-overflow-tooltip />
          <el-table-column prop="region" label="区域" width="55" align="center" />
          <el-table-column label="容量" width="110" align="center">
            <template #default="{row}">
              <el-progress :percentage="row.usageRate" :stroke-width="6" :show-text="false"
                :status="row.usageRate>85?'exception':row.usageRate>60?'warning':'success'" />
              <span class="cap-text">{{ row.used }}/{{ row.capacity }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.map-page { width:100vw; height:100vh; position:relative; overflow:hidden; background:#0a1628; }

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
    position: absolute; top: 0; left: 0; right: 0;
    display: flex; justify-content: center; padding-top: 6px;
    .top-title {
      font-size: 18px; font-weight: 500; letter-spacing: 4px;
      color: rgba(255,255,255,0.92);
      text-shadow: 0 0 20px rgba(64,158,255,0.6);
      font-family: 'Microsoft YaHei', sans-serif;
    }
  }
}

// Cesium
.cesium-wrapper { position:absolute; inset:0; z-index:0; #cesiumContainer{width:100%;height:100%} }
.cesium-loading { position:absolute; inset:0; z-index:10; display:flex; flex-direction:column; align-items:center; justify-content:center; background:#0a1628; color:#c9d1d9; p{margin-top:12px; font-size:14px} }
.loader-spin { width:40px; height:40px; border:3px solid rgba(64,158,255,.2); border-top-color:#409eff; border-radius:50%; animation:spin 1s linear infinite }
@keyframes spin { to{transform:rotate(360deg)} }

// ====== 左AI卡片 ======
.float-left {
  position: absolute; left: 16px; top: 76px; bottom: 16px; width: 280px; z-index: 10;
  display: flex; flex-direction: column;
  .ai-card {
    flex: 1; background: rgba(10,22,40,0.72); backdrop-filter: blur(12px);
    border: 1px solid rgba(64,158,255,0.18); border-radius: 10px; color: #e0e6f0;
    :deep(.el-card__header) { padding: 10px 14px; font-size: 14px; border-color: rgba(64,158,255,0.12); color: #c0d0e8; }
    :deep(.el-card__body) { padding: 10px 14px; }
    .ai-content { font-size: 12px; line-height: 1.7; white-space: pre-wrap; max-height: calc(100vh - 320px); overflow-y: auto; color: #b0c4d8; }
  }
}

// ====== 右数据面板 ======
.float-right {
  position: absolute; right: 16px; top: 76px; bottom: 16px; width: 340px; z-index: 10;
  background: rgba(10,22,40,0.78); backdrop-filter: blur(16px);
  border: 1px solid rgba(64,158,255,0.2); border-radius: 12px;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 4px 32px rgba(0,0,0,0.45);
  .panel-header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 12px 14px; border-bottom: 1px solid rgba(64,158,255,0.12);
    .stat-row { display:flex; align-items:baseline; gap:2px; .stat-num{font-size:18px;font-weight:700;color:#409eff} .stat-lbl{font-size:11px;color:rgba(224,230,240,.5);margin-right:4px} }
  }
  .legend-row { display:flex; gap:14px; padding:6px 14px; font-size:11px; color:rgba(224,230,240,.45);
    .dot{display:inline-block;width:7px;height:7px;border-radius:50%;margin-right:4px} }
  .table-area {
    flex:1; padding: 0 6px 6px; overflow:hidden;
    :deep(.el-table) {
      background:transparent!important; --el-table-bg-color:transparent; --el-table-tr-bg-color:transparent;
      --el-table-header-bg-color:rgba(64,158,255,.06); --el-table-row-hover-bg-color:rgba(64,158,255,.12);
      --el-table-border-color:rgba(64,158,255,.08); --el-table-text-color:#d0dae8; --el-table-header-text-color:#90a8c8;
      font-size:12px; th{font-weight:500}
      .selected-row{--el-table-tr-bg-color:rgba(64,158,255,.18)!important}
    }
    :deep(.el-table__inner-wrapper::before){display:none}
    .cap-text{display:block;font-size:10px;color:rgba(224,230,240,.45);margin-top:2px}
  }
}
</style>
