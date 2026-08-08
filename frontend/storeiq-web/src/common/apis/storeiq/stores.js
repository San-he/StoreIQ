/**
 * 店铺管理 API
 */
import { request } from "@/http/axios"

/** 分页列表 */
export function getStoreListApi(data) {
  return request({ url: "/stores/list", method: "post", data })
}

/** 地图标记（全量轻量） */
export function getStoreMapApi() {
  return request({ url: "/stores/map", method: "get" })
}

/** 店铺详情 */
export function getStoreDetailApi(id) {
  return request({ url: `/stores/${id}`, method: "get" })
}

/** 新增店铺 */
export function createStoreApi(data) {
  return request({ url: "/stores", method: "post", data })
}

/** 编辑店铺 */
export function updateStoreApi(id, data) {
  return request({ url: `/stores/${id}`, method: "put", data })
}

/** 删除店铺 */
export function deleteStoreApi(id) {
  return request({ url: `/stores/${id}`, method: "delete" })
}

/** 区域列表 */
export function getStoreRegionsApi() {
  return request({ url: "/stores/regions", method: "get" })
}

/** 某店铺库存明细（含商品详情 + 在售状态） */
export function getStoreInventoryApi(id) {
  return request({ url: `/stores/${id}/inventory`, method: "get" })
}

/** 为店铺添加商品 */
export function addStoreInventoryApi(id, data) {
  return request({ url: `/stores/${id}/inventory`, method: "post", data })
}

/** 销售趋势（按月聚合） */
export function getStoreSalesTrendApi(id, year) {
  return request({ url: `/stores/${id}/sales-trend`, method: "get", params: { year } })
}

/** 库存商品最早/最晚进货日期 */
export function getStoreInventoryPurchaseDatesApi(id) {
  return request({ url: `/stores/${id}/inventory/purchase-dates`, method: "get" })
}
