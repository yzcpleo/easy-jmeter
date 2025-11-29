<template>
  <div class="container">
    <!-- 查询表单 -->
    <el-card>
      <el-form :model="queryForm" inline>
        <el-form-item label="链路">
          <el-select v-model="queryForm.chainId" placeholder="选择链路" style="width: 200px;" @change="handleChainChange">
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
        <el-form-item>
          <el-button type="primary" @click="query" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 链路架构图 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>链路架构图</span>
      </template>
      <div ref="topologyChart" style="width: 100%; height: 600px;"></div>
    </el-card>

    <!-- 路径时延统计 -->
    <el-card style="margin-top: 20px;" v-if="pathStats.length > 0">
      <template #header>
        <span>路径时延统计</span>
      </template>
      <el-table :data="pathStats" border>
        <el-table-column prop="pathName" label="路径名称" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="totalRequests" label="总请求数" width="120"></el-table-column>
        <el-table-column prop="avgLatency" label="平均时延(ms)" width="120">
          <template #default="scope">
            {{ scope.row.avgLatency ? scope.row.avgLatency.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="p95Latency" label="P95时延(ms)" width="120">
          <template #default="scope">
            {{ scope.row.p95Latency ? scope.row.p95Latency.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="p99Latency" label="P99时延(ms)" width="120">
          <template #default="scope">
            {{ scope.row.p99Latency ? scope.row.p99Latency.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="maxLatency" label="最大时延(ms)" width="120"></el-table-column>
        <el-table-column prop="successRate" label="成功率(%)" width="120">
          <template #default="scope">
            {{ scope.row.successRate ? scope.row.successRate.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { onMounted, ref, onBeforeUnmount } from 'vue'
import { get } from '@/lin/plugin/axios'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getPathsLatencyStats } from '@/api/chain'

export default {
  name: 'ChainTopology',
  setup() {
    const loading = ref(false)
    const chains = ref([])
    const nodes = ref([])
    const paths = ref([])
    const pathStats = ref([])
    const topologyChart = ref(null)
    let chartInstance = null
    const queryForm = ref({
      chainId: null,
      timeRange: null
    })

    onMounted(() => {
      loadChains()
      initChart()
    })

    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.dispose()
      }
    })

    const loadChains = async () => {
      try {
        const res = await get('/v1/chain/trace/page', { page: 0, count: 1000 }, { showBackend: true })
        chains.value = res.items || []
      } catch (error) {
        console.error('获取链路列表失败:', error)
        chains.value = []
      }
    }

    const loadNodes = async (chainId) => {
      if (!chainId) {
        nodes.value = []
        return
      }
      try {
        const res = await get('/v1/chain/node/chain/' + chainId, {}, { showBackend: true })
        nodes.value = Array.isArray(res) ? res : (res.data || res || [])
      } catch (error) {
        console.error('获取节点列表失败:', error)
        nodes.value = []
      }
    }

    const loadPaths = async (chainId) => {
      if (!chainId) {
        paths.value = []
        return
      }
      try {
        const res = await get('/v1/chain/metric-path/chain/' + chainId, {}, { showBackend: true })
        paths.value = Array.isArray(res) ? res : (res.data || res || [])
      } catch (error) {
        console.error('获取路径列表失败:', error)
        paths.value = []
      }
    }

    const query = async () => {
      if (!queryForm.value.chainId) {
        ElMessage.warning('请先选择链路')
        return
      }
      if (!queryForm.value.timeRange || queryForm.value.timeRange.length < 2) {
        ElMessage.warning('请选择时间范围')
        return
      }

      try {
        loading.value = true
        await loadNodes(queryForm.value.chainId)
        await loadPaths(queryForm.value.chainId)
        
        // 格式化时间
        const startTime = formatDateTimeString(queryForm.value.timeRange[0])
        const endTime = formatDateTimeString(queryForm.value.timeRange[1])
        
        // 获取路径时延统计
        if (paths.value.length > 0) {
          const stats = await getPathsLatencyStats(queryForm.value.chainId, startTime, endTime)
          pathStats.value = Array.isArray(stats) ? stats : (stats.data || stats || [])
        } else {
          pathStats.value = []
        }
        
        updateTopologyChart()
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('查询失败:', error)
        ElMessage.error('查询失败: ' + (error.message || '未知错误'))
      }
    }

    const handleChainChange = () => {
      nodes.value = []
      paths.value = []
      pathStats.value = []
      if (queryForm.value.chainId) {
        loadNodes(queryForm.value.chainId)
        loadPaths(queryForm.value.chainId)
      }
      updateTopologyChart()
    }

    const formatDateTimeString = (date) => {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }

    const initChart = () => {
      if (topologyChart.value) {
        chartInstance = echarts.init(topologyChart.value)
        window.addEventListener('resize', () => {
          if (chartInstance) {
            chartInstance.resize()
          }
        })
      }
    }

    const updateTopologyChart = () => {
      if (!chartInstance || !nodes.value || nodes.value.length === 0) {
        return
      }

      // 构建节点数据
      const nodeData = nodes.value.map((node, index) => {
        // 简单的布局：按顺序排列
        const angle = (index * 2 * Math.PI) / nodes.value.length
        const radius = 200
        return {
          id: node.id,
          name: node.node_name,
          value: node.node_name,
          x: Math.cos(angle) * radius,
          y: Math.sin(angle) * radius,
          symbolSize: 50,
          itemStyle: {
            color: '#409EFF'
          },
          label: {
            show: true,
            position: 'inside'
          }
        }
      })

      // 构建连接数据（基于路径配置）
      const links = []
      const pathMap = new Map()
      
      paths.value.forEach(path => {
        const startNode = nodes.value.find(n => n.id === path.startNodeId)
        const endNode = nodes.value.find(n => n.id === path.endNodeId)
        if (startNode && endNode) {
          const key = `${path.startNodeId}-${path.endNodeId}`
          if (!pathMap.has(key)) {
            pathMap.set(key, {
              source: path.startNodeId,
              target: path.endNodeId,
              value: path.metricName,
              lineStyle: {
                width: 2,
                curveness: 0.3
              },
              label: {
                show: true,
                formatter: path.metricName
              }
            })
            links.push(pathMap.get(key))
          }
        }
      })

      // 如果没有路径配置，根据节点顺序创建连接
      if (links.length === 0 && nodes.value.length > 1) {
        for (let i = 0; i < nodes.value.length - 1; i++) {
          links.push({
            source: nodes.value[i].id,
            target: nodes.value[i + 1].id,
            value: '',
            lineStyle: {
              width: 2,
              curveness: 0.3
            }
          })
        }
      }

      // 更新图表
      chartInstance.setOption({
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            if (params.dataType === 'node') {
              return `${params.data.name}<br/>节点ID: ${params.data.id}`
            } else if (params.dataType === 'edge') {
              return `${params.data.value || '连接'}`
            }
            return ''
          }
        },
        series: [{
          type: 'graph',
          layout: 'none',
          data: nodeData,
          links: links,
          roam: true,
          label: {
            show: true,
            position: 'right',
            formatter: '{b}'
          },
          lineStyle: {
            color: 'source',
            curveness: 0.3
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: {
              width: 4
            }
          }
        }]
      })
    }

    return {
      loading,
      chains,
      nodes,
      paths,
      pathStats,
      topologyChart,
      queryForm,
      query,
      handleChainChange
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}
</style>

