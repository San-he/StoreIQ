/**
 * 认证相关 API
 */
import { request } from "@/http/axios"

/** 登录 */
export function loginApi(data) {
  return request({
    url: "/auth/login",
    method: "post",
    data
  })
}

/** 获取当前用户信息 */
export function getUserInfoApi() {
  return request({
    url: "/auth/user-info",
    method: "get"
  })
}
