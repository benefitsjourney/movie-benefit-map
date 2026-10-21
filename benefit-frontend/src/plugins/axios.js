import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
})

// 每次送出請求前會自動從瀏覽器的localStorage拿token
// 用一般會員帳號呼叫 POST /auth/login，複製回傳的token
// 測試時F12→Console，貼上localStorage.setItem('token', '貼上token')就可以不經由登入頁登入（30分鐘後失效）
// TODO（登入頁）：之後會員那邊要做的時候這裡不用改，登入成功後執行localStorage.setItem("token", data.token) 就能接
//                登出時執行localStorage.removeItem("token")。※key名稱一定要是 "token"
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default http
