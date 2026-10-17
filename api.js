import axios from 'axios'
const baseURL = process.env.VUE_APP_API_BASE_URL || ''
const api = axios.create({ baseURL, withCredentials: true, timeout: 35000 })
let csrf = ''
let pending
export async function refreshCsrf() {
  if (!pending)
    pending = axios
      .get(baseURL + '/api/csrf', { withCredentials: true, timeout: 10000 })
      .then((r) => {
        csrf = r.data.data.token
        return csrf
      })
      .finally(() => {
        pending = null
      })
  return pending
}
api.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) {
    if (!csrf) await refreshCsrf()
    config.headers['X-XSRF-TOKEN'] = csrf
  }
  return config
})
api.interceptors.response.use(
  (response) => {
    if (response.data && response.data.code !== 200)
      return Promise.reject(new Error(response.data.msg || '请求失败'))
    return response
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('CurUser')
      window.dispatchEvent(new Event('session-expired'))
    }
    if (error.response && error.response.status === 403) csrf = ''
    return Promise.reject(
      new Error(
        (error.response && error.response.data && error.response.data.msg) ||
          '连接失败，请检查服务后重试',
      ),
    )
  },
)
export default api
