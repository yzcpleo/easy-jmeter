<template>
  <div class="container">
    <el-row :gutter="25">
      <el-col :span="6">
        <div class="first-head">
          <el-text type="primary" truncated class="title-hello">{{hello}}！ {{ name }}</el-text>
          <el-text type="info" class="title-info">欢迎使用性能测试平台</el-text>
        </div>
      </el-col>
      <el-col :span="18">
        <div class="first-head-total">
          <div class="total-item">
            <div class="total-item-title">项目数</div>
            <div class="total-item-value">{{totalCount.projectNum}}</div>
          </div>
          <div class="total-item">
            <div class="total-item-title">用例数</div>
            <div class="total-item-value">{{totalCount.caseNum}}</div>
          </div>
          <div class="total-item">
            <div class="total-item-title">压力机数</div>
            <div class="total-item-value">{{totalCount.machineNum}}</div>
          </div>
          <div class="total-item">
            <div class="total-item-title">测试次数</div>
            <div class="total-item-value">{{totalCount.taskNum}}</div>
          </div>
          <div class="total-item">
            <div class="total-item-title">压测总时长</div>
            <div class="total-item-value">{{timeDeal(totalCount.durationSum)}}</div>
          </div>
          <div class="total-item">
            <div class="total-item-title">总请求数</div>
            <div class="total-item-value">{{totalCount.totalSamples}}</div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="25">
      <el-col :span="6">
        <div class="box case-list-box">
          <div class="case-list-header">
            <div class="header-title">
              <i class="iconfont icon-list"></i>
              <span>用例列表</span>
              <el-badge :value="cases.length" class="case-count-badge" type="primary" />
            </div>
          </div>
          <div class="case-search">
            <el-input 
              placeholder="搜索用例..." 
              v-model="caseName" 
              clearable 
              prefix-icon="Search">
            </el-input>
          </div>
          <div class="case-list">
            <div 
              class="case-item" 
              v-for="item in displayedCases" 
              :key="item.id" 
              :class="{'case-item-active': item.is_choose}" 
              @click="handleChooseCase(item)">
              <div class="case-item-content">
                <i class="iconfont icon-file-text case-icon"></i>
                <span class="case-name">{{ item.name }}</span>
              </div>
              <i class="iconfont icon-right case-arrow" v-if="item.is_choose"></i>
            </div>
            
            <div class="load-more" v-if="cases.length > displayLimit && displayedCases.length < cases.length">
              <el-button text type="primary" @click="loadMore">
                <i class="iconfont icon-down"></i>
                加载更多 (剩余 {{ cases.length - displayedCases.length }} 个)
              </el-button>
            </div>
            
            <div class="case-info" v-if="cases.length > 50 && !caseName">
              <i class="iconfont icon-info-circle"></i>
              <p>用例较多，建议使用搜索功能快速查找</p>
            </div>
            
            <div class="case-empty" v-if="cases.length === 0">
              <i class="iconfont icon-inbox"></i>
              <p>{{ caseName ? '未找到匹配的用例' : '暂无用例' }}</p>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="18">
        <div class="box">
          <div v-if="selected" class="statistics">
            <div class="first-head-total">
              <div class="total-item-case">
                <div class="total-item-title-case">平均响应时间</div>
                <div class="total-item-value-case" v-if="parseFloat(statisticsData.average_response_time)">{{parseFloat(statisticsData.average_response_time).toFixed(2)}}</div>
                <div class="total-item-value-case" v-else>--</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">90th响应时间</div>
                <div class="total-item-value-case" v-if="parseFloat(statisticsData.average90th_response_time)">{{parseFloat(statisticsData.average90th_response_time).toFixed(2)}}</div>
                <div class="total-item-value-case" v-else>--</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">平均错误率</div>
                <div class="total-item-value-case" v-if="parseFloat(statisticsData.average_error_rate)">{{parseFloat(statisticsData.average_error_rate).toFixed(2)}}%</div>
                <div class="total-item-value-case" v-else>--</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">平均吞吐量</div>
                <div class="total-item-value-case" v-if="parseFloat(statisticsData.average_throughput)">{{parseFloat(statisticsData.average_throughput).toFixed(2)}}</div>
                <div class="total-item-value-case" v-else>--</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">测试次数</div>
                <div class="total-item-value-case">{{statisticsData.task_num}}</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">压测总时长</div>
                <div class="total-item-value-case">{{timeDeal(statisticsData.duration_sum)}}</div>
              </div>
              <div class="total-item-case">
                <div class="total-item-title-case">总请求数</div>
                <div class="total-item-value-case">{{statisticsData.samples_sum}}</div>
              </div>
            </div>
            <div id="respoonseTimesChart"  class="report-echarts"></div>
            <div id="throughputAndErrorChart"  class="report-echarts"></div>
          </div>
          <div v-else class="statistics"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed, watch, getCurrentInstance } from 'vue'
