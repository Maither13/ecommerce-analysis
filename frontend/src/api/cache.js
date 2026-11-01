import request from '@/utils/request'

export const getCacheList    = (type)   => request.get('/cache/list', { params: { type } })
export const getCacheStats   = ()       => request.get('/cache/stats')
export const getCacheConfig  = ()       => request.get('/cache/config')
export const refreshCache    = (key)    => request.post(`/cache/refresh/${encodeURIComponent(key)}`)
export const deleteCache     = (key)    => request.delete(`/cache/${encodeURIComponent(key)}`)
export const clearAllCache   = ()       => request.post('/cache/clear-all')
export const saveCacheConfig = (config) => request.post('/cache/config', config)
