import { request } from "@/http/axios"

export function getSupplierListApi(data) {
  return request({ url: "/suppliers/list", method: "post", data })
}

export function getSupplierAllApi() {
  return request({ url: "/suppliers/all", method: "get" })
}

export function createSupplierApi(data) {
  return request({ url: "/suppliers", method: "post", data })
}

export function updateSupplierApi(id, data) {
  return request({ url: `/suppliers/${id}`, method: "put", data })
}

export function deleteSupplierApi(id) {
  return request({ url: `/suppliers/${id}`, method: "delete" })
}
