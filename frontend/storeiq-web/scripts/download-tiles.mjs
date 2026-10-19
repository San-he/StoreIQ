#!/usr/bin/env node
/**
 * 离线地图瓦片下载脚本（解决国内 OSM 瓦片访问不稳定/空白问题）
 *
 * 用法（在 frontend/storeiq-web 目录下执行）：
 *   node scripts/download-tiles.mjs                                     # 默认：全国范围 zoom 4~8，高德源（约 2200 张）
 *   node scripts/download-tiles.mjs --minZoom 4 --maxZoom 10            # 全国 zoom 4~10（约 2.6 万张，量大请谨慎）
 *   node scripts/download-tiles.mjs --minLat 30.6 --maxLat 31.5 --minLon 120.8 --maxLon 122.2 --maxZoom 15  # 单城市高精度
 *   node scripts/download-tiles.mjs --source osm                        # 换用 OSM 源
 *
 * 瓦片保存到 public/tiles/{z}/{x}/{y}.png；MapPicker 组件优先读取本地瓦片（不存在时自动切换高德在线）
 * 提示：zoom 每增加一级，瓦片数约 ×4，全国范围不建议超过 zoom 10；单城市可到 zoom 15~16
 */
import { promises as fs } from 'node:fs'
import path from 'node:path'

const args = process.argv.slice(2)
function arg(name, def) {
  const i = args.indexOf(`--${name}`)
  return i >= 0 && args[i + 1] != null ? args[i + 1] : def
}

const MIN_ZOOM = Number(arg('minZoom', 4))
const MAX_ZOOM = Number(arg('maxZoom', 8))
const MIN_LAT = Number(arg('minLat', 18))
const MAX_LAT = Number(arg('maxLat', 54))
const MIN_LON = Number(arg('minLon', 73))
const MAX_LON = Number(arg('maxLon', 135))
const SOURCE = arg('source', 'amap') // amap | osm
const CONCURRENCY = 6
const OUT_DIR = path.resolve('public/tiles')

const SOURCES = {
  amap: {
    subdomains: ['1', '2', '3', '4'],
    url: (s, z, x, y) => `https://webrd0${s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=${x}&y=${y}&z=${z}`
  },
  osm: {
    subdomains: ['a', 'b', 'c'],
    url: (s, z, x, y) => `https://${s}.tile.openstreetmap.org/${z}/${x}/${y}.png`
  }
}

const sec = (lat) => 1 / Math.cos((lat * Math.PI) / 180)
function tileXY(lon, lat, z) {
  const n = 2 ** z
  const x = Math.floor(((lon + 180) / 360) * n)
  const latRad = (lat * Math.PI) / 180
  const y = Math.floor(((1 - Math.log(Math.tan(latRad) + sec(lat)) / Math.PI) / 2) * n)
  return [x, y]
}

// 生成瓦片清单
const tasks = []
for (let z = MIN_ZOOM; z <= MAX_ZOOM; z++) {
  const [x1, y1] = tileXY(MIN_LON, MAX_LAT, z) // 左上
  const [x2, y2] = tileXY(MAX_LON, MIN_LAT, z) // 右下
  for (let x = x1; x <= x2; x++) for (let y = y1; y <= y2; y++) tasks.push({ z, x, y })
}
console.log(`瓦片清单：zoom ${MIN_ZOOM}~${MAX_ZOOM}，共 ${tasks.length} 张，源：${SOURCE}`)

let done = 0, failed = 0, skipped = 0, lastLog = 0
async function fetchTile(sub, t) {
  const res = await fetch(SOURCES[SOURCE].url(sub, t.z, t.x, t.y), { signal: AbortSignal.timeout(15000) })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return Buffer.from(await res.arrayBuffer())
}

async function download(t, sub) {
  const file = path.join(OUT_DIR, String(t.z), String(t.x), `${t.y}.png`)
  try {
    try { await fs.access(file); skipped++; return } catch { /* 不存在则下载 */ }
    const buf = await fetchTile(sub, t)
    await fs.mkdir(path.dirname(file), { recursive: true })
    await fs.writeFile(file, buf)
    done++
  } catch {
    try { // 重试一次
      const buf = await fetchTile(sub, t)
      await fs.mkdir(path.dirname(file), { recursive: true })
      await fs.writeFile(file, buf)
      done++
    } catch { failed++; console.log(`失败: ${t.z}/${t.x}/${t.y}`) }
  }
  if (Date.now() - lastLog > 2000) {
    lastLog = Date.now()
    console.log(`进度 ${((done + skipped + failed) / tasks.length * 100).toFixed(1)}%（成功 ${done}，跳过 ${skipped}，失败 ${failed}）`)
  }
}

// 并发池
let cursor = 0
async function worker(id) {
  const subs = SOURCES[SOURCE].subdomains
  while (cursor < tasks.length) {
    const t = tasks[cursor++]
    await download(t, subs[id % subs.length])
  }
}
await Promise.all(Array.from({ length: CONCURRENCY }, (_, i) => worker(i)))
console.log(`完成：成功 ${done}，跳过 ${skipped}，失败 ${failed}，输出目录 ${OUT_DIR}`)
