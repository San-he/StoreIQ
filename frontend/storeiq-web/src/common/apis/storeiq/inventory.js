import { request } from "@/http/axios"

export function getInventoryListApi(data) {
  return request({ url: "/inventory/list", method: "post", data })
}

/** 切换库存商品在售状态 */
export function updateInventoryStatusApi(id, data) {
  return request({ url: `/inventory/${id}/status`, method: "put", data })
}