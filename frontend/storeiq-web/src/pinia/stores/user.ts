import { getCurrentUserApi } from "@@/apis/users"
import { setToken as _setToken, getToken, removeToken } from "@@/utils/local-storage"
import { pinia } from "@/pinia"
import { resetRouter, router } from "@/router"
import { useSettingsStore } from "./settings"
import { useTagsViewStore } from "./tags-view"

export const useUserStore = defineStore("user", () => {
  // 用户 Token
  // 用于在请求头中传递身份验证信息
  const token = ref<string>(getToken() || "")
  // 用户角色
  const roles = ref<string[]>([])
  // 用户权限
  const permissions = ref<string[]>([])
  // 用户名
  const username = ref<string>("")
  // 是否已获取用户详情
  const isGotUserInfo = ref<boolean>(false)
  // 标签页管理 store
  const tagsViewStore = useTagsViewStore()
  // 设置 store
  const settingsStore = useSettingsStore()

  // 设置 Token
  const setToken = (value: string) => {
    _setToken(value)
    token.value = value
  }

  // 获取用户详情
  const getInfo = async () => {
    const { data } = await getCurrentUserApi()
    username.value = data.username
    roles.value = data.roles ? data.roles : []
    permissions.value = data.permissions ?? []
    // 防止路由守卫逻辑进入无限循环
    isGotUserInfo.value = true
  }

  // 模拟用户变化
  const changeUser = (value: string) => {
    const newToken = `token-${value}`
    token.value = newToken
    _setToken(newToken)
    // 用刷新页面代替重新登录
    location.reload()
  }

  // 登出
  const logout = () => {
    resetToken()
    resetRouter()
    resetTagsView()
    // 重定向到登录页
    router.replace("/login")
  }

  // 重置 Token
  const resetToken = () => {
    removeToken()
    token.value = ""
    roles.value = []
    permissions.value = []
    isGotUserInfo.value = false
  }

  // 重置 Visited Views 和 Cached Views
  const resetTagsView = () => {
    if (!settingsStore.cacheTagsView) {
      tagsViewStore.delAllVisitedViews()
      tagsViewStore.delAllCachedViews()
    }
  }

  return { token, roles, permissions, username, isGotUserInfo, setToken, getInfo, changeUser, logout, resetToken }
})

/**
 * @description 在 SPA 应用中可用于在 pinia 实例被激活前使用 store
 * @description 在 SSR 应用中可用于在 setup 外使用 store
 */
export function useUserStoreOutside() {
  return useUserStore(pinia)
}
