import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10 * 60 * 1000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res && res.success === false) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const uploadApi = {
  uploadFile(file, flightNumber, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('flightNumber', flightNumber)

    return request.post('/upload/file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: progressEvent => {
        if (onProgress && progressEvent.total > 0) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
  },

  getProgress(taskId) {
    return request.get(`/upload/progress/${taskId}`)
  },

  getTasks() {
    return request.get('/upload/tasks')
  }
}

export const flightApi = {
  getFlights(keyword) {
    return request.get('/flights', { params: { keyword } })
  },

  getFlightNumbers() {
    return request.get('/flights/numbers')
  },

  getFlight(flightNumber) {
    return request.get(`/flights/${flightNumber}`)
  },

  getFlightData(flightNumber, maxPoints, startTime, endTime) {
    return request.get(`/flights/${flightNumber}/data`, {
      params: { maxPoints, startTime, endTime }
    })
  },

  deleteFlight(id) {
    return request.delete(`/flights/${id}`)
  }
}

export default request
