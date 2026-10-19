<template>
  <div class="map-picker">
    <!-- 地址搜索定位 -->
    <div class="mp-search">
      <el-input v-model="searchText" size="small" placeholder="输入地址搜索定位（如：北京市朝阳区三里屯）" clearable @keyup.enter="handleSearch">
        <template #append>
          <el-button size="small" :loading="searching" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <div v-if="searchResults.length" class="mp-results">
        <div v-for="(r, i) in searchResults" :key="i" class="mp-result-item" @click="pickResult(r)">
          <span class="mp-r-name">{{ r.name }}</span>
          <span class="mp-r-extra">{{ r.extra }}</span>
        </div>
      </div>
    </div>
    <!-- 地图 -->
    <div ref="mapEl" class="mp-map"></div>
    <div class="mp-tip">点击地图选择位置，拖拽标记可微调</div>
  </div>
</template>

<script setup>
/**
 * 地图选点组件（Leaflet + OpenStreetMap，免费无 Key）
 * - 点击地图放置标记（可拖拽微调）
 * - 地址搜索定位（open-meteo 地理编码接口，免费无需 Key）
 * v-model:longitude / v-model:latitude 双向绑定坐标；address-found 事件回传搜索到的地址文本
 */
defineOptions({ name: 'MapPicker' })
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  longitude: { type: Number, default: null },
  latitude: { type: Number, default: null }
})
const emit = defineEmits(['update:longitude', 'update:latitude', 'address-found'])

const mapEl = ref(null)
const searchText = ref('')
const searching = ref(false)
const searchResults = ref([])

let map = null
let marker = null

const CHINA_CENTER = [36.5, 104]

// ===== 瓦片源（按优先级自动降级：本地离线 > 高德在线 > OSM 兜底）=====
// OSM 官方瓦片在国内访问极慢/不可用，故默认走本地离线瓦片（scripts/download-tiles.mjs 下载到 public/tiles/），
// 本地不存在时自动切换高德在线瓦片，高德也不可用时兜底 OSM
const TILE_SOURCES = [
  { url: '/tiles/{z}/{x}/{y}.png', subdomains: '' },
  { url: 'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', subdomains: '1234' },
  { url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', subdomains: 'abc' }
]
let tileLayer = null
let tileSourceIndex = 0
let tileErrorCount = 0

/** 添加当前瓦片源；连续 20 张瓦片加载失败则自动切换下一个源 */
function addTileSource() {
  const src = TILE_SOURCES[tileSourceIndex]
  tileLayer = L.tileLayer(src.url, {
    maxZoom: 19,
    subdomains: src.subdomains || '',
    attribution: '© 高德地图 / © OpenStreetMap 贡献者'
  })
  tileLayer.on('tileload', () => { tileErrorCount = 0 })
  tileLayer.on('tileerror', () => {
    tileErrorCount++
    if (tileErrorCount >= 20 && tileSourceIndex < TILE_SOURCES.length - 1) {
      tileSourceIndex++
      tileErrorCount = 0
      map.removeLayer(tileLayer)
      addTileSource()
    }
  })
  tileLayer.addTo(map)
}

/** 坐标保留 7 位小数（约 1cm 精度，与后端存储一致） */
function roundCoord(v) { return Math.round(v * 1e7) / 1e7 }

/** 自定义标记样式：divIcon 避免 leaflet 默认图标资源在 Vite 打包下丢失 */
function createMarker() {
  const m = L.marker([0, 0], {
    icon: L.divIcon({
      className: 'mp-marker-wrap',
      html: '<div class="mp-marker-dot"></div>',
      iconSize: [16, 16],
      iconAnchor: [8, 8]
    }),
    draggable: true
  })
  m.on('dragend', () => {
    const p = m.getLatLng()
    emit('update:latitude', roundCoord(p.lat))
    emit('update:longitude', roundCoord(p.lng))
  })
  return m
}

function placeMarker(lat, lon) {
  if (!marker) {
    marker = createMarker()
    marker.addTo(map)
  }
  marker.setLatLng([lat, lon])
}

function handleMapClick(e) {
  placeMarker(e.latlng.lat, e.latlng.lng)
  emit('update:latitude', roundCoord(e.latlng.lat))
  emit('update:longitude', roundCoord(e.latlng.lng))
  searchResults.value = []
}

async function handleSearch() {
  const kw = searchText.value.trim()
  if (!kw || searching.value) return
  searching.value = true
  try {
    const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(kw)}&count=5&language=zh&format=json`)
    const json = await res.json()
    searchResults.value = (json.results || []).map(r => ({
      name: r.name,
      extra: [r.admin1, r.country].filter(Boolean).join(' · '),
      lat: r.latitude,
      lon: r.longitude
    }))
    if (!searchResults.value.length) ElMessage.warning('未找到该地址，换个关键词试试')
  } catch {
    searchResults.value = []
    ElMessage.warning('地址搜索失败，请检查网络')
  } finally { searching.value = false }
}

function pickResult(r) {
  searchResults.value = []
  searchText.value = r.name
  placeMarker(r.lat, r.lon)
  map.setView([r.lat, r.lon], Math.max(map.getZoom(), 14))
  emit('update:latitude', roundCoord(r.lat))
  emit('update:longitude', roundCoord(r.lon))
  emit('address-found', [r.name, r.extra].filter(Boolean).join(' · '))
}

// 外部坐标变化（编辑回填 / 表单重置）→ 同步标记
watch(() => [props.longitude, props.latitude], ([lon, lat]) => {
  if (!map) return
  if (lon == null || lat == null) {
    if (marker) { marker.remove(); marker = null }
    return
  }
  placeMarker(lat, lon)
  map.setView([lat, lon], Math.max(map.getZoom(), 12), { animate: false })
})

/** 弹窗打开后调用：地图在隐藏容器中初始化时尺寸为 0，需重新计算 */
function refresh() { map?.invalidateSize() }
defineExpose({ refresh })

onMounted(() => {
  map = L.map(mapEl.value, {
    center: CHINA_CENTER,
    zoom: 5,
    minZoom: 4,                        // 低于 4 级（世界视角）无瓦片覆盖，会黑屏
    maxBounds: [[3.5, 65], [54, 145]], // 视野限制在中国及周边，防止拖出瓦片覆盖区
    maxBoundsViscosity: 0.6
  })
  addTileSource()
  map.on('click', handleMapClick)
  // 编辑模式：初始化已有坐标
  if (props.latitude != null && props.longitude != null) {
    placeMarker(props.latitude, props.longitude)
    map.setView([props.latitude, props.longitude], 13, { animate: false })
  }
})

onBeforeUnmount(() => {
  map?.remove()
  map = null
  marker = null
})
</script>

<style lang="scss" scoped>
.map-picker {
  position: relative;
  width: 100%;
}
.mp-search {
  margin-bottom: 8px;
  position: relative;
}
.mp-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 1000;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  max-height: 180px;
  overflow: auto;
}
.mp-result-item {
  padding: 6px 10px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  &:hover { background: #f0f7ff; }
  .mp-r-extra { color: #909399; font-size: 12px; white-space: nowrap; }
}
.mp-map {
  height: 260px;
  width: 100%;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  // 瓦片未加载/缺失时显示浅灰底而非黑色
  background: #e8eaed;
}
.mp-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
:deep(.mp-marker-wrap) {
  .mp-marker-dot {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #f56c6c;
    border: 2px solid #fff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
  }
}
</style>
