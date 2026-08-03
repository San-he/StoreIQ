import { request } from "@/http/axios"

export function getNotificationListApi() {
  return request({ url: "/notifications/list", method: "get" })
}

export function getUnreadCountApi() {
  return request({ url: "/notifications/unread-count", method: "get" })
}

export function markReadApi(id) {
  return request({ url: `/notifications/${id}/read`, method: "put" })
}

export function markAllReadApi() {
  return request({ url: "/notifications/read-all", method: "put" })
}
