import request from '@/utils/request'

// ── 认证 ──────────────────────────────────────────────
export const login = (data) => request.post('/auth/login', data)
export const getUserInfo = () => request.get('/auth/info')

// ── 基础指标 ──────────────────────────────────────────
export const getLatestMetrics = () => request.get('/stats/basic/latest')
export const getTrend = (params) => request.get('/stats/basic/trend', { params })
export const getHistory = (days = 30) => request.get('/stats/basic/history', { params: { days } })

// ── 多维度分析 ────────────────────────────────────────
export const getDimensionAll = (dimension, statDate) =>
  request.get('/stats/multi-dimension/all', { params: { dimension, statDate } })

// ── 转化漏斗 ──────────────────────────────────────────
export const getFunnel = (filterType = 'ALL', statDate) =>
  request.get('/stats/funnel', { params: { filterType, statDate } })

// ── 新老用户对比 ──────────────────────────────────────
export const getComparison = (statDate) =>
  request.get('/stats/comparison', { params: { statDate } })
export const getComparisonList = (statDate) =>
  request.get('/stats/comparison/list', { params: { statDate } })

// ── 系统信息 ──────────────────────────────────────────
export const getStatInfo = () => request.get('/stats/info')
