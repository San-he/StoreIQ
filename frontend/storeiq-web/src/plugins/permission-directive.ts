import type { App, Directive } from "vue"
import { isArray } from "@@/utils/validate"
import { useUserStore } from "@/pinia/stores/user"

/**
 * @name 权限指令
 * @description 和权限判断函数 checkPermission 功能类似
 */
/** 检查用户是否拥有指定角色或权限中的任意一个，"all" 权限为超级管理员通配符 */
function userHasAccess(roles: string[], permissions: string[], target: string[]): boolean {
  // "all" 权限为超级管理员通配符
  if (permissions.includes("all") || roles.includes("superadmin")) return true
  return roles.some(role => target.includes(role)) || permissions.some(permission => target.includes(permission))
}

const permission: Directive = {
  mounted(el, binding) {
    const { value: rolesOrPermissions } = binding
    const { roles, permissions } = useUserStore()
    if (isArray(rolesOrPermissions) && rolesOrPermissions.length > 0) {
      const hasPermission = userHasAccess(roles, permissions, rolesOrPermissions)
      hasPermission || el.parentNode?.removeChild(el)
    } else {
      throw new Error(`参数必须是一个数组且长度大于 0，参考：v-permission="['admin', 'permission:button-level']"`)
    }
  }
}

export function installPermissionDirective(app: App) {
  app.directive("permission", permission)
}
