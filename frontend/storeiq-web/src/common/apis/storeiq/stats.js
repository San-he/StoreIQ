import { request } from "@/http/axios"

export function getOverviewApi() {
  return request({ url: "/stats/overview", method: "get" })
}

export function getSalesMonthlyApi(year = 2026) {
  return request({ url: `/stats/sales/monthly?year=${year}`, method: "get" })
}

export function getSalesQuarterlyApi(year = 2026) {
  return request({ url: `/stats/sales/quarterly?year=${year}`, method: "get" })
}

export function getSalesByStoreApi() {
  return request({ url: "/stats/sales/by-store", method: "get" })
}

export function getSalesByCategoryApi() {
  return request({ url: "/stats/sales/by-category", method: "get" })
}

export function getProfitTrendApi(year = 2026) {
  return request({ url: `/stats/profit/trend?year=${year}`, method: "get" })
}

export function getInventoryDistributionApi() {
  return request({ url: "/stats/inventory/distribution", method: "get" })
}

/** 商品销售统计（按店铺/时间/分类筛选，支持分页） */
export function getProductSalesApi(params = {}) {
  return request({ url: "/stats/product-sales", method: "get", params: {
    pageNum: 1,
    pageSize: 10,
    ...params
  }})
}
