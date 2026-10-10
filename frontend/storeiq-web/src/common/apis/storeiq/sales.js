import { request } from "@/http/axios"

export function getSaleListApi(data) {
  return request({ url: "/sales/list", method: "post", data })
}

export function getSaleDetailApi(id) {
  return request({ url: `/sales/${id}`, method: "get" })
}

export function createSaleApi(data) {
  return request({ url: "/sales", method: "post", data })
}

export function createSaleBatchApi(data) {
  return request({ url: "/sales/batch", method: "post", data })
}

export function exportSaleApi(data) {
  return request({ url: "/sales/export", method: "post", data })
}

export function scanProductApi(code) {
  return request({ url: `/sales/scan/${code}`, method: "get" })
}
