import { request } from "@/http/axios"

export function getInventoryListApi(data) {
  return request({ url: "/inventory/list", method: "post", data })
}

export function getInventoryAlertsApi() {
  return request({ url: "/inventory/alerts", method: "get" })
}

/** 切换库存商品在售状态 */
export function updateInventoryStatusApi(id, data) {
  return request({ url: `/inventory/${id}/status`, method: "put", data })
}
