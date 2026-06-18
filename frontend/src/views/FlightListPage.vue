<template>
  <div class="flight-list-page">
    <div class="page-card">
      <h2 class="page-title">航班列表</h2>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索航班号"
          clearable
          style="width: 320px;"
          @keyup.enter="loadFlights"
          @clear="loadFlights"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button :icon="Search" @click="loadFlights">搜索</el-button>
          </template>
        </el-input>

        <el-button :icon="Refresh" @click="loadFlights">刷新</el-button>
      </div>

      <el-table :data="flights" v-loading="loading" stripe border style="margin-top: 16px;">
        <el-table-column prop="flightNumber" label="航班号" width="140">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #2563eb;">{{ row.flightNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column label="飞行时间" width="180">
          <template #default="{ row }">
            <div class="time-col">
              <div><el-icon><VideoPlay /></el-icon> {{ formatDateTime(row.departureTime) }}</div>
              <div style="margin-top: 4px;"><el-icon><VideoPause /></el-icon> {{ formatDateTime(row.arrivalTime) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="数据点数" prop="totalRecords" width="140" align="right">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.totalRecords?.toLocaleString() || 0 }} 条</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="aircraftType" label="机型" width="120" />
        <el-table-column prop="aircraftRegistration" label="注册号" width="120" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToHistory(row.flightNumber)">
              <el-icon><DataAnalysis /></el-icon>
              查看数据
            </el-button>
            <el-popconfirm
              title="确认删除此航班及其所有传感器数据？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && flights.length === 0" description="暂无航班数据，请先上传CSV文件" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Delete, DataAnalysis, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { flightApi } from '@/api'

const router = useRouter()
const keyword = ref('')
const flights = ref([])
const loading = ref(false)

async function loadFlights() {
  loading.value = true
  try {
    const res = await flightApi.getFlights(keyword.value.trim())
    if (res.success) {
      flights.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

function goToHistory(flightNumber) {
  router.push({ path: '/history', query: { flightNumber } })
}

async function handleDelete(row) {
  try {
    await flightApi.deleteFlight(row.id)
    ElMessage.success('删除成功')
    loadFlights()
  } catch (err) {
  }
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(() => {
  loadFlights()
})
</script>

<style scoped lang="scss">
.flight-list-page {
  .search-bar {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .time-col {
    font-size: 13px;
    color: #475569;
    display: flex;
    flex-direction: column;

    :deep(.el-icon) {
      vertical-align: middle;
      margin-right: 4px;
    }
  }
}
</style>
