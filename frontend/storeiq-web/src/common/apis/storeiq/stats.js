import { request } from "@/http/axios"

export function getOverviewApi(params = {}) {
  return request({ url: "/stats/overview", method: "get", params })
}

export function getSalesMonthlyApi(params = {}) {
  return request({ url: "/stats/sales/monthly", method: "get", params })
}

export function getSalesQuarterlyApi(params = {}) {
  return request({ url: "/stats/sales/quarterly", method: "get", params })
}

export function getSalesByStoreApi(params = {}) {
  return request({ url: "/stats/sales/by-store", method: "get", params })
}

export function getSalesByCategoryApi(params = {}) {
  return request({ url: "/stats/sales/by-category", method: "get", params })
}

export function getProfitTrendApi(params = {}) {
  return request({ url: "/stats/profit/trend", method: "get", params })
}

export function getInventoryDistributionApi() {
  return request({ url: "/stats/inventory/distribution", method: "get" })
}

/** 商品销售统计（按店铺/时间/分类筛选，支持分页） */
export function getProductSalesApi(params = {}) {
  return request({
    url: "/stats/product-sales", method: "get", params: {
      pageNum: 1,
      pageSize: 10,
      ...params
    }
  })
}

/** 库存告警 */
export function getInventoryAlertsApi() {
  return request({ url: "/stats/inventory/alerts", method: "get" })
}