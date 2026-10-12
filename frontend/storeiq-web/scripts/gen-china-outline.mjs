/**
 * 一次性离线脚本：从 public/geo/china.json 生成全国最外层轮廓 public/geo/china-outline.json
 * 用途：大屏地图"国界双层流光"需要一条连续的最外层边界线；
 *       省级 GeoJSON 没有全国轮廓 feature，故对 34 个省级 feature 求并集（dissolve）后取外环。
 * 运行：npm i --no-save --package-lock=false @turf/turf && node scripts/gen-china-outline.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { featureCollection, dissolve, cleanCoords, simplify, flatten } from '@turf/turf'

const root = path.resolve(import.meta.dirname, '..')
const SRC = path.join(root, 'public/geo/china.json')
const OUT = path.join(root, 'public/geo/china-outline.json')

const RING_KEY_PRECISION = 6 // 顶点 key 保留小数位
const MIN_RING_POINTS = 4    // 少于 4 点的环丢弃
const SIMPLIFY_TOLERANCE = 0.005 // 简化容差（度，约 550m，亚像素级）

// ---------- 工具 ----------
const vkey = (p) => p[0].toFixed(RING_KEY_PRECISION) + ',' + p[1].toFixed(RING_KEY_PRECISION)

/** 去掉连续重复点（容差 1e-7），保证闭合（末点==首点），少于 MIN_RING_POINTS 点返回 null */
function cleanRing(ring) {
  const pts = []
  for (const p of ring) {
    const last = pts[pts.length - 1]
    if (last && Math.abs(last[0] - p[0]) < 1e-7 && Math.abs(last[1] - p[1]) < 1e-7) continue
    pts.push(p)
  }
  if (pts.length < 3) return null
  // 去掉与首点重复的末点后重新闭合
  const first = pts[0]
  const last = pts[pts.length - 1]
  if (Math.abs(first[0] - last[0]) < 1e-7 && Math.abs(first[1] - last[1]) < 1e-7) pts.pop()
  if (pts.length < 3) return null
  pts.push([...pts[0]]) // 闭合
  return pts
}

/** 简化环并返回 {points, pointCount, bbox}，不合格返回 null */
function polishRing(ring) {
  const cleaned = cleanRing(ring)
  if (!cleaned) return null
  const simplified = simplify(
    { type: 'Feature', geometry: { type: 'LineString', coordinates: cleaned } },
    { tolerance: SIMPLIFY_TOLERANCE, highQuality: false, mutate: true }
  )
  const pts = simplified.geometry.coordinates
  if (!pts || pts.length < MIN_RING_POINTS) return null
  const bbox = [Infinity, Infinity, -Infinity, -Infinity]
  for (const [lon, lat] of pts) {
    if (lon < bbox[0]) bbox[0] = lon
    if (lat < bbox[1]) bbox[1] = lat
    if (lon > bbox[2]) bbox[2] = lon
    if (lat > bbox[3]) bbox[3] = lat
  }
  return { points: pts, pointCount: pts.length, bbox }
}

// ---------- 主流程 A：@turf/dissolve 求并集 ----------
function extractViaDissolve(feats) {
  // turf v7 的 dissolve 不接受 MultiPolygon，先 flatten 拆成 Polygon
  const flat = flatten(featureCollection(feats))
  const dissolved = dissolve(flat)
  // 不相邻的陆地（大陆/台湾/海南等）会输出为多个 feature，全部收集
  const rings = []
  for (const f of dissolved.features) {
    const g = f.geometry
    const polys = g.type === 'Polygon' ? [g.coordinates] : g.coordinates
    // 每个 polygon 只取外环（index 0），忽略 holes
    for (const p of polys) rings.push(p[0])
  }
  return rings
}

