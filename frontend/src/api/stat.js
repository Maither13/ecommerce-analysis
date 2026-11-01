import request from '@/utils/request'

// ── 基础指标 ──────────────────────────────────────────
/** 最新一天汇总（看板卡片） */
export const getBasicMetricsLatest = () =>
  request.get('/stats/basic/latest')

/** 近 N 天历史列表 */
export const getBasicMetricsHistory = (days = 30) =>
  request.get('/stats/basic/history', { params: { days } })

/** PV/UV 趋势（?startDate&endDate） */
export const getBasicMetricsTrend = (params) =>
  request.get('/stats/basic/trend', { params })

/** 按范围汇总 */
export const getBasicMetricsSummary = (params) =>
  request.get('/stats/basic-metrics', { params })

// ── 多维度 ────────────────────────────────────────────
/** 全量图表数据，dimension = age/sex/device/source/os/market */
export const getMultiDimension = (dimension, statDate) =>
  request.get('/stats/multi-dimension/all', { params: { dimension, statDate } })

/** 分页表格数据 */
export const getMultiDimensionPage = (params) =>
  request.get('/stats/multi-dimension', { params })

// ── 转化漏斗 ──────────────────────────────────────────
export const getConversionFunnel = (filterType = 'ALL', statDate) =>
  request.get('/stats/funnel', { params: { filterType, statDate } })

// ── 新老用户对比 ──────────────────────────────────────
export const getUserComparison = (statDate) =>
  request.get('/stats/comparison', { params: { statDate } })

export const getUserComparisonList = (statDate) =>
  request.get('/stats/comparison/list', { params: { statDate } })

// ── 系统概览 ──────────────────────────────────────────
export const getStatistics = () => request.get('/stats/info')
