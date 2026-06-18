<template>
  <div class="history-page">
    <div class="page-card">
      <h2 class="page-title">飞行传感器数据回溯</h2>

      <div class="query-bar">
        <div class="query-form">
          <el-autocomplete
            v-model="form.flightNumber"
            :fetch-suggestions="queryFlightNumbers"
            placeholder="请输入航班号，如 CA1234"
            clearable
            style="width: 280px;"
            @keyup.enter="handleQuery"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-autocomplete>

          <el-button
            type="primary"
            :icon="Search"
            :loading="loading"
            @click="handleQuery"
          >查询数据</el-button>

          <el-select
            v-model="form.maxPoints"
            placeholder="数据采样"
            style="width: 160px;"
            @change="reloadIfLoaded"
          >
            <el-option label="全量数据" :value="null" />
            <el-option label="1000点" :value="1000" />
            <el-option label="500点" :value="500" />
            <el-option label="300点" :value="300" />
            <el-option label="100点" :value="100" />
          </el-select>

          <el-radio-group v-model="viewMode" size="default" @change="updateCharts">
            <el-radio-button label="combined">
              <el-icon><TrendCharts /></el-icon>
              综合视图
            </el-radio-button>
            <el-radio-button label="separate">
              <el-icon><Histogram /></el-icon>
              分图视图
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </div>

    <template v-if="flightData">
      <div class="page-card">
        <h3 class="section-title">航班基本信息</h3>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="航班号">
            <span style="font-weight: 600; color: #2563eb;">{{ flightData.flight?.flightNumber }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="数据点数">
            {{ flightData.summary?.dataPointCount?.toLocaleString() || 0 }} 条
          </el-descriptions-item>
          <el-descriptions-item label="飞行时长">
            {{ flightData.summary?.duration || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(flightData.flight?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="起飞时间">
            {{ formatDateTime(flightData.flight?.departureTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="降落时间">
            {{ formatDateTime(flightData.flight?.arrivalTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="机型">{{ flightData.flight?.aircraftType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册号">{{ flightData.flight?.aircraftRegistration || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="page-card">
        <h3 class="section-title">数据统计概览</h3>
        <div class="stat-grid">
          <div class="stat-item primary">
            <div class="label">最大高度</div>
            <div class="value">{{ formatValue(flightData.summary?.maxAltitude) }} m</div>
          </div>
          <div class="stat-item info">
            <div class="label">平均高度</div>
            <div class="value">{{ formatValue(flightData.summary?.avgAltitude) }} m</div>
          </div>
          <div class="stat-item success">
            <div class="label">最大速度</div>
            <div class="value">{{ formatValue(flightData.summary?.maxSpeed) }} km/h</div>
          </div>
          <div class="stat-item warning">
            <div class="label">平均速度</div>
            <div class="value">{{ formatValue(flightData.summary?.avgSpeed) }} km/h</div>
          </div>
          <div class="stat-item danger">
            <div class="label">最高发动机温度</div>
            <div class="value">{{ formatValue(flightData.summary?.maxEngineTemp) }} ℃</div>
          </div>
          <div class="stat-item success">
            <div class="label">平均发动机温度</div>
            <div class="value">{{ formatValue(flightData.summary?.avgEngineTemp) }} ℃</div>
          </div>
        </div>
      </div>

      <div class="page-card" v-if="viewMode === 'combined'">
        <div class="card-header">
          <h3 class="section-title">综合折线图（多维叠加）</h3>
          <div class="legend-selector">
            <el-checkbox-group v-model="activeSeries" @change="updateCharts">
              <el-checkbox
                v-for="opt in seriesOptions"
                :key="opt.key"
                :label="opt.key"
                :value="opt.key"
              >
                <span :style="{ color: opt.color }">●</span>
                {{ opt.label }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
        <div ref="combinedChartRef" class="chart-container combined-chart"></div>
      </div>

      <template v-if="viewMode === 'separate'">
        <div class="chart-cards" v-if="separateChartConfigs.length">
          <div
            v-for="cfg in separateChartConfigs"
            :key="cfg.key"
            class="page-card chart-card"
          >
            <div class="card-header">
              <h3 class="section-title" :style="{ borderLeftColor: cfg.color }">
                {{ cfg.label }}
              </h3>
              <div class="metric-mini" v-if="cfg.stats">
                <el-tag type="info" size="small">最小: {{ formatValue(cfg.stats.min) }}{{ cfg.unit }}</el-tag>
                <el-tag type="success" size="small">平均: {{ formatValue(cfg.stats.avg) }}{{ cfg.unit }}</el-tag>
                <el-tag type="warning" size="small">最大: {{ formatValue(cfg.stats.max) }}{{ cfg.unit }}</el-tag>
              </div>
            </div>
            <div :ref="el => setSeparateChartRef(cfg.key, el)" class="chart-container"></div>
          </div>
        </div>
      </template>

      <div class="page-card">
        <h3 class="section-title">原始数据预览 (前100条)</h3>
        <el-table :data="previewData" max-height="400" border stripe size="small">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="时间戳" prop="timestamp" width="180" align="center">
            <template #default="{ row }">{{ formatDateTime(row.timestamp) }}</template>
          </el-table-column>
          <el-table-column label="高度(m)" prop="altitude" width="100" align="right">
            <template #default="{ row }">{{ formatValue(row.altitude) }}</template>
          </el-table-column>
          <el-table-column label="速度(km/h)" prop="speed" width="110" align="right">
            <template #default="{ row }">{{ formatValue(row.speed) }}</template>
          </el-table-column>
          <el-table-column label="发动机温度(℃)" prop="engineTemperature" width="140" align="right">
            <template #default="{ row }">{{ formatValue(row.engineTemperature) }}</template>
          </el-table-column>
          <el-table-column label="燃油消耗" prop="fuelConsumption" width="110" align="right">
            <template #default="{ row }">{{ formatValue(row.fuelConsumption) }}</template>
          </el-table-column>
          <el-table-column label="客舱压力" prop="cabinPressure" width="100" align="right">
            <template #default="{ row }">{{ formatValue(row.cabinPressure) }}</template>
          </el-table-column>
          <el-table-column label="垂直速度" prop="verticalSpeed" width="110" align="right">
            <template #default="{ row }">{{ formatValue(row.verticalSpeed) }}</template>
          </el-table-column>
          <el-table-column label="航向" prop="heading" width="90" align="right">
            <template #default="{ row }">{{ formatValue(row.heading) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-empty
      v-else-if="!loading && hasSearched && !flightData"
      description="暂无该航班的数据，请检查航班号是否正确，或先上传CSV数据"
      :image-size="180"
    >
      <el-button type="primary" @click="$router.push('/')">
        <el-icon><UploadFilled /></el-icon>
        去上传数据
      </el-button>
    </el-empty>

    <el-empty
      v-else-if="!loading && !hasSearched"
      description="请输入航班号查询飞行传感器数据"
      :image-size="180"
    />
  </div>
</template>

<script setup>import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, UploadFilled, TrendCharts, Histogram } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { flightApi } from '@/api';
const route = useRoute();
const loading = ref(false);
const hasSearched = ref(false);
const flightData = ref(null);
const form = reactive({
 flightNumber: '',
 maxPoints: 500
});
const viewMode = ref('combined');
const combinedChartRef = ref(null);
let combinedChart = null;
const separateChartRefs = {};
const separateChartInstances = {};
const seriesOptions = [
 { key: 'altitude', label: '高度 (m)', color: '#3b82f6', unit: ' m', yAxisIndex: 0 },
 { key: 'speed', label: '速度 (km/h)', color: '#10b981', unit: ' km/h', yAxisIndex: 1 },
 { key: 'engineTemperature', label: '发动机温度 (℃)', color: '#ef4444', unit: ' ℃', yAxisIndex: 2 },
 { key: 'fuelConsumption', label: '燃油消耗', color: '#f59e0b', unit: '', yAxisIndex: 3 },
 { key: 'cabinPressure', label: '客舱压力', color: '#8b5cf6', unit: '', yAxisIndex: 4 },
 { key: 'verticalSpeed', label: '垂直速度', color: '#06b6d4', unit: '', yAxisIndex: 5 },
 { key: 'heading', label: '航向', color: '#64748b', unit: '°', yAxisIndex: 6 }
];
const activeSeries = ref(['altitude', 'speed', 'engineTemperature']);
const separateChartConfigs = computed(() => {
 if (!flightData.value?.sensorData?.length)
 return [];
 const data = flightData.value.sensorData;
 const configs = [];
 for (const opt of seriesOptions) {
 if (opt.key === 'heading')
 continue;
 const values = data.map(d => d[opt.key]).filter(v => v != null && !isNaN(v));
 if (values.length === 0)
 continue;
 configs.push({
 key: opt.key,
 label: opt.label,
 color: opt.color,
 unit: opt.unit,
 stats: {
 min: Math.min(...values),
 max: Math.max(...values),
 avg: values.reduce((a, b) => a + b, 0) / values.length
 }
 });
 }
 return configs;
});
const previewData = computed(() => {
 return flightData.value?.sensorData?.slice(0, 100) || [];
});
function setSeparateChartRef(key, el) {
 if (el) {
 separateChartRefs[key] = el;
 nextTick(() => {
 if (!separateChartInstances[key]) {
 initSeparateChart(key);
 }
 });
 }
}
function queryFlightNumbers(queryString, cb) {
 flightApi.getFlightNumbers().then(res => {
 if (res.success) {
 const list = (res.data || [])
 .filter(n => n.toLowerCase().includes(queryString.toLowerCase()))
 .slice(0, 20)
 .map(n => ({ value: n }));
 cb(list);
 }
 else {
 cb([]);
 }
 }).catch(() => cb([]));
}
async function handleQuery() {
 if (!form.flightNumber.trim()) {
 ElMessage.warning('请输入航班号');
 return;
 }
 loading.value = true;
 hasSearched.value = true;
 try {
 const res = await flightApi.getFlightData(form.flightNumber.trim(), form.maxPoints);
 if (res.success) {
 flightData.value = res.data;
 await nextTick();
 setTimeout(() => {
 initCharts();
 updateCharts();
 }, 50);
 }
 else {
 flightData.value = null;
 }
 }
 catch (err) {
 flightData.value = null;
 }
 finally {
 loading.value = false;
 }
}
function reloadIfLoaded() {
 if (hasSearched.value && form.flightNumber.trim()) {
 handleQuery();
 }
}
function initCharts() {
 destroyCharts();
 if (viewMode.value === 'combined') {
 if (combinedChartRef.value && !combinedChart) {
 combinedChart = echarts.init(combinedChartRef.value);
 }
 }
 else {
 for (const cfg of separateChartConfigs.value) {
 initSeparateChart(cfg.key);
 }
 }
}
function initSeparateChart(key) {
 const el = separateChartRefs[key];
 if (!el || separateChartInstances[key])
 return;
 separateChartInstances[key] = echarts.init(el);
 updateSeparateChart(key);
}
function updateCharts() {
 if (viewMode.value === 'combined') {
 updateCombinedChart();
 }
 else {
 for (const cfg of separateChartConfigs.value) {
 updateSeparateChart(cfg.key);
 }
 }
}
function updateCombinedChart() {
 if (!combinedChart || !flightData.value?.sensorData?.length)
 return;
 const data = flightData.value.sensorData;
 const timestamps = data.map(d => formatDateTime(d.timestamp));
 const series = [];
 const yAxis = [];
 const colors = [];
 activeSeries.value.forEach((key, index) => {
 const opt = seriesOptions.find(o => o.key === key);
 if (!opt)
 return;
 yAxis.push({
 type: 'value',
 name: opt.label,
 position: index % 2 === 0 ? 'left' : 'right',
 offset: Math.floor(index / 2) * 70,
 axisLine: { show: true, lineStyle: { color: opt.color } },
 axisLabel: { color: opt.color },
 splitLine: { show: index === 0, lineStyle: { type: 'dashed' } }
 });
 colors.push(opt.color);
 series.push({
 name: opt.label,
 type: 'line',
 showSymbol: false,
 smooth: true,
 sampling: 'lttb',
 yAxisIndex: index,
 lineStyle: { width: 1.5, color: opt.color },
 itemStyle: { color: opt.color },
 areaStyle: index === 0 ? {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: opt.color + '40' },
 { offset: 1, color: opt.color + '05' }
 ])
 } : undefined,
 data: data.map(d => d[key] != null ? d[key] : null)
 });
 });
 const option = {
 color: colors,
 tooltip: {
 trigger: 'axis',
 axisPointer: { type: 'cross' },
 confine: true
 },
 legend: {
 data: series.map(s => s.name),
 top: 0,
 type: 'scroll'
 },
 grid: {
 left: '5%',
 right: '15%',
 top: 50,
 bottom: 60,
 containLabel: true
 },
 xAxis: {
 type: 'category',
 data: timestamps,
 boundaryGap: false,
 axisLabel: {
 rotate: 45,
 fontSize: 10,
 interval: Math.floor(timestamps.length / 10)
 },
 axisLine: { lineStyle: { color: '#94a3b8' } }
 },
 yAxis: yAxis,
 dataZoom: [
 { type: 'inside', start: 0, end: 100 },
 { type: 'slider', start: 0, end: 100, height: 25, bottom: 15 }
 ],
 series
 };
 combinedChart.setOption(option, true);
}
function updateSeparateChart(key) {
 const chart = separateChartInstances[key];
 if (!chart || !flightData.value?.sensorData?.length)
 return;
 const data = flightData.value.sensorData;
 const opt = seriesOptions.find(o => o.key === key);
 if (!opt)
 return;
 const timestamps = data.map(d => formatDateTime(d.timestamp));
 const values = data.map(d => d[key] != null ? d[key] : null);
 const option = {
 tooltip: {
 trigger: 'axis',
 axisPointer: { type: 'shadow' }
 },
 grid: {
 left: '3%',
 right: '4%',
 top: 20,
 bottom: 50,
 containLabel: true
 },
 xAxis: {
 type: 'category',
 data: timestamps,
 boundaryGap: false,
 axisLabel: {
 rotate: 45,
 fontSize: 10,
 interval: Math.floor(timestamps.length / 8)
 }
 },
 yAxis: {
 type: 'value',
 name: opt.label,
 axisLabel: { color: opt.color }
 },
 dataZoom: [
 { type: 'inside', start: 0, end: 100 },
 { type: 'slider', start: 0, end: 100, height: 18, bottom: 10 }
 ],
 series: [{
 name: opt.label,
 type: 'line',
 showSymbol: false,
 smooth: true,
 sampling: 'lttb',
 lineStyle: { width: 2, color: opt.color },
 itemStyle: { color: opt.color },
 areaStyle: {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: opt.color + '50' },
 { offset: 1, color: opt.color + '05' }
 ])
 },
 data: values
 }]
 };
 chart.setOption(option, true);
}
function destroyCharts() {
 if (combinedChart) {
 combinedChart.dispose();
 combinedChart = null;
 }
 for (const key in separateChartInstances) {
 separateChartInstances[key]?.dispose();
 delete separateChartInstances[key];
 }
}
function formatValue(v, digits = 2) {
 if (v == null || isNaN(v))
 return '-';
 if (Number.isInteger(v))
 return v.toLocaleString();
 return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: digits });
}
function formatDateTime(dt) {
 if (!dt)
 return '-';
 const d = new Date(dt);
 const pad = n => String(n).padStart(2, '0');
 return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}
let resizeHandler = null;
onMounted(() => {
 if (route.query.flightNumber) {
 form.flightNumber = route.query.flightNumber;
 handleQuery();
 }
 resizeHandler = () => {
 combinedChart?.resize();
 for (const key in separateChartInstances) {
 separateChartInstances[key]?.resize();
 }
 };
 window.addEventListener('resize', resizeHandler);
});
onBeforeUnmount(() => {
 destroyCharts();
 if (resizeHandler) {
 window.removeEventListener('resize', resizeHandler);
 }
});
watch(viewMode, () => {
 destroyCharts();
 hasSearched.value = false;
 nextTick(() => {
 hasSearched.value = !!flightData.value;
 if (flightData.value) {
 setTimeout(() => {
 initCharts();
 updateCharts();
 }, 100);
 }
 });
});
</script>

<style scoped lang="scss">
.history-page {
  .query-bar {
    .query-form {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 12px;

    .section-title {
      margin-bottom: 0;
      padding-left: 10px;
      border-left: 4px solid #2563eb;
      line-height: 1.2;
    }

    .legend-selector {
      display: flex;
      flex-wrap: wrap;
      max-width: 70%;
      gap: 4px 16px;

      :deep(.el-checkbox) {
        margin-right: 0;
      }
    }

    .metric-mini {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }

  .chart-container {
    width: 100%;
    height: 450px;
  }

  .combined-chart {
    height: 520px;
  }

  .chart-cards {
    display: flex;
    flex-direction: column;
    gap: 0;

    .chart-card {
      margin-bottom: 16px;
    }
  }
}
</style>
