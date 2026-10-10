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

// ===== 数据清理相关 =====

export function getCleanupSummaryApi() {
  return request({ url: "/cleanup/summary", method: "get" })
}

export function getCleanupRulesApi() {
  return request({ url: "/cleanup/rules", method: "get" })
}

export function saveCleanupRulesApi(data) {
  return request({ url: "/cleanup/rules", method: "put", data })
}

export function executeCleanupApi() {
  return request({ url: "/cleanup/execute", method: "post" })
}

export function getCleanupHistoryApi() {
  return request({ url: "/cleanup/history", method: "get" })
}
