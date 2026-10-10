import { request } from "@/http/axios"

export function getPurchaseListApi(data) {
  return request({ url: "/purchases/list", method: "post", data })
}

export function getPurchaseDetailApi(id) {
  return request({ url: `/purchases/${id}`, method: "get" })
}

export function createPurchaseApi(data) {
  return request({ url: "/purchases", method: "post", data })
}

export function createPurchaseBatchApi(data) {
  return request({ url: "/purchases/batch", method: "post", data })
}

export function exportPurchaseApi(data) {
  return request({ url: "/purchases/export", method: "post", data })
}
