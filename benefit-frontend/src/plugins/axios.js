import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
})

// 請求自動帶 localStorage 的 token（key 必須是 "token"）
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default http
