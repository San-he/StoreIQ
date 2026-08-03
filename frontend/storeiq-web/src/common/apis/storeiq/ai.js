import { request } from "@/http/axios"

export function aiChatApi(data) {
  return request({ url: "/ai/chat", method: "post", data })
}

export function getAutoTrendApi() {
  return request({ url: "/ai/auto/trend", method: "get" })
}

export function getAutoHotProductsApi() {
  return request({ url: "/ai/auto/hot-products", method: "get" })
}

export function getAutoRestockApi() {
  return request({ url: "/ai/auto/restock", method: "get" })
}

export function getAutoProfitOptimizationApi() {
  return request({ url: "/ai/auto/profit-optimization", method: "get" })
}
