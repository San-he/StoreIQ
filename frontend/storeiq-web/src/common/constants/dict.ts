/**
 * 全局状态字典 — 统一管理 label / value / tag 映射
 *
 * 用法：
 *   import { PROPERTY_TYPE, getDictLabel, getDictTag } from '@@/constants/dict'
 *   getDictLabel(PROPERTY_TYPE, 'owned')  // '购买'
 *   getDictTag(PROPERTY_TYPE, 'owned')    // 'warning'
 */

export interface DictEntry {
  label: string
  tag: 'success' | 'warning' | 'info' | 'danger' | 'primary' | ''
}

/** 店铺产权类型 */
export const PROPERTY_TYPE: Record<string, DictEntry> = {
  owned: { label: '购买', tag: 'warning' },
  lease: { label: '租赁', tag: 'primary' }
}

/** 店铺运营状态 */
export const STORE_STATUS: Record<number, DictEntry> = {
  1: { label: '运营中', tag: 'success' },
  0: { label: '已停用', tag: 'info' }
}

/** 商品上架状态（product.status） */
export const PRODUCT_STATUS: Record<number, DictEntry> = {
  1: { label: '上架', tag: 'success' },
  0: { label: '下架', tag: 'info' }
}

/** 商品在售状态（inventory.status — 店铺内是否在售） */
export const SELLING_STATUS: Record<number, DictEntry> = {
  1: { label: '在售', tag: 'success' },
  0: { label: '已暂停', tag: 'info' }
}

/** 获取字典项的 label */
export function getDictLabel(dict: Record<string | number, DictEntry>, key: string | number | null | undefined): string {
  if (key == null) return '-'
  return dict[key]?.label ?? '-'
}

/** 获取字典项的 tag 类型 */
export function getDictTag(dict: Record<string | number, DictEntry>, key: string | number | null | undefined): string {
  if (key == null) return 'info'
  return dict[key]?.tag ?? 'info'
}
