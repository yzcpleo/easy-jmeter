<template>
  <div class="container">
    <el-row :gutter="20">
      <!-- 左侧配置 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>数据收集配置</span>
          </template>
          <el-form :model="collectionForm" label-width="100px">
            <el-form-item label="选择链路">
              <el-select 
                v-model="collectionForm.chainId" 
                placeholder="请选择链路"
                @change="handleChainChange"
                style="width: 100%">
                <el-option
                  v-for="chain in chains"
                  :key="chain.id"
                  :label="chain.chain_name"
                  :value="chain.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="任务ID">
              <el-input v-model="collectionForm.taskId" placeholder="输入任务ID"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="startCollection"
                :loading="starting"
                :disabled="!collectionForm.chainId || !collectionForm.taskId">
                <el-icon><VideoPlay /></el-icon>
                启动收集
              </el-button>
              <el-button 
                type="danger" 
                @click="stopCollection"
                :loading="stopping"
                :disabled="!isCollecting">
                <el-icon><VideoPause /></el-icon>
                停止收集
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button 
                type="success" 
                @click="viewHistoryData"
                :loading="loadingHistory"
                :disabled="!collectionForm.chainId"
                style="width: 100%">
                <el-icon><Search /></el-icon>
                查看历史数据
              </el-button>
            </el-form-item>
          </el-form>

          <el-divider>收集状态</el-divider>
          <div v-if="collectionStatus" class="status-info">
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(collectionStatus.status)">
                  {{ collectionStatus.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ formatTime(collectionStatus.startTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="总记录数">
                {{ collectionStatus.totalRecords || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="成功">
                <span style="color: #67C23A;">{{ collectionStatus.successRecords || 0 }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="错误">
                <span style="color: #F56C6C;">{{ collectionStatus.errorRecords || 0 }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-else class="status-empty">
            <el-icon :size="40" color="#909399"><InfoFilled /></el-icon>
            <p>暂无收集任务</p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧实时数据 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between;">
              <span>实时数据</span>
              <el-button text @click="refreshData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          
          <!-- 图表 -->
          <div ref="latencyChart" style="width: 100%; height: 300px;"></div>

          <!-- 最新数据表格 -->
          <el-divider>最新数据记录</el-divider>
          <el-table :data="latestData" size="small" max-height="400">
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
            <el-table-column prop="latency" label="延时(ms)" width="100">
              <template #default="scope">
                <span :class="getLatencyClass(scope.row.latency)">
                  {{ scope.row.latency || 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="scope">
                {{ formatTime(scope.row.requestStartTime || scope.row.request_start_time) }}
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
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { onMounted, ref, reactive, onBeforeUnmount } from 'vue'
import { get } from '@/lin/plugin/axios'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, Refresh, InfoFilled, Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { startCollection, stopCollection, getCollectionStatus, getLatencyData } from '@/api/chain'

export default {
  name: 'ChainCollection',
  components: {
    VideoPlay,
    VideoPause,
    Refresh,
    InfoFilled,
    Search,
  },
  setup() {
    const chains = ref([])
    const starting = ref(false)
    const stopping = ref(false)
    const isCollecting = ref(false)
    const loadingHistory = ref(false)
    const collectionStatus = ref(null)
    const latestData = ref([])
    const latencyChart = ref(null)
    let chart = null
    let refreshTimer = null

    const collectionForm = reactive({
      chainId: null,
      taskId: ''
    })

    onMounted(() => {
      getChains()
      initChart()
      // 每30秒刷新一次
      refreshTimer = setInterval(() => {
        if (isCollecting.value) {
          refreshData()
        }
      }, 30000)
    })

    onBeforeUnmount(() => {
      if (refreshTimer) clearInterval(refreshTimer)
      if (chart) chart.dispose()
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

    const handleChainChange = () => {
      collectionStatus.value = null
      isCollecting.value = false
    }

    const startCollectionHandler = async () => {
      try {
        starting.value = true
        await startCollection({
          taskId: collectionForm.taskId,
          chainId: collectionForm.chainId
        })
        ElMessage.success('数据收集已启动')
        isCollecting.value = true
        starting.value = false
        refreshData()
      } catch (error) {
        starting.value = false
        console.error('启动失败:', error)
        ElMessage.error('启动失败: ' + (error.message || '未知错误'))
      }
    }

    const stopCollectionHandler = async () => {
      try {
        stopping.value = true
        await stopCollection({
          taskId: collectionForm.taskId,
          chainId: collectionForm.chainId
        })
        ElMessage.success('数据收集已停止')
        isCollecting.value = false
        stopping.value = false
        refreshData()
      } catch (error) {
        stopping.value = false
        console.error('停止失败:', error)
        ElMessage.error('停止失败: ' + (error.message || '未知错误'))
      }
    }

    const refreshData = async () => {
      if (!collectionForm.chainId || !collectionForm.taskId) return

      try {
        // 获取收集状态
        const status = await getCollectionStatus(collectionForm.taskId, collectionForm.chainId)
        collectionStatus.value = status

        // 获取最新数据
        const data = await getLatencyData({
          chainId: collectionForm.chainId,
          taskId: collectionForm.taskId,
          page: 0,
          count: 20
        })
        // 处理数据格式，兼容下划线和驼峰命名
        latestData.value = (data.items || []).map(item => ({
          ...item,
          requestId: item.requestId || item.request_id,
          nodeName: item.nodeName || item.node_name,
          requestStartTime: item.requestStartTime || item.request_start_time,
          requestEndTime: item.requestEndTime || item.request_end_time,
          collectionTime: item.collectionTime || item.collection_time
        }))
        updateChart()
      } catch (error) {
        console.error('刷新数据失败:', error)
      }
    }

    const viewHistoryData = async () => {
      if (!collectionForm.chainId) {
        ElMessage.warning('请先选择链路')
        return
      }

      try {
        loadingHistory.value = true
        
        // 直接查询历史数据，不需要任务ID和收集状态
        const data = await getLatencyData({
          chainId: collectionForm.chainId,
          taskId: collectionForm.taskId || undefined,
          page: 0,
          count: 50
        })
        
        // 处理数据格式，兼容下划线和驼峰命名
        latestData.value = (data.items || []).map(item => ({
          ...item,
          requestId: item.requestId || item.request_id,
          nodeName: item.nodeName || item.node_name,
          requestStartTime: item.requestStartTime || item.request_start_time,
          requestEndTime: item.requestEndTime || item.request_end_time,
          collectionTime: item.collectionTime || item.collection_time
        }))
        
        if (latestData.value.length === 0) {
          ElMessage.info('暂无历史数据')
        } else {
          ElMessage.success(`加载了 ${latestData.value.length} 条历史数据`)
          updateChart()
        }
        
        loadingHistory.value = false
      } catch (error) {
        loadingHistory.value = false
        console.error('加载历史数据失败:', error)
        ElMessage.error('加载历史数据失败')
      }
    }

    const initChart = () => {
      if (!latencyChart.value) return
      chart = echarts.init(latencyChart.value)
      chart.setOption({
        title: { text: '节点延时监控', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value', name: '延时(ms)' },
        series: []
      })
    }

    const updateChart = () => {
      if (!chart || latestData.value.length === 0) return

      // 按节点分组
      const nodeData = {}
      latestData.value.forEach(item => {
        const nodeName = item.nodeName || item.node_name || '未知节点'
        if (!nodeData[nodeName]) {
          nodeData[nodeName] = []
        }
        nodeData[nodeName].push(item.latency || 0)
      })

      const series = Object.keys(nodeData).map(nodeName => ({
        name: nodeName || '未知节点',
        type: 'line',
        data: nodeData[nodeName]
      }))

      const xData = latestData.value.map((_, index) => index + 1)

      chart.setOption({
        xAxis: { data: xData },
        series: series
      })
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

    const getStatusType = (status) => {
      const typeMap = {
        'COLLECTING': 'success',
        'STOPPED': 'info',
        'ERROR': 'danger'
      }
      return typeMap[status] || 'info'
    }

    const getLatencyClass = (latency) => {
      if (latency < 100) return 'latency-good'
      if (latency < 500) return 'latency-warning'
      return 'latency-danger'
    }

    return {
      chains,
      collectionForm,
      starting,
      stopping,
      isCollecting,
      loadingHistory,
      collectionStatus,
      latestData,
      latencyChart,
      handleChainChange,
      startCollection: startCollectionHandler,
      stopCollection: stopCollectionHandler,
      refreshData,
      viewHistoryData,
      formatTime,
      getStatusType,
      getLatencyClass,
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 0 30px 30px;

  .status-info {
    padding: 10px 0;
  }

  .status-empty {
    text-align: center;
    padding: 40px 0;
    color: #909399;
  }

  .latency-good {
    color: #67C23A;
    font-weight: bold;
  }

  .latency-warning {
    color: #E6A23C;
    font-weight: bold;
  }

  .latency-danger {
    color: #F56C6C;
    font-weight: bold;
  }
}
</style>
