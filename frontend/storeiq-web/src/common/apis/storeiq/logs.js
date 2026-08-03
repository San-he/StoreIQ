import { request } from "@/http/axios"

export function getLogModulesApi() {
  return request({ url: "/logs/modules", method: "get" })
}

export function getLogListApi(data) {
  return request({ url: "/logs/list", method: "post", data })
}
