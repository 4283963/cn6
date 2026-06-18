import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Upload',
    component: () => import('@/views/UploadPage.vue'),
    meta: { title: '数据上传' }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/HistoryPage.vue'),
    meta: { title: '数据回溯' }
  },
  {
    path: '/flights',
    name: 'Flights',
    component: () => import('@/views/FlightListPage.vue'),
    meta: { title: '航班列表' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
