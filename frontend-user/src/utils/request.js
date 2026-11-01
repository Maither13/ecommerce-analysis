import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('user_token')
  if (token) config.headers['Authorization'] = token
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code === 200) return data.data
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('user_token')
      localStorage.removeItem('user_info')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default request
