<template>
  <div class="container">
    <!-- 查询表单 -->
    <el-card>
      <el-form :model="queryForm" inline>
        <el-form-item label="链路">
          <el-select v-model="queryForm.chainId" placeholder="选择链路" style="width: 200px;">
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
            v-model="queryForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="粒度">
          <el-select v-model="queryForm.granularity" style="width: 120px;">
            <el-option label="分钟" value="minute"></el-option>
            <el-option label="5分钟" value="5minute"></el-option>
            <el-option label="小时" value="hour"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="exportReport">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 整体性能概览 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>性能概览</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="平均延时" :value="metrics.avgLatency" suffix="ms">
            <template #prefix>
              <el-icon color="#409EFF"><Timer /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="成功率" :value="metrics.successRate" suffix="%">
            <template #prefix>
              <el-icon color="#67C23A"><CircleCheck /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="P95延时" :value="metrics.p95Latency" suffix="ms">
            <template #prefix>
              <el-icon color="#E6A23C"><Warning /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="总请求数" :value="metrics.totalCount">
            <template #prefix>
              <el-icon color="#909399"><DataLine /></el-icon>
            </template>
          </el-statistic>
        </el-col>
      </el-row>
    </el-card>

    <!-- 趋势图表 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>延时趋势分析</span>
      </template>
      <div ref="latencyTrendChart" style="width: 100%; height: 350px;"></div>
    </el-card>

    <!-- 吞吐量和节点对比 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>吞吐量趋势</span>
          </template>
          <div ref="throughputChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>节点性能对比</span>
          </template>
          <div ref="nodeComparisonChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 延时分布 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>延时分布</span>
      </template>
      <div ref="distributionChart" style="width: 100%; height: 300px;"></div>
    </el-card>

    <!-- 瓶颈节点分析 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>瓶颈节点分析</span>
      </template>
      <el-table :data="bottleneckNodes" size="small">
        <el-table-column prop="nodeName" label="节点名称"></el-table-column>
        <el-table-column prop="avgLatency" label="平均延时(ms)" sortable></el-table-column>
        <el-table-column prop="maxLatency" label="最大延时(ms)" sortable></el-table-column>
        <el-table-column prop="p95Latency" label="P95延时(ms)" sortable></el-table-column>
        <el-table-column prop="errorRate" label="错误率" sortable>
          <template #default="scope">
            <span :style="{color: scope.row.errorRate > 5 ? '#F56C6C' : '#67C23A'}">
              {{ scope.row.errorRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="recommendation" label="建议" show-overflow-tooltip></el-table-column>
      </el-table>
    </el-card>

    <!-- 详细数据表格 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>详细数据</span>
      </template>
      <el-table :data="detailData" size="small" max-height="400">
        <el-table-column label="请求ID" width="180" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.requestId || scope.row.request_id || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="节点" width="120">
          <template #default="scope">
            {{ scope.row.nodeName || scope.row.node_name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="latency" label="延时(ms)" width="100" sortable>
          <template #default="scope">
            {{ scope.row.latency || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.requestStartTime || scope.row.request_start_time) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.requestEndTime || scope.row.request_end_time) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="(scope.row.success === 1 || scope.row.success === true) ? 'success' : 'danger'" size="small">
              {{ (scope.row.success === 1 || scope.row.success === true) ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.count"
        :page-sizes="[20, 50, 100, 200]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="query"
        @current-change="query"
        style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>
  </div>
</template>

<script>
import { onMounted, ref, reactive, onBeforeUnmount } from 'vue'
import { get } from '@/lin/plugin/axios'
import { ElMessage } from 'element-plus'
import { Search, Download, Timer, CircleCheck, Warning, DataLine } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'ChainAnalysis',
  components: {
    Search,
    Download,
    Timer,
    CircleCheck,
    Warning,
    DataLine,
  },
  setup() {
    const chains = ref([])
    const loading = ref(false)
    const detailData = ref([])
    const bottleneckNodes = ref([])
    const latencyTrendChart = ref(null)
    const throughputChart = ref(null)
    const nodeComparisonChart = ref(null)
    const distributionChart = ref(null)

    let chartLatencyTrend = null
    let chartThroughput = null
    let chartNodeComparison = null
    let chartDistribution = null

    const queryForm = reactive({
      chainId: null,
      timeRange: [],
      granularity: '5minute'
    })

    const metrics = reactive({
      avgLatency: 0,
      successRate: 0,
      p95Latency: 0,
      totalCount: 0
    })

    const pagination = reactive({
      page: 1,
      count: 20,
      total: 0
    })

    onMounted(() => {
      getChains()
      initCharts()
      setDefaultTimeRange()
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

    const setDefaultTimeRange = () => {
      const end = new Date()
      const start = new Date(end.getTime() - 3600000) // 最近1小时
      queryForm.timeRange = [start, end]
    }

    const query = async () => {
      if (!queryForm.chainId || !queryForm.timeRange || queryForm.timeRange.length < 2) {
        ElMessage.warning('请选择链路和时间范围')
        return
      }

      try {
        loading.value = true
        
        // 格式化时间为字符串（格式：yyyy-MM-dd HH:mm:ss）
        const startTime = formatDateTimeString(queryForm.timeRange[0])
        const endTime = formatDateTimeString(queryForm.timeRange[1])
        
        console.log('性能分析查询参数:', {
          chainId: queryForm.chainId,
          timeRange: queryForm.timeRange,
          startTime: startTime,
          endTime: endTime,
          granularity: queryForm.granularity
        })
        
        // 获取统计数据
        const stats = await get(`/v1/chain/collection/chain-stats/${queryForm.chainId}`, {
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        
        console.log('性能统计数据:', stats)
        
        // 检查是否有数据
        if (!stats || (stats.totalRequests === 0 && stats.totalCount === 0)) {
          ElMessage.warning(`所选时间范围内没有数据。请检查：\n1. 该链路是否已启动数据收集\n2. 时间范围是否包含数据收集的时间段\n3. 数据收集是否成功`)
          // 清空图表和数据
          updateMetrics({ avgLatency: 0, successRate: 0, p95Latency: 0, totalCount: 0 })
          updateLatencyTrendChart([])
          updateThroughputChart([])
          updateNodeComparisonChart([])
          updateDistributionChart([])
          detailData.value = []
          pagination.total = 0
          loading.value = false
          return
        }
        
        updateMetrics(stats)
        
        // 获取趋势数据
        const trend = await get('/v1/chain/collection/latency-trend', {
          chainId: queryForm.chainId,
          startTime: startTime,
          endTime: endTime,
          granularity: queryForm.granularity
        }, { showBackend: true })
        
        updateLatencyTrendChart(trend)
        
        // 获取吞吐量数据
        const throughput = await get('/v1/chain/collection/throughput', {
          chainId: queryForm.chainId,
          startTime: startTime,
          endTime: endTime,
          granularity: queryForm.granularity
        }, { showBackend: true })
        
        updateThroughputChart(throughput)
        
        // 获取节点性能数据
        const nodePerf = await get('/v1/chain/collection/node-performance', {
          chainId: queryForm.chainId,
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        
        updateNodeComparisonChart(nodePerf)
        analyzeBottlenecks(nodePerf)
        
        // 获取延时分布
        const distribution = await get('/v1/chain/collection/latency-distribution', {
          chainId: queryForm.chainId,
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        
        updateDistributionChart(distribution)
        
        // 获取详细数据
        const detail = await get('/v1/chain/collection/data', {
          chainId: queryForm.chainId,
          startCollectionTime: startTime,
          endCollectionTime: endTime,
          page: pagination.page - 1,
          count: pagination.count
        }, { showBackend: true })
        
        // 处理数据格式，兼容下划线和驼峰命名
        detailData.value = (detail.items || []).map(item => ({
          ...item,
          requestId: item.requestId || item.request_id,
          nodeName: item.nodeName || item.node_name,
          requestStartTime: item.requestStartTime || item.request_start_time,
          requestEndTime: item.requestEndTime || item.request_end_time,
          collectionTime: item.collectionTime || item.collection_time
        }))
        pagination.total = detail.total || 0
        
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('查询失败:', error)
        ElMessage.error('查询失败')
      }
    }

    const updateMetrics = (stats) => {
      // 兼容后端返回的字段名
      metrics.avgLatency = Math.round(stats.avgLatency || stats.avgChainLatency || 0)
      metrics.successRate = Math.round(stats.successRate || 0)
      metrics.p95Latency = Math.round(stats.p95Latency || stats.p95ChainLatency || 0)
      metrics.totalCount = stats.totalCount || stats.totalRequests || 0
    }

    const initCharts = () => {
      if (latencyTrendChart.value) {
        chartLatencyTrend = echarts.init(latencyTrendChart.value)
        chartLatencyTrend.setOption({
          tooltip: { trigger: 'axis' },
          legend: { bottom: 10 },
          grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value', name: '延时(ms)' },
          series: [
            { name: '平均延时', type: 'line', smooth: true, data: [] },
            { name: 'P95延时', type: 'line', smooth: true, data: [] },
            { name: 'P99延时', type: 'line', smooth: true, data: [] }
          ]
        })
      }

      if (throughputChart.value) {
        chartThroughput = echarts.init(throughputChart.value)
        chartThroughput.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value', name: '请求数' },
          series: [{ type: 'bar', data: [] }]
        })
      }

      if (nodeComparisonChart.value) {
        chartNodeComparison = echarts.init(nodeComparisonChart.value)
        chartNodeComparison.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          legend: { bottom: 10 },
          grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value', name: '延时(ms)' },
          series: [
            { name: '平均延时', type: 'bar', data: [] },
            { name: 'P95延时', type: 'bar', data: [] }
          ]
        })
      }

      if (distributionChart.value) {
        chartDistribution = echarts.init(distributionChart.value)
        chartDistribution.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { orient: 'vertical', left: 'left' },
          series: [{
            type: 'pie',
            radius: '50%',
            data: [],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        })
      }
    }

    const updateLatencyTrendChart = (data) => {
      if (!chartLatencyTrend || !data || !data.length) return
      const xData = data.map(item => item.time || item.timestamp || '')
      const avgData = data.map(item => item.avgLatency || item.avg_latency || 0)
      const p95Data = data.map(item => item.p95Latency || item.p95_latency || 0)
      const p99Data = data.map(item => item.p99Latency || item.p99_latency || 0)
      
      chartLatencyTrend.setOption({
        xAxis: { data: xData },
        series: [
          { name: '平均延时', data: avgData },
          { name: 'P95延时', data: p95Data },
          { name: 'P99延时', data: p99Data }
        ]
      })
    }

    const updateThroughputChart = (data) => {
      if (!chartThroughput || !data || !data.length) return
      const xData = data.map(item => item.time || item.timestamp || '')
      const yData = data.map(item => item.count || item.qps || 0)
      
      chartThroughput.setOption({
        xAxis: { data: xData },
        series: [{ name: '吞吐量', data: yData }]
      })
    }

    const updateNodeComparisonChart = (data) => {
      if (!chartNodeComparison || !data || !data.length) return
      const nodes = data.map(item => item.nodeName || item.node_name || '未知节点')
      const avgData = data.map(item => item.avgLatency || item.avg_latency || 0)
      const p95Data = data.map(item => item.p95Latency || item.p95_latency || 0)
      
      chartNodeComparison.setOption({
        xAxis: { data: nodes },
        series: [
          { name: '平均延时', data: avgData },
          { name: 'P95延时', data: p95Data }
        ]
      })
    }

    const updateDistributionChart = (data) => {
      if (!chartDistribution || !data || !data.length) return
      chartDistribution.setOption({
        series: [{ data: data }]
      })
    }

    const analyzeBottlenecks = (nodePerf) => {
      if (!nodePerf || !nodePerf.length) {
        bottleneckNodes.value = []
        return
      }
      
      bottleneckNodes.value = nodePerf.map(node => {
        const avgLatency = node.avgLatency || node.avg_latency || 0
        const maxLatency = node.maxLatency || node.max_latency || 0
        const p95Latency = node.p95Latency || node.p95_latency || 0
        const errorRate = node.errorRate || node.error_rate || 0
        
        let recommendation = '性能良好'
        if (avgLatency > 1000) {
          recommendation = '平均延时过高，建议优化查询或增加缓存'
        } else if (errorRate > 5) {
          recommendation = '错误率过高，建议检查日志排查问题'
        } else if (p95Latency > 2000) {
          recommendation = 'P95延时较高，建议关注长尾请求'
        }
        
        return {
          nodeName: node.nodeName || node.node_name || '未知节点',
          avgLatency: avgLatency,
          maxLatency: maxLatency,
          p95Latency: p95Latency,
          errorRate: errorRate,
          recommendation
        }
      }).sort((a, b) => b.avgLatency - a.avgLatency)
    }

    const destroyCharts = () => {
      if (chartLatencyTrend) chartLatencyTrend.dispose()
      if (chartThroughput) chartThroughput.dispose()
      if (chartNodeComparison) chartNodeComparison.dispose()
      if (chartDistribution) chartDistribution.dispose()
    }

    // 存储查询到的数据，用于导出报告
    const reportData = reactive({
      chainName: '',
      startTime: '',
      endTime: '',
      stats: null,
      nodePerf: [],
      trend: [],
      throughput: [],
      distribution: []
    })

    const exportReport = async () => {
      // 检查是否有数据
      if (!queryForm.chainId || !queryForm.timeRange || queryForm.timeRange.length < 2) {
        ElMessage.warning('请先查询数据后再导出报告')
        return
      }

      if (metrics.totalCount === 0) {
        ElMessage.warning('当前没有数据可导出，请先查询数据')
        return
      }

      try {
        ElMessage.info('正在生成报告，请稍候...')
        
        // 获取链路名称
        const selectedChain = chains.value.find(c => c.id === queryForm.chainId)
        const chainName = selectedChain ? selectedChain.chain_name : '未知链路'
        
        // 格式化时间
        const startTime = formatDateTimeString(queryForm.timeRange[0])
        const endTime = formatDateTimeString(queryForm.timeRange[1])
        
        // 获取图表图片（base64）
        const chartImages = {}
        if (chartLatencyTrend) {
          chartImages.latencyTrend = chartLatencyTrend.getDataURL({
            type: 'png',
            pixelRatio: 2,
            backgroundColor: '#fff'
          })
        }
        if (chartThroughput) {
          chartImages.throughput = chartThroughput.getDataURL({
            type: 'png',
            pixelRatio: 2,
            backgroundColor: '#fff'
          })
        }
        if (chartNodeComparison) {
          chartImages.nodeComparison = chartNodeComparison.getDataURL({
            type: 'png',
            pixelRatio: 2,
            backgroundColor: '#fff'
          })
        }
        if (chartDistribution) {
          chartImages.distribution = chartDistribution.getDataURL({
            type: 'png',
            pixelRatio: 2,
            backgroundColor: '#fff'
          })
        }
        
        // 生成HTML报告
        const htmlContent = generateReportHTML({
          chainName,
          startTime,
          endTime,
          metrics,
          bottleneckNodes: bottleneckNodes.value,
          detailData: detailData.value,
          chartImages,
          granularity: queryForm.granularity
        })
        
        // 下载HTML文件
        downloadHTML(htmlContent, `链路性能分析报告_${chainName}_${startTime.replace(/[: ]/g, '-')}_${endTime.replace(/[: ]/g, '-')}.html`)
        
        ElMessage.success('报告导出成功')
      } catch (error) {
        console.error('导出报告失败:', error)
        ElMessage.error('导出报告失败：' + (error.message || '未知错误'))
      }
    }

    // 生成HTML报告内容
    const generateReportHTML = (data) => {
      const { chainName, startTime, endTime, metrics, bottleneckNodes, detailData, chartImages, granularity } = data
      
      const formatTableRow = (row) => {
        const requestId = row.requestId || row.request_id || '-'
        const nodeName = row.nodeName || row.node_name || '-'
        const latency = row.latency || 0
        const requestStartTime = formatTime(row.requestStartTime || row.request_start_time)
        const requestEndTime = formatTime(row.requestEndTime || row.request_end_time)
        const success = (row.success === 1 || row.success === true) ? '成功' : '失败'
        const successClass = (row.success === 1 || row.success === true) ? 'success' : 'danger'
        
        return `
          <tr>
            <td>${requestId}</td>
            <td>${nodeName}</td>
            <td>${latency}</td>
            <td>${requestStartTime}</td>
            <td>${requestEndTime}</td>
            <td class="${successClass}">${success}</td>
          </tr>
        `
      }
      
      const bottleneckRows = bottleneckNodes.map(node => `
        <tr>
          <td>${node.nodeName || '未知节点'}</td>
          <td>${node.avgLatency || 0}</td>
          <td>${node.maxLatency || 0}</td>
          <td>${node.p95Latency || 0}</td>
          <td>${node.errorRate || 0}%</td>
          <td>${node.recommendation || '-'}</td>
        </tr>
      `).join('')
      
      const detailRows = detailData.map(formatTableRow).join('')
      
      return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>链路性能分析报告 - ${chainName}</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
      line-height: 1.6;
      color: #333;
      background: #f5f5f5;
      padding: 20px;
    }
    .container {
      max-width: 1200px;
      margin: 0 auto;
      background: #fff;
      padding: 30px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.1);
    }
    h1 {
      color: #409EFF;
      border-bottom: 3px solid #409EFF;
      padding-bottom: 10px;
      margin-bottom: 30px;
    }
    h2 {
      color: #606266;
      margin-top: 30px;
      margin-bottom: 15px;
      padding-left: 10px;
      border-left: 4px solid #409EFF;
    }
    .info-section {
      background: #f8f9fa;
      padding: 20px;
      border-radius: 4px;
      margin-bottom: 30px;
    }
    .info-row {
      display: flex;
      margin-bottom: 10px;
    }
    .info-label {
      font-weight: bold;
      width: 120px;
      color: #606266;
    }
    .info-value {
      color: #303133;
    }
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 30px;
    }
    .metric-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      padding: 20px;
      border-radius: 8px;
      text-align: center;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }
    .metric-card:nth-child(1) {
      background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
    }
    .metric-card:nth-child(2) {
      background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
    }
    .metric-card:nth-child(3) {
      background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
    }
    .metric-card:nth-child(4) {
      background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
    }
    .metric-value {
      font-size: 32px;
      font-weight: bold;
      margin: 10px 0;
    }
    .metric-label {
      font-size: 14px;
      opacity: 0.9;
    }
    .chart-container {
      margin: 30px 0;
      text-align: center;
    }
    .chart-container img {
      max-width: 100%;
      height: auto;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.1);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 20px 0;
      background: #fff;
    }
    th, td {
      padding: 12px;
      text-align: left;
      border: 1px solid #e4e7ed;
    }
    th {
      background: #f5f7fa;
      font-weight: bold;
      color: #606266;
    }
    tr:nth-child(even) {
      background: #fafafa;
    }
    tr:hover {
      background: #f0f9ff;
    }
    .success {
      color: #67C23A;
      font-weight: bold;
    }
    .danger {
      color: #F56C6C;
      font-weight: bold;
    }
    .footer {
      margin-top: 50px;
      padding-top: 20px;
      border-top: 1px solid #e4e7ed;
      text-align: center;
      color: #909399;
      font-size: 12px;
    }
    @media print {
      body {
        background: #fff;
        padding: 0;
      }
      .container {
        box-shadow: none;
        padding: 20px;
      }
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>链路性能分析报告</h1>
    
    <div class="info-section">
      <div class="info-row">
        <span class="info-label">链路名称：</span>
        <span class="info-value">${chainName}</span>
      </div>
      <div class="info-row">
        <span class="info-label">分析时间范围：</span>
        <span class="info-value">${startTime} 至 ${endTime}</span>
      </div>
      <div class="info-row">
        <span class="info-label">时间粒度：</span>
        <span class="info-value">${granularity === 'minute' ? '分钟' : granularity === '5minute' ? '5分钟' : '小时'}</span>
      </div>
      <div class="info-row">
        <span class="info-label">报告生成时间：</span>
        <span class="info-value">${new Date().toLocaleString('zh-CN')}</span>
      </div>
    </div>
    
    <h2>性能概览</h2>
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-label">平均延时</div>
        <div class="metric-value">${metrics.avgLatency || 0}</div>
        <div class="metric-label">ms</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">成功率</div>
        <div class="metric-value">${metrics.successRate || 0}</div>
        <div class="metric-label">%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">P95延时</div>
        <div class="metric-value">${metrics.p95Latency || 0}</div>
        <div class="metric-label">ms</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">总请求数</div>
        <div class="metric-value">${metrics.totalCount || 0}</div>
        <div class="metric-label">次</div>
      </div>
    </div>
    
    ${chartImages.latencyTrend ? `
    <h2>延时趋势分析</h2>
    <div class="chart-container">
      <img src="${chartImages.latencyTrend}" alt="延时趋势图">
    </div>
    ` : ''}
    
    ${chartImages.throughput ? `
    <h2>吞吐量趋势</h2>
    <div class="chart-container">
      <img src="${chartImages.throughput}" alt="吞吐量趋势图">
    </div>
    ` : ''}
    
    ${chartImages.nodeComparison ? `
    <h2>节点性能对比</h2>
    <div class="chart-container">
      <img src="${chartImages.nodeComparison}" alt="节点性能对比图">
    </div>
    ` : ''}
    
    ${chartImages.distribution ? `
    <h2>延时分布</h2>
    <div class="chart-container">
      <img src="${chartImages.distribution}" alt="延时分布图">
    </div>
    ` : ''}
    
    ${bottleneckNodes.length > 0 ? `
    <h2>瓶颈节点分析</h2>
    <table>
      <thead>
        <tr>
          <th>节点名称</th>
          <th>平均延时(ms)</th>
          <th>最大延时(ms)</th>
          <th>P95延时(ms)</th>
          <th>错误率(%)</th>
          <th>优化建议</th>
        </tr>
      </thead>
      <tbody>
        ${bottleneckRows}
      </tbody>
    </table>
    ` : ''}
    
    ${detailData.length > 0 ? `
    <h2>详细数据</h2>
    <table>
      <thead>
        <tr>
          <th>请求ID</th>
          <th>节点</th>
          <th>延时(ms)</th>
          <th>开始时间</th>
          <th>结束时间</th>
          <th>状态</th>
        </tr>
      </thead>
      <tbody>
        ${detailRows}
      </tbody>
    </table>
    ` : ''}
    
    <div class="footer">
      <p>本报告由 Easy JMeter 链路延迟监控系统自动生成</p>
      <p>生成时间：${new Date().toLocaleString('zh-CN')}</p>
    </div>
  </div>
</body>
</html>`
    }

    // 下载HTML文件
    const downloadHTML = (content, filename) => {
      const blob = new Blob([content], { type: 'text/html;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }

    const formatTime = (time) => {
      if (!time) return '-'
      try {
        // 处理多种时间格式
        if (typeof time === 'string') {
          // 处理 ISO 格式或标准格式
          const timeStr = time.replace('T', ' ').substring(0, 19)
          return new Date(timeStr).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
          })
        }
        return new Date(time).toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      } catch (e) {
        return time.toString()
      }
    }

    return {
      chains,
      queryForm,
      loading,
      metrics,
      detailData,
      bottleneckNodes,
      pagination,
      latencyTrendChart,
      throughputChart,
      nodeComparisonChart,
      distributionChart,
      query,
      exportReport,
      formatTime,
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 0 30px 30px;
}
</style>
