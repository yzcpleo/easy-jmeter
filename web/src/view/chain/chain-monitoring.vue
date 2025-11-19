<template>
  <div class="container">
    <!-- 控制栏 -->
    <el-card class="control-card">
      <el-form :model="monitorForm" inline>
        <el-form-item label="选择链路">
          <el-select 
            v-model="monitorForm.chainId" 
            placeholder="请选择链路" 
            @change="handleChainChange"
            style="width: 200px">
            <el-option
              v-for="chain in chains"
              :key="chain.id"
              :label="chain.chain_name"
              :value="chain.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="monitorForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px">
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="queryData" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 性能指标卡片 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background-color: #409EFF;">
              <el-icon :size="30"><Timer /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.avgLatency || 0 }}</div>
              <div class="metric-label">平均延时 (ms)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background-color: #67C23A;">
              <el-icon :size="30"><CircleCheck /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.successRate || 0 }}%</div>
              <div class="metric-label">成功率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background-color: #E6A23C;">
              <el-icon :size="30"><Warning /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.p95Latency || 0 }}</div>
              <div class="metric-label">P95延时 (ms)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background-color: #F56C6C;">
              <el-icon :size="30"><CircleClose /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.errorCount || 0 }}</div>
              <div class="metric-label">错误数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>延时趋势</span>
          </template>
          <div ref="latencyTrendChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>吞吐量趋势</span>
          </template>
          <div ref="throughputChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 节点性能对比 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>节点性能对比</span>
          </template>
          <div ref="nodePerformanceChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { onMounted, ref, reactive, onBeforeUnmount } from 'vue'
