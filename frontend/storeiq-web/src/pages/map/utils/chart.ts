/**
 * ECharts 初始化 & 自适应工具
 */
import * as echarts from 'echarts'

export function initChart(domId: string, instance: echarts.ECharts | null): echarts.ECharts | null {
  instance?.dispose()
  const dom = document.getElementById(domId)
  if (!dom) return null
  const chart = echarts.init(dom)
  const observer = new ResizeObserver(() => chart.resize())
  observer.observe(dom)
  return chart
}
