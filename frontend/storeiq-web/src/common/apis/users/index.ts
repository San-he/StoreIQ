import type * as Users from "./type"
import { request } from "@/http/axios"

/** 获取当前登录用户详情 */
export function getCurrentUserApi() {
  return request<Users.CurrentUserResponseData>({
    url: "/auth/user-info",
    method: "get"
  })
}

/** 获取角色列表 */
export function getUserRolesApi() {
  return request({ url: "/users/roles", method: "get" })
}

/** 用户分页列表 */
export function getUserListApi(data: any) {
  return request({ url: "/users/list", method: "post", data })
}

/** 创建用户 */
export function createUserApi(data: any) {
  return request({ url: "/users", method: "post", data })
}

/** 更新用户 */
export function updateUserApi(id: number, data: any) {
  return request({ url: `/users/${id}`, method: "put", data })
}

/** 删除用户 */
export function deleteUserApi(id: number) {
  return request({ url: `/users/${id}`, method: "delete" })
}