import { get } from '@/lin/plugin/axios'
import Utils from 'lin/util/util'

export default {
  setup() {
    const hello = ref('')
    const name = ref('')
    const totalCount = ref({"caseNum": 0,"machineNum": 0,"durationSum": 0,"projectNum": 0,"taskNum": 0})
    const caseName = ref('')
    const cases = ref([])
    const casesOriginal = ref([])
    const selected = ref(null)
    const statisticsData = ref([])
    const displayLimit = ref(50)
    const {proxy} = getCurrentInstance()
    
    // 计算属性：返回限制数量的用例列表
    const displayedCases = computed(() => {
      return cases.value.slice(0, displayLimit.value)
    })

    onMounted(() => {
      getGreeting()
      getUserInfo()
      getTotalCount()
      getCases()
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', resizeHandler)
    })

    const getUserInfo = async () => {
      let res
      try {
        res = await get('/cms/user/information', { showBackend: true })
        if (res.nickname !== null) {
          name.value = res.nickname
        } else {
          name.value = res.username
        }
      } catch (error) {
      }
    }

    const getTotalCount = async () => {
      let res
      try {
        res = await get('/v1/common/total', { showBackend: true })
        totalCount.value = res
      } catch (error) {
      }
    }

    const getCases = async () => {
      let res
      try {
        res = await get('/v1/case', { showBackend: true })
        for (let i = 0; i < res.length; i++) {
          res[i].is_choose = false
        }
        casesOriginal.value = res
        cases.value = res
        if (cases.value) {
          handleChooseCase(cases.value[0])
        }
      } catch (error) {
        casesOriginal.value = []
      }
    }

    const handleChooseCase = (item) => {
      selected.value = item.id
      for (let i = 0; i < cases.value.length; i++) {
        if (cases.value[i].id === item.id) {
          cases.value[i].is_choose = true
        } else {
          cases.value[i].is_choose = false
        }
      }
      for (let i = 0; i < casesOriginal.value.length; i++) {
        if (casesOriginal.value[i].id === item.id) {
          casesOriginal.value[i].is_choose = true
        } else {
          casesOriginal.value[i].is_choose = false
        }
      }
    }

    const searchCases = () => {
      cases.value = []
      for (let i = 0; i < casesOriginal.value.length; i++) {
        if (casesOriginal.value[i].name.includes(caseName.value, { ignoreCase: true })) {
          cases.value.push(casesOriginal.value[i])
        }
      }
      // 搜索时重置显示限制
      displayLimit.value = 50
    }
    
    const loadMore = () => {
      // 每次加载更多 50 个
      displayLimit.value = Math.min(displayLimit.value + 50, cases.value.length)
    }

    const getStatisticsById = async () => {
      let res
      try {
        res = await get(`/v1/common/statistics/${selected.value}` , { showBackend: true })
        statisticsData.value = res
        // 确保数据完整后才初始化图表
        if (res && res.graph_data && res.graph_data.responseTimeInfos && res.graph_data.throughputAndErrorInfos) {
          setTimeout( function(){
            initChart()
            setChartOption()
          }, 500 )
        }
      } catch (error) {
        console.error('Failed to load statistics:', error)
      }
    }

    const initChart = () => {
      let respoonseTimesEChart = proxy.$echarts.getInstanceByDom(document.getElementById('respoonseTimesChart'))
      if (respoonseTimesEChart == null) {
        respoonseTimesEChart= proxy.$echarts.init(document.getElementById('respoonseTimesChart'))
      }
      let throughputAndErrorEChart = proxy.$echarts.getInstanceByDom(document.getElementById('throughputAndErrorChart'))
      if (throughputAndErrorEChart == null) {
        throughputAndErrorEChart= proxy.$echarts.init(document.getElementById('throughputAndErrorChart'))
      }
      window.addEventListener("resize", resizeHandler)
    }

    const resizeHandler = () => {
      let respoonseTimesEChart = proxy.$echarts.getInstanceByDom(document.getElementById('respoonseTimesChart'))
      if (respoonseTimesEChart !== null) {
        respoonseTimesEChart.resize()
      }
      let throughputAndErrorEChart = proxy.$echarts.getInstanceByDom(document.getElementById('throughputAndErrorChart'))
      if (throughputAndErrorEChart !== null) {
        throughputAndErrorEChart.resize()
      }
    }

    const setChartOption = () => {
      let responseTimesOption = getOption("responseTimeInfos")
      let respoonseTimesEChart = proxy.$echarts.getInstanceByDom(document.getElementById('respoonseTimesChart'))
      respoonseTimesEChart.setOption(responseTimesOption, true)

      let throughputAndErrorOption = getOption("throughputAndErrorInfos")
      let throughputAndErrorEChart = proxy.$echarts.getInstanceByDom(document.getElementById('throughputAndErrorChart'))
      throughputAndErrorEChart.setOption(throughputAndErrorOption, true)
    }

    const getOption = (infos) => {
      const option = {
        title: {
          text: "",
          left: "center",
        },
        grid: {
          left: '3%',
          right: '2%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          axisLabel: {
            interval: 'auto',
            rotate: 15,
          },
          data: []

        },
        yAxis: {
          name: '',
          scale:true
        },
        tooltip: {
          trigger: 'axis'
        },
        toolbox: {
          show: true,
          top: 20,
          feature: {
            dataZoom: {
              yAxisIndex: 'none',
              title: {
                zoom: '区域缩放',
                back: '缩放还原'
              },
            },
          }
        },
        legend: {
          data: [],
          bottom: -5,
        },
        series: [],
      }
      
      // 安全检查：确保数据存在
      if (!statisticsData.value || !statisticsData.value.graph_data || !statisticsData.value.graph_data[infos]) {
        console.warn('Statistics data not available for:', infos)
        return option
      }
      
      option.series = statisticsData.value.graph_data[infos].series
      option.title.text = statisticsData.value.graph_data[infos].titleCN
      option.legend.data = statisticsData.value.graph_data[infos].labels
      option.yAxis.name = statisticsData.value.graph_data[infos].yName
      option.xAxis.data = statisticsData.value.graph_data[infos].times
      return option
    }

    const _debounce =Utils.debounce(()=>{
      searchCases()
    }, 800)

    watch(caseName, () => {
      _debounce()
    })

    watch(selected, () => {
      getStatisticsById()
    })

    const getGreeting = () => {
      const currentTime = new Date();
      const currentHour = currentTime.getHours();
      if (currentHour < 6) {
        hello.value = '凌晨好'
      } else if (currentHour < 9) {
        hello.value = '早上好'
      } else if (currentHour < 12) {
        hello.value = '上午好'
      } else if (currentHour < 14) {
        hello.value = '中午好'
      } else if (currentHour < 18) {
        hello.value = '下午好'
      } else if (currentHour < 24) {
        hello.value = '晚上好'
      } else {
        hello.value = '你好'
      }
    }

    const timeDeal = computed(() => (time) => {
      const hours = Math.floor(time / 3600)
      const minutes = Math.floor((time % 3600) / 60)
      const seconds = time % 60
      let result = ""
      if (hours > 0) {
        result += `${hours}小时`
      }
      if (hours < 1) {
        if (minutes > 0) {
          result += `${minutes}分钟`
        }
        if (seconds > 0 || result === "") {
          result += `${seconds}秒`
        }
      } else {
        if (minutes > 0) {
          result += `${minutes}分钟`
        }
      }
      return result.trim() || '0秒'
    })


    return {
      getGreeting,
      hello,
      getUserInfo,
      name,
      getTotalCount,
      totalCount,
      timeDeal,
      caseName,
      getCases,
      cases,
      casesOriginal,
      handleChooseCase,
      searchCases,
      getStatisticsById,
      statisticsData,
      selected,
      setChartOption,
      initChart,
      resizeHandler,
      getOption,
      displayedCases,
      displayLimit,
      loadMore,
    }
  },
}
</script>