// ---------- 主流程 B（降级）：外边界边计数 + 最顺时针转向成链 ----------
function extractViaEdgeChaining(feats) {
  const edgeCount = new Map()
  const edgeOf = (a, b) => {
    const sa = vkey(a), sb = vkey(b)
    if (sa === sb) return null
    return sa < sb ? sa + '|' + sb : sb + '|' + sa
  }
  const addRing = (ring) => {
    for (let i = 0; i < ring.length - 1; i++) {
      const k = edgeOf(ring[i], ring[i + 1])
      if (!k) continue
      edgeCount.set(k, (edgeCount.get(k) || 0) + 1)
    }
  }
  for (const f of feats) {
    const g = f.geometry
    const polys = g.type === 'Polygon' ? [g.coordinates] : g.coordinates
    for (const p of polys) for (const r of p) addRing(r)
  }

  // 外边界边：只被 1 个省份使用
  const adj = new Map() // 顶点 -> [{next, key}]
  const edges = new Map() // key -> [a, b] 坐标
  for (const [k, count] of edgeCount) {
    if (count !== 1) continue
    const [sa, sb] = k.split('|')
    const a = sa.split(',').map(Number)
    const b = sb.split(',').map(Number)
    edges.set(k, [a, b])
    if (!adj.has(sa)) adj.set(sa, [])
    if (!adj.has(sb)) adj.set(sb, [])
    adj.get(sa).push({ next: sb, key: k })
    adj.get(sb).push({ next: sa, key: k })
  }

  const rings = []
  while (edges.size) {
    // 取任意一条未用边作为环起点
    const [startKey, [a]] = edges.entries().next().value
    const loop = [a]
    let curKey = vkey(a)
    const startVKey = curKey
    let prevVKey = null
    let guard = 0
    for (;;) {
      // 找到当前边 key：由 prev->cur 确定的 key
      let usedKey = startKey
      if (prevVKey !== null) {
        usedKey = prevVKey < curKey ? prevVKey + '|' + curKey : curKey + '|' + prevVKey
      }
      edges.delete(usedKey)
      if (curKey === startVKey && loop.length > 1) break
      const candidates = (adj.get(curKey) || []).filter((c) => edges.has(c.key) && c.next !== prevVKey)
      if (!candidates.length) break
      // 选择相对来向"最顺时针"的下一条边
      let best = null
      if (prevVKey === null) {
        best = candidates[0]
      } else {
        const [px, py] = prevVKey.split(',').map(Number)
        const [cx, cy] = curKey.split(',').map(Number)
        const dIn = { x: cx - px, y: cy - py }
        let bestAngle = Infinity
        for (const c of candidates) {
          const [nx, ny] = c.next.split(',').map(Number)
          const dOut = { x: nx - cx, y: ny - cy }
          // atan2(cross, dot)：逆时针为正；取最小（最顺时针）
          const angle = Math.atan2(dIn.x * dOut.y - dIn.y * dOut.x, dIn.x * dOut.x + dIn.y * dOut.y)
          if (angle < bestAngle) { bestAngle = angle; best = c }
        }
      }
      prevVKey = curKey
      curKey = best.next
      loop.push(curKey.split(',').map(Number))
      if (++guard > 100000) break
    }
    if (loop.length >= 3 && vkey(loop[0]) === vkey(loop[loop.length - 1])) {
      rings.push(loop)
    }
  }
  return rings
}

// ---------- 主流程 ----------
const src = JSON.parse(readFileSync(SRC, 'utf8'))
const feats = src.features
  .filter((f) => String(f.properties.adcode) !== '100000_JD') // 剔除十段线
  .map((f) => cleanCoords(f, { mutate: false })) // 预清洗连续重复点

let rings = null
try {
  rings = extractViaDissolve(feats)
  console.log('[outline] 用 @turf/dissolve 提取成功')
} catch (e) {
  console.warn('[outline] dissolve 失败，降级为边计数+最顺时针成链:', e.message)
  rings = extractViaEdgeChaining(feats)
}

const result = rings.map(polishRing).filter(Boolean).sort((a, b) => b.pointCount - a.pointCount)
writeFileSync(OUT, JSON.stringify({ generator: 'scripts/gen-china-outline.mjs', source: 'public/geo/china.json', rings: result }))

// ---------- 自检 ----------
console.log('[outline] 环数:', result.length)
result.slice(0, 12).forEach((r, i) => console.log(`  ring[${i}] points=${r.pointCount} bbox=[${r.bbox.map((v) => v.toFixed(2)).join(', ')}]`))
const main = result[0]
const okRings = result.length >= 3
const okBbox = main && main.bbox[0] < 75 && main.bbox[2] > 133 && main.bbox[1] < 21.5 && main.bbox[3] > 53
const okPoints = main && main.pointCount >= 500
if (!okRings || !okBbox || !okPoints) {
  console.error('[outline] 自检失败：ring数>=3:', okRings, '大陆bbox:', okBbox, '点数>=500:', okPoints)
  process.exit(1)
}
console.log('[outline] 自检通过，输出:', OUT)
