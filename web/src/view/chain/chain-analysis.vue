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
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss">
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

    const setDefaultTimeRange = () => {
      const end = new Date()
      const start = new Date(end.getTime() - 3600000) // 最近1小时
      queryForm.timeRange = [
        start.toISOString().slice(0, 19).replace('T', ' '),
        end.toISOString().slice(0, 19).replace('T', ' ')
      ]
    }

    const query = async () => {
      if (!queryForm.chainId || !queryForm.timeRange || queryForm.timeRange.length < 2) {
        ElMessage.warning('请选择链路和时间范围')
        return
      }

      try {
        loading.value = true
        
        // 格式化时间
        const formatTime = (time) => {
          if (!time) return ''
          if (typeof time === 'string') {
            return time.replace('T', ' ').substring(0, 19)
          }
          return time.toISOString().slice(0, 19).replace('T', ' ')
        }
        
        const startTime = formatTime(queryForm.timeRange[0])
        const endTime = formatTime(queryForm.timeRange[1])
        
        // 获取统计数据
        const stats = await get(`/v1/chain/collection/chain-stats/${queryForm.chainId}`, {
          startTime: startTime,
          endTime: endTime
        }, { showBackend: true })
        
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

    const exportReport = () => {
      ElMessage.info('报告导出功能开发中...')
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