<style scoped lang="scss">
.container {
  padding: 20px;
  .first-head {
    height: 10vh;
    width: 100%;
    vertical-align: middle;
    text-align: center;
    .title-hello {
      font-size: 18px;
      margin-top: 3vh;
      display: block;
    }
    .title-info {
      display: block;
      font-size: 14px;
      margin-top: 8px;
    }
  }
  .first-head-total {
    height: 10vh;
    width: 100%;
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
    .total-item {
      height: 100%;
      width: 165px;
      margin-left: 10px;
      text-align: center;
      display: flex;
      justify-content: center;
      align-items: center;
      flex-direction: column;
      .total-item-title {
        font-size: 16px;
        color: #6e6d6d;
      }
     .total-item-value {
        font-size: 18px;
        margin-top: 8px;
        color: #4577ff;
      }

    }
    .total-item-case {
      height: 100%;
      width: 135px;
      margin-left: 8px;
      text-align: center;
      display: flex;
      justify-content: center;
      align-items: center;
      flex-direction: column;
      .total-item-title-case {
        font-size: 14px;
        color: #6e6d6d;
      }
     .total-item-value-case {
        font-size: 16px;
        margin-top: 8px;
        color: #4577ff;
      }

    }
  }
  .report-echarts {
    width: 100%; 
    height: 28vh;
    margin: 1VH 0;
  }
  .case-list-box {
    overflow: hidden;
    border: 1px solid #e8eef5;
    
    .case-list-header {
      padding: 16px 20px;
      border-bottom: 1px solid #f0f2f5;
      background: linear-gradient(135deg, #f5f8fc 0%, #ffffff 100%);
      
      .header-title {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        
        .iconfont {
          font-size: 20px;
          color: #409eff;
        }
        
        .case-count-badge {
          margin-left: auto;
          
          :deep(.el-badge__content) {
            font-size: 11px;
            padding: 2px 6px;
            height: 18px;
            line-height: 18px;
          }
        }
      }
    }
  }
  
  .case-search {
    padding: 12px 16px;
    background: #ffffff;
    
    :deep(.el-input__wrapper) {
      border-radius: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
      }
      
      &.is-focus {
        box-shadow: 0 2px 12px rgba(64, 158, 255, 0.25);
      }
    }
  }
  
  .case-list {
    width: 100%;
    overflow-y: auto;
    height: calc(70vh - 140px);
    padding: 8px 12px;
    
    .case-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 14px;
      margin-bottom: 6px;
      cursor: pointer;
      border-radius: 8px;
      background: #f8f9fa;
      border: 1px solid transparent;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        background: #409eff;
        transform: scaleY(0);
        transition: transform 0.3s ease;
      }
      
      .case-item-content {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
        min-width: 0;
        
        .case-icon {
          font-size: 16px;
          color: #909399;
          flex-shrink: 0;
          transition: all 0.3s ease;
        }
        
        .case-name {
          font-size: 14px;
          font-weight: 500;
          color: #606266;
          white-space: nowrap;
          text-overflow: ellipsis;
          overflow: hidden;
          transition: all 0.3s ease;
        }
      }
      
      .case-arrow {
        font-size: 14px;
        color: #409eff;
        opacity: 0;
        transform: translateX(-8px);
        transition: all 0.3s ease;
      }
      
      &:hover {
        background: #e8f4ff;
        border-color: #b3d8ff;
        transform: translateX(4px);
        
        .case-icon {
          color: #409eff;
          transform: scale(1.1);
        }
        
        .case-name {
          color: #409eff;
        }
      }
      
      &.case-item-active {
        background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
        border-color: #409eff;
        box-shadow: 0 3px 8px rgba(64, 158, 255, 0.25);
        
        &::before {
          transform: scaleY(1);
          background: #ffffff;
        }
        
        .case-item-content {
          .case-icon {
            color: #ffffff;
            transform: scale(1.1);
          }
          
          .case-name {
            color: #ffffff;
            font-weight: 600;
          }
        }
        
        .case-arrow {
          opacity: 1;
          transform: translateX(0);
          color: #ffffff;
        }
        
        &:hover {
          transform: translateX(3px);
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
        }
      }
    }
    
    .case-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: #c0c4cc;
      
      .iconfont {
        font-size: 64px;
        margin-bottom: 16px;
        opacity: 0.5;
      }
      
      p {
        font-size: 14px;
        margin: 0;
      }
    }
    
    .load-more {
      display: flex;
      justify-content: center;
      padding: 16px 0;
      margin-top: 8px;
      border-top: 1px solid #f0f2f5;
      
      :deep(.el-button) {
        font-size: 13px;
        font-weight: 500;
        
        .iconfont {
          margin-right: 4px;
          font-size: 14px;
        }
      }
    }
    
    .case-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      margin: 8px 12px;
      background: linear-gradient(135deg, #fff7e6 0%, #fff3d9 100%);
      border-left: 3px solid #ffa940;
      border-radius: 6px;
      
      .iconfont {
        font-size: 16px;
        color: #fa8c16;
        flex-shrink: 0;
      }
      
      p {
        font-size: 12px;
        color: #ad6800;
        margin: 0;
        line-height: 1.5;
      }
    }
  }
  
  .case-list::-webkit-scrollbar {
    width: 6px;
  }
  
  .case-list::-webkit-scrollbar-track {
    background: transparent;
    border-radius: 3px;
  }
  
  .case-list::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
    transition: all 0.3s ease;
    
    &:hover {
      background: #c0c4cc;
    }
  }
  .box {
    width: 100%;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 14px 0 #f3f3f3;
    height: 70vh;
    .statistics {
      height: 100%;
    }
  }
}

</style>
