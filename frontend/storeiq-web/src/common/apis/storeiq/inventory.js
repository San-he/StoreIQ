import { request } from "@/http/axios"

export function getInventoryListApi(data) {
  return request({ url: "/inventory/list", method: "post", data })
}

export function getInventoryAlertsApi() {
  return request({ url: "/inventory/alerts", method: "get" })
}
