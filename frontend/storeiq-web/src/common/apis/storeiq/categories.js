import { request } from "@/http/axios"

export function getCategoryListApi() {
  return request({ url: "/categories/list", method: "get" })
}

export function createCategoryApi(data) {
  return request({ url: "/categories", method: "post", data })
}

export function updateCategoryApi(id, data) {
  return request({ url: `/categories/${id}`, method: "put", data })
}

export function deleteCategoryApi(id) {
  return request({ url: `/categories/${id}`, method: "delete" })
}
