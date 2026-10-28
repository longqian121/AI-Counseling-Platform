import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 5000, // 请求的超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        const { data, config } = response
        if (data.code === '200') {
            return data.data
        } else {
            // 优先使用 data 字段（参数错误的具体信息），其次使用 msg
            const errorMsg = (typeof data.data === 'string' && data.data) || data.msg || '请求失败'
            ElMessage.error(errorMsg)

            if (data.code === '-1') {
                if (!config.url?.includes('/login')) {
                    localStorage.removeItem('token')
                    localStorage.removeItem('userInfo')
                    window.location.href = '/auth/login'
                }
            }
            return Promise.reject(new Error(errorMsg))
        }
    },
    (error) => {
        return Promise.reject(error)
    }
)

export default service