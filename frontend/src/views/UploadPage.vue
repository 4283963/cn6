<template>
  <div class="upload-page">
    <div class="page-card">
      <h2 class="page-title">CSV 数据文件上传</h2>

      <el-form :model="form" label-width="100px" class="upload-form">
        <el-form-item label="航班号" required>
          <el-input
            v-model="form.flightNumber"
            placeholder="请输入航班号，如：CA1234、MU5678"
            clearable
            maxlength="20"
            style="max-width: 400px;"
          >
            <template #prefix>
              <el-icon><Promotion /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".csv"
          >
            <div class="upload-inner">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将 CSV 文件拖到此处，或<em>点击上传</em>
              </div>
              <div class="el-upload__tip" style="margin-top: 10px;">
                <el-icon><InfoFilled /></el-icon>
                支持几百MB的大文件，仅支持 csv 格式
              </div>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="fileInfo" label="文件信息">
          <div class="file-info">
            <div class="info-row">
              <span class="info-label">文件名：</span>
              <span class="info-value">{{ fileInfo.name }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">文件大小：</span>
              <span class="info-value">{{ formatFileSize(fileInfo.size) }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :icon="uploading ? '' : (UploadFilled)"
            :loading="uploading"
            :disabled="!canSubmit || uploading"
            @click="handleUpload"
          >
            {{ uploading ? '上传中...' : '开始上传并解析' }}
          </el-button>
          <el-button
            size="large"
            :disabled="!fileInfo"
            @click="resetForm"
          >
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="uploading || parseProgress" class="progress-area">
        <div class="section-title">上传与解析进度</div>

        <el-steps :active="currentStep" finish-status="success" process-status="process" align-center>
          <el-step title="上传文件" :icon="UploadFilled" />
          <el-step title="解析数据" :icon="DataAnalysis" />
          <el-step title="入库完成" :icon="CircleCheckFilled" />
        </el-steps>

        <div class="progress-detail">
          <div class="progress-item">
            <div class="progress-label">
              <span>文件上传</span>
              <span class="progress-value">{{ uploadProgress }}%</span>
            </div>
            <el-progress :percentage="uploadProgress" :stroke-width="14" :show-text="false" status="success" />
          </div>
          <div class="progress-item">
            <div class="progress-label">
              <span>数据解析入库</span>
              <span class="progress-value">{{ parseProgress?.progress || 0 }}%</span>
            </div>
            <el-progress
              :percentage="parseProgress?.progress || 0"
              :stroke-width="14"
              :show-text="false"
              :status="parseStatus"
            />
          </div>
        </div>

        <div v-if="parseProgress" class="parse-info">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="任务ID">{{ parseProgress.taskId }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatFileSize(parseProgress.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType">{{ statusText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已处理记录">
              {{ parseProgress.processedRecords?.toLocaleString() || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="总记录数">
              {{ parseProgress.totalRecords?.toLocaleString() || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="航班号">{{ parseProgress.flightNumber }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="parseProgress.errorMessage" class="error-msg">
            <el-alert
              :title="parseProgress.errorMessage"
              type="error"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <h2 class="page-title">上传任务记录</h2>
      <el-table :data="tasks" v-loading="loadingTasks" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="flightNumber" label="航班号" width="120" />
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="processedRecords" label="记录数" width="120">
          <template #default="{ row }">{{ row.processedRecords?.toLocaleString() || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskTagType(row.status)" size="small">{{ getTaskStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="getTaskProgress(row)" :stroke-width="8" :show-text="true" />
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="primary"
              link
              @click="goToHistory(row.flightNumber)"
            >查看数据</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <h2 class="page-title">CSV 文件格式说明</h2>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <div style="line-height: 1.8;">
            <p><strong>表头字段（支持中英文别名）：</strong></p>
            <ul style="margin: 10px 0; padding-left: 24px;">
              <li><code>timestamp</code> / <code>time</code> / <code>时间</code> — 时间戳 (必填)，格式：YYYY-MM-DD HH:mm:ss 或 ISO 格式或毫秒时间戳</li>
              <li><code>altitude</code> / <code>高度</code> / <code>height</code> — 高度（米）</li>
              <li><code>speed</code> / <code>速度</code> / <code>tas</code> — 速度（km/h）</li>
              <li><code>engineTemperature</code> / <code>egt</code> / <code>发动机温度</code> — 发动机温度（℃）</li>
              <li><code>fuelConsumption</code> / <code>fuel</code> / <code>燃油消耗</code> — 燃油消耗</li>
              <li><code>cabinPressure</code> / <code>客舱压力</code> — 客舱压力</li>
              <li><code>latitude</code> / <code>lat</code> / <code>纬度</code> — 纬度</li>
              <li><code>longitude</code> / <code>lon</code> / <code>经度</code> — 经度</li>
              <li><code>verticalSpeed</code> / <code>vs</code> / <code>垂直速度</code> — 垂直速度</li>
              <li><code>heading</code> / <code>hdg</code> / <code>航向</code> — 航向角</li>
            </ul>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled, DataAnalysis, CircleCheckFilled,
  InfoFilled, Promotion
} from '@element-plus/icons-vue'
import { uploadApi } from '@/api'

const router = useRouter()
const uploadRef = ref(null)

const form = ref({
  flightNumber: ''
})
const fileInfo = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const parseProgress = ref(null)
const currentTaskId = ref(null)
const tasks = ref([])
const loadingTasks = ref(false)
const progressTimer = ref(null)

const canSubmit = computed(() => {
  return form.value.flightNumber.trim() && fileInfo.value
})

const currentStep = computed(() => {
  if (!uploading.value && !parseProgress.value) return 0
  if (parseProgress.value?.status === 'COMPLETED') return 3
  if (parseProgress.value?.status === 'FAILED') return 1
  if (uploadProgress.value < 100) return 1
  return 2
})

const parseStatus = computed(() => {
  if (!parseProgress.value) return null
  if (parseProgress.value.status === 'COMPLETED') return 'success'
  if (parseProgress.value.status === 'FAILED') return 'exception'
  return null
})

const statusText = computed(() => getTaskStatusText(parseProgress.value?.status))
const statusTagType = computed(() => getTaskTagType(parseProgress.value?.status))

function handleFileChange(file) {
  if (!file.name.toLowerCase().endsWith('.csv')) {
    ElMessage.error('请上传 CSV 格式的文件')
    uploadRef.value?.clearFiles()
    fileInfo.value = null
    return
  }
  fileInfo.value = file
}

function handleFileRemove() {
  fileInfo.value = null
}

async function handleUpload() {
  if (!canSubmit.value) return

  uploading.value = true
  uploadProgress.value = 0
  parseProgress.value = null

  try {
    const res = await uploadApi.uploadFile(
      fileInfo.value,
      form.value.flightNumber.trim(),
      (percent) => {
        uploadProgress.value = percent
      }
    )

    if (res.success && res.data) {
      currentTaskId.value = res.data.taskId
      parseProgress.value = res.data

      startProgressPolling()
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (err) {
    uploading.value = false
    uploadProgress.value = 0
  }
}

function startProgressPolling() {
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
  }

  progressTimer.value = setInterval(async () => {
    if (!currentTaskId.value) return

    try {
      const res = await uploadApi.getProgress(currentTaskId.value)
      if (res.success && res.data) {
        parseProgress.value = res.data

        if (res.data.status === 'COMPLETED') {
          uploading.value = false
          uploadProgress.value = 100
          clearInterval(progressTimer.value)
          progressTimer.value = null
          ElMessage.success(`解析完成，成功导入 ${res.data.processedRecords?.toLocaleString() || 0} 条记录`)
          loadTasks()
        } else if (res.data.status === 'FAILED') {
          uploading.value = false
          clearInterval(progressTimer.value)
          progressTimer.value = null
          ElMessage.error('数据解析失败：' + (res.data.errorMessage || '未知错误'))
          loadTasks()
        }
      }
    } catch (err) {
      console.error('查询进度失败', err)
    }
  }, 2000)
}

function resetForm() {
  form.value.flightNumber = ''
  uploadRef.value?.clearFiles()
  fileInfo.value = null
  uploading.value = false
  uploadProgress.value = 0
  parseProgress.value = null
  currentTaskId.value = null
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
    progressTimer.value = null
  }
}

async function loadTasks() {
  loadingTasks.value = true
  try {
    const res = await uploadApi.getTasks()
    if (res.success) {
      tasks.value = res.data || []
    }
  } finally {
    loadingTasks.value = false
  }
}

function goToHistory(flightNumber) {
  router.push({ path: '/history', query: { flightNumber } })
}

function getTaskProgress(task) {
  if (task.status === 'COMPLETED') return 100
  return task.progress || 0
}

function getTaskStatusText(status) {
  const map = {
    PENDING: '等待中',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

function getTaskTagType(status) {
  const map = {
    PENDING: 'info',
    PROCESSING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || ''
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN')
}

onMounted(() => {
  loadTasks()
})

onBeforeUnmount(() => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
  }
})
</script>

<style scoped lang="scss">
.upload-page {
  .upload-form {
    max-width: 900px;
  }

  .upload-dragger {
    width: 100%;
    max-width: 700px;

    .upload-inner {
      padding: 20px 0;

      .upload-icon {
        font-size: 67px;
        color: #409EFF;
      }

      .el-upload__text {
        em {
          color: #409EFF;
          font-style: normal;
        }
      }

      .el-upload__tip {
        color: #909399;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
      }
    }
  }

  .file-info {
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    max-width: 400px;

    .info-row {
      display: flex;
      margin: 6px 0;

      .info-label {
        color: #6b7280;
        min-width: 80px;
      }

      .info-value {
        color: #1f2937;
        font-weight: 500;
        word-break: break-all;
      }
    }
  }

  .progress-area {
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px dashed #e5e7eb;

    .progress-detail {
      margin: 24px 0;
      padding: 0 20px;

      .progress-item {
        margin: 18px 0;

        .progress-label {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          font-size: 14px;
          color: #374151;

          .progress-value {
            font-weight: 600;
            color: #2563eb;
          }
        }
      }
    }

    .parse-info {
      margin-top: 20px;

      .error-msg {
        margin-top: 16px;
      }
    }
  }
}
</style>