import { get } from '@/lin/plugin/axios'
import { ElMessage } from 'element-plus'
import { Search, Timer, CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'ChainMonitoring',
  components: {
    Search,
    Timer,
    CircleCheck,
    Warning,
    CircleClose,
  },
  setup() {
    const chains = ref([])
    const loading = ref(false)
    const latencyTrendChart = ref(null)
    const throughputChart = ref(null)
    const nodePerformanceChart = ref(null)
    
    const monitorForm = reactive({
      chainId: null,
      timeRange: []
    })

    const metrics = reactive({
      avgLatency: 0,
      successRate: 0,
      p95Latency: 0,
      errorCount: 0
    })

    const charts = reactive({
      latencyTrend: null,
      throughput: null,
      nodePerformance: null
    })

    onMounted(() => {
      getChains()
      initCharts()
    })

    onBeforeUnmount(() => {
      destroyCharts()
    })

    const getChains = async () => {
      try {
        const res = await get('/v1/chain/trace/page', { page: 0, count: 100 }, { showBackend: true })
        chains.value = res.items || []
      } catch (error) {
        console.error('获取链路列表失败:', error)
        chains.value = []
      }
    }

    // 格式化时间为字符串（格式：yyyy-MM-dd HH:mm:ss）
    // Element UI 日期选择器返回的 Date 对象已经是本地时间（东八区），直接格式化即可
    const formatDateTimeString = (date) => {
      if (!date) return ''
      
      // 如果是字符串，直接处理
      if (typeof date === 'string') {
        // 移除可能的T和Z，保留日期时间部分
        return date.replace('T', ' ').replace('Z', '').substring(0, 19)
      }
      
      // 如果是Date对象，使用本地时间方法格式化（已经是东八区时间）
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }

    const handleChainChange = () => {
      // 当链路改变时，自动查询最近1小时数据（使用本地时间，即东八区时间）
      if (monitorForm.chainId) {
        const now = new Date()
        const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
        // Element UI 日期选择器需要 Date 对象，不是字符串
        monitorForm.timeRange = [oneHourAgo, now]
        queryData()
      }
    }

    const queryData = async () => {
      if (!monitorForm.chainId) {
        ElMessage.warning('请先选择链路')
        return
      }

      if (!monitorForm.timeRange || monitorForm.timeRange.length < 2) {
        ElMessage.warning('请选择时间范围')
        return
      }

      try {
        loading.value = true
        
        // 格式化时间为字符串（格式：yyyy-MM-dd HH:mm:ss）
        // Element UI 日期选择器返回的 Date 对象已经是本地时间（东八区），直接格式化即可
        const startTime = formatDateTimeString(monitorForm.timeRange[0])
        const endTime = formatDateTimeString(monitorForm.timeRange[1])
        
        console.log('时间范围:', {
          timeRange: monitorForm.timeRange,
          startTime: startTime,
          endTime: endTime
        })
        
        // 获取链路统计数据
        const res = await get(`/v1/chain/collection/chain-stats/${monitorForm.chainId}`, {
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        updateMetrics(res)
        
        // 获取节点性能数据
        const nodePerf = await get('/v1/chain/collection/node-performance', {
          chainId: monitorForm.chainId,
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        
        // 获取延时趋势数据
        const trend = await get('/v1/chain/collection/latency-trend', {
          chainId: monitorForm.chainId,
          startTime: startTime,
          endTime: endTime,
          granularity: '5minute'
        }, { showBackend: true })
        
        // 获取吞吐量数据
        const throughput = await get('/v1/chain/collection/throughput', {
          chainId: monitorForm.chainId,
          startTime: startTime,
          endTime: endTime,
          granularity: '5minute'
        }, { showBackend: true })
        
        updateCharts(res, nodePerf, trend, throughput)
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('查询数据失败:', error)
        ElMessage.error('查询数据失败: ' + (error.message || '未知错误'))
      }
    }

    const updateMetrics = (data) => {
      // 兼容后端返回的字段名
      metrics.avgLatency = Math.round(data.avgLatency || data.avgChainLatency || 0)
      metrics.successRate = Math.round(data.successRate || 0)
      metrics.p95Latency = Math.round(data.p95Latency || data.p95ChainLatency || 0)
      metrics.errorCount = data.errorCount || data.failedRequests || 0
    }

    const resetQuery = () => {
      monitorForm.chainId = null
      monitorForm.timeRange = []
      metrics.avgLatency = 0
      metrics.successRate = 0
      metrics.p95Latency = 0
      metrics.errorCount = 0
      setChartsDefaultOptions()
    }

    const initCharts = () => {
      if (!latencyTrendChart.value) return
      
      charts.latencyTrend = echarts.init(latencyTrendChart.value)
      charts.throughput = echarts.init(throughputChart.value)
      charts.nodePerformance = echarts.init(nodePerformanceChart.value)
      
      setChartsDefaultOptions()
    }

    const setChartsDefaultOptions = () => {
      if (!charts.latencyTrend) return

      // 延时趋势图
      charts.latencyTrend.setOption({
        title: { text: '延时趋势', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'time', boundaryGap: false },
        yAxis: { type: 'value', name: '延时(ms)' },
        series: []
      })

      // 吞吐量图
      charts.throughput.setOption({
        title: { text: '吞吐量趋势', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'time', boundaryGap: false },
        yAxis: { type: 'value', name: 'QPS' },
        series: []
      })

      // 节点性能对比
      charts.nodePerformance.setOption({
        title: { text: '节点性能对比', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value', name: '延时(ms)' },
        series: []
      })
    }

    const updateCharts = (data, nodePerf, trend, throughput) => {
      if (!charts.latencyTrend) return

      // 更新延时趋势图
      if (trend && trend.length > 0) {
        const latencyData = trend.map(item => [
          new Date(item.time.replace(' ', 'T')),
          item.avgLatency || 0
        ])
        
        charts.latencyTrend.setOption({
          series: [{
            name: '平均延时',
            type: 'line',
            data: latencyData,
            smooth: true,
            itemStyle: { color: '#409EFF' }
          }]
        })
      }

      // 更新吞吐量图
      if (throughput && throughput.length > 0) {
        const throughputData = throughput.map(item => [
          new Date(item.time.replace(' ', 'T')),
          item.qps || item.count || 0
        ])
        
        charts.throughput.setOption({
          series: [{
            name: 'QPS',
            type: 'line',
            data: throughputData,
            smooth: true,
            itemStyle: { color: '#67C23A' }
          }]
        })
      }

      // 更新节点性能对比图
      if (nodePerf && nodePerf.length > 0) {
        const nodeNames = nodePerf.map(item => item.nodeName || '未知节点')
        const avgLatencies = nodePerf.map(item => item.avgLatency || 0)
        const p95Latencies = nodePerf.map(item => item.p95Latency || 0)

        charts.nodePerformance.setOption({
          xAxis: { data: nodeNames },
          series: [
            {
              name: '平均延时',
              type: 'bar',
              data: avgLatencies,
              itemStyle: { color: '#409EFF' }
            },
            {
              name: 'P95延时',
              type: 'bar',
              data: p95Latencies,
              itemStyle: { color: '#E6A23C' }
            }
          ]
        })
      }
    }

    const destroyCharts = () => {
      if (charts.latencyTrend) charts.latencyTrend.dispose()
      if (charts.throughput) charts.throughput.dispose()
      if (charts.nodePerformance) charts.nodePerformance.dispose()
    }

    return {
      chains,
      monitorForm,
      metrics,
      loading,
      latencyTrendChart,
      throughputChart,
      nodePerformanceChart,
      handleChainChange,
      queryData,
      resetQuery,
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 0 30px;

  .control-card {
    margin-top: 20px;
  }

  .metric-card {
    .metric-content {
      display: flex;
      align-items: center;
      padding: 10px 0;

      .metric-icon {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 20px;
        color: white;
      }

      .metric-info {
        flex: 1;

        .metric-value {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
          line-height: 1;
        }

        .metric-label {
          font-size: 14px;
          color: #909399;
          margin-top: 5px;
        }
      }
    }
  }
}
</style>
