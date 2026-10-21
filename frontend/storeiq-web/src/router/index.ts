import type { RouteRecordRaw } from "vue-router"
import { createRouter } from "vue-router"
import { REDIRECT_PATH, routerConfig } from "@/router/config"
import { registerNavigationGuard } from "@/router/guard"
import { flatMultiLevelRoutes } from "./helper"

const Layouts = () => import("@/layouts/index.vue")

/**
 * @name 常驻路由
 * @description 除了 redirect/403/404/login 等隐藏页面，其他页面建议设置唯一的 Name 属性
 */
// 这是一个全局重定向入口。当我们需要从外部系统（如邮件链接、第三方回调）跳转到应用内部时，
// 统一经过 / redirect 页面。它会解析URL参数中的 path，用 router.replace 跳转到目标页，
// 而且用 replace 是为了不污染浏览历史，避免用户后退时看到中间页
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: REDIRECT_PATH,
    component: Layouts,
    meta: {
      hidden: true
    },
    children: [
      {
        path: ":path(.*)",
        component: () => import("@/pages/redirect/index.vue")
      }
    ]
  },
  {
    path: "/403",
    component: () => import("@/pages/error/403.vue"),
    meta: {
      hidden: true
    }
  },
  {
    path: "/404",
    component: () => import("@/pages/error/404.vue"),
    meta: {
      hidden: true
    },
    alias: "/:pathMatch(.*)*"  //alias用户访问 /任意不存在的路径 时，会被匹配到 /404 路由
  },
  {
    path: "/login",
    component: () => import("@/pages/login/index.vue"),
    meta: {
      hidden: true
    }
  },
  // ================ Cesium 地图首页（独立全屏，不走 Layout） ================
  {
    path: "/map",
    component: () => import("@/pages/map/index.vue"),
    name: "MapView",
    meta: {
      hidden: true,
      keepAlive: true
    }
  },
  {
    path: "/",
    component: Layouts,
    // 默认落地页 = 3D 数据大屏首页（登录后/直接访问域名都先进大屏）
    redirect: "/map",
    children: [
      {
        path: "home",
        redirect: "/map",
        name: "Home",
        meta: {
          title: "首页",
          svgIcon: "dashboard",
          affix: true
        }
      }
    ]
  },
  // ================ StoreIQ 业务路由 ================
  {
    path: "/store",
    component: Layouts,
    redirect: "/store/list",
    name: "Store",
    meta: {
      title: "店铺管理",
      elIcon: "Shop"
    },
    children: [
      {
        path: "list",
        component: () => import("@/pages/store/index.vue"),
        name: "StoreList",
        meta: { title: "店铺列表", keepAlive: true }
      },
      {
        path: "detail/:id",
        component: () => import("@/pages/store/detail.vue"),
        name: "StoreDetail",
        meta: { title: "店铺详情", hidden: true }
      }
    ]
  },
  {
    path: "/product",
    component: Layouts,
    redirect: "/product/list",
    name: "Product",
    meta: {
      title: "商品管理",
      elIcon: "Goods"
    },
    children: [
      {
        path: "list",
        component: () => import("@/pages/product/index.vue"),
        name: "ProductList",
        meta: { title: "商品列表", keepAlive: true }
      }
    ]
  },
  {
    path: "/inventory",
    component: Layouts,
    name: "Inventory",
    meta: {
      title: "库存管理",
      elIcon: "Box"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/inventory/index.vue"),
        name: "InventoryList",
        meta: { title: "库存总览", keepAlive: true }
      }
    ]
  },
  {
    path: "/purchase",
    component: Layouts,
    name: "Purchase",
    meta: {
      title: "进货管理",
      elIcon: "ShoppingCart"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/purchase/index.vue"),
        name: "PurchaseList",
        meta: { title: "进货记录", keepAlive: true }
      }
    ]
  },
  {
    path: "/sale",
    component: Layouts,
    name: "Sale",
    meta: {
      title: "销售管理",
      elIcon: "Sell"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/sale/index.vue"),
        name: "SaleList",
        meta: { title: "销售记录", keepAlive: true }
      }
    ]
  },
  {
    path: "/supplier",
    component: Layouts,
    name: "Supplier",
    meta: {
      title: "供应商管理",
      elIcon: "Van"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/supplier/index.vue"),
        name: "SupplierList",
        meta: { title: "供应商列表", keepAlive: true }
      }
    ]
  },
  {
    path: "/analytics",
    component: Layouts,
    name: "Analytics",
    meta: {
      title: "数据分析",
      elIcon: "DataAnalysis"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/analytics/index.vue"),
        name: "AnalyticsView",
        meta: { title: "数据分析", keepAlive: true }
      }
    ]
  },
  {
    path: "/log",
    component: Layouts,
    name: "Log",
    meta: {
      title: "操作日志",
      elIcon: "Document"
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/log/index.vue"),
        name: "LogList",
        meta: { title: "操作日志" }
      }
    ]
  }
]

/**
 * @name 动态路由
 * @description 用来放置有权限 (roles / permissions 属性) 的路由
 * @description 必须带有唯一的 Name 属性
 */
export const dynamicRoutes: RouteRecordRaw[] = [
  {
    path: "/user",
    component: Layouts,
    name: "User",
    meta: {
      title: "用户管理",
      elIcon: "User",
      roles: ["superadmin"]
    },
    children: [
      {
        path: "",
        component: () => import("@/pages/user/index.vue"),
        name: "UserList",
        meta: { title: "用户列表", roles: ["superadmin"] }
      }
    ]
  }
]

/** 路由实例 */
export const router = createRouter({
  history: routerConfig.history,
  routes: routerConfig.thirdLevelRouteCache ? flatMultiLevelRoutes(constantRoutes) : constantRoutes
})

/** 重置路由 */
export function resetRouter() {
  try {
    // 注意：所有动态路由路由必须带有 Name 属性，否则可能会不能完全重置干净
    router.getRoutes().forEach((route) => {
      const { name, meta } = route
      if (name && (meta.roles?.length || meta.permissions?.length)) {
        router.hasRoute(name) && router.removeRoute(name)
      }
    })
  } catch {
    // 强制刷新浏览器也行，只是交互体验不是很好
    location.reload()
  }
}

// 注册路由导航守卫
registerNavigationGuard(router)