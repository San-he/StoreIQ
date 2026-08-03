/**
 * 商品管理 API
 */
import { request } from "@/http/axios"

export function getProductListApi(data) {
  return request({ url: "/products/list", method: "post", data })
}

export function getProductAllApi() {
  return request({ url: "/products/all", method: "get" })
}

export function getProductDetailApi(id) {
  return request({ url: `/products/${id}`, method: "get" })
}

export function createProductApi(data) {
  return request({ url: "/products", method: "post", data })
}

export function updateProductApi(id, data) {
  return request({ url: `/products/${id}`, method: "put", data })
}

export function deleteProductApi(id) {
  return request({ url: `/products/${id}`, method: "delete" })
}
