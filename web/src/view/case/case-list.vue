<template>
  <div class="container" v-if="!showEdit">
    <div class="header">
      <div class="newBtn"><el-button type="primary" @click="handleCreate">新 增</el-button></div>
      <div class="search">
        <el-select v-model="projectId" placeholder="请选择工程" clearable filterable>
          <el-option v-for="item in projects" :key="item.id" :label="item.name" :value="item.id"/>
        </el-select>
        <el-input placeholder="请输入用例名称查询" v-model="name" clearable></el-input>
      </div>
    </div>
    <el-main v-if="loading" v-loading = "loading" element-loading-text="Loading..." element-loading-background="#F9FAFB" style="height: 600px;"/>
    <div class="list" v-else>
      <div class="case-card" v-for="(item,index) in casesRes" :key="item.id" @click.stop="handleDetail(item)" @mouseover="caseMouseover(item)" @mouseleave="caseMouseout">
        <div class="card-header">
          <div class="case-title">
            <i class="iconfont icon-file-text title-icon"></i>
            <span class="name">{{item.name}}</span>
          </div>
          <div class="case-meta">
            <i class="iconfont icon-user meta-icon"></i>
            <span class="creator">{{item.creator}}</span>
            <i class="iconfont icon-time meta-icon"></i>
            <span class="create-time">{{item.create_time}}</span>
          </div>
        </div>
        
        <div class="card-body">
          <div class="status-section">
            <div class="status-header">
              <div class="status-label">
                <i class="iconfont icon-activity status-icon-label"></i>
                <span>执行状态</span>
              </div>
              <div class="status-badge" :class="getStatusClass(item)">
                <i class="status-icon" :class="getStatusIcon(item)"></i>
                <span>{{item.status ? item.status.desc : '待执行'}}</span>
              </div>
            </div>
            <el-progress 
              :text-inside="false" 
              :stroke-width="8" 
              :duration="60"
              :percentage="getProgressNum(item.task_progress)"
              :color="getProgressColor(item)" 
              :striped="setProgressStriped(item)"
              striped-flow
              :show-text="false"
              class="status-progress">
            </el-progress>
            <div class="progress-info" v-if="item.task_progress">
              <span class="progress-text">进度: {{getProgressNum(item.task_progress)}}%</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="action-buttons">
            <el-tooltip content="执行" placement="top">
              <el-button 
                circle 
                type="success" 
                size="small" 
                @click.stop="executeCase(item.id)"
                class="action-btn">
                <i class="iconfont icon-start"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="配置QPS" placement="top">
              <el-button 
                circle 
                type="primary" 
                size="small" 
                @click.stop="modifyQPSLimit(item.task_id, item.qps_limit)"
                class="action-btn">
                <i class="iconfont icon-config"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="停止" placement="top">
              <el-button 
                circle 
                type="warning" 
                size="small" 
                @click.stop="handleStop(item.task_id)"
                class="action-btn">
                <i class="iconfont icon-stop"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="调试" placement="top">
              <el-button 
                circle 
                type="info" 
                size="small" 
                @click.stop="handleDebug(item.id)"
                class="action-btn">
                <i class="iconfont icon-debug"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button 
                circle 
                type="primary" 
                size="small" 
                plain
                @click.stop="handleEdit(item.id)"
                class="action-btn">
                <i class="iconfont icon-modify"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button 
                circle 
                type="danger" 
                size="small" 
                @click.stop="handleDelete(item.id)"
                class="action-btn">
                <i class="iconfont icon-remove"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="历史记录" placement="top">
              <el-button 
                circle 
                type="info" 
                size="small" 
                plain
                @click.stop="handleHistory(item.name)"
                class="action-btn">
                <i class="iconfont icon-history"></i>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>
    <task :taskVisible="taskVisible" :caseId="taskCaseId" @taskClose="closeTask"></task>
    <qps-limit :qpsLimitVisible="qpsLimitVisible" :taskId="taskId" @qpsLimitDialogClose="closeQPSLimit"></qps-limit>
    <case-debug :caseId="debugCaseId" :debugVisible="debugVisible" @debugDialogClose="closeDebug"></case-debug>
  </div>
  <case v-else @editClose="editClose" :editCaseId="editCaseId" :projects="projects"></case>
</template>

<script>
  import Utils from 'lin/util/util'
  import { onMounted, ref, watch, inject } from 'vue'
  import { useRoute, useRouter } from "vue-router"
  import { get,put,_delete } from '@/lin/plugin/axios'
  import { ElMessageBox, ElMessage } from 'element-plus'
  import Case from './case'
  import Task from './task'
  import QpsLimit from './qps-limit'
  import CaseDebug from './case-debug'

  export default {
    components: {
      Case,
      Task,
      QpsLimit,
      CaseDebug,
    },
    setup() {
      const showEdit = ref(false)
      const name = ref('')
      const cases = ref([])
      const loading = ref(false)
      const editCaseId = ref(null)
      const projects = ref([])
      const projectId = ref('')
      const casesRes = ref([])
      const taskVisible = ref(false)
      const qpsLimitVisible = ref(false)
      const debugVisible = ref(false)
      const taskCaseId = ref(null)
      const taskId = ref('')
      const socketio = inject('socketio')
      const route = useRoute()
      const router = useRouter()
      const debugCaseId = ref(0)
      const hoverCaseId = ref(0)

      onMounted(() => {
        history.replaceState({}, '', '/#'+route.path);
        getProjects()
        getCases()
      })

      socketio.on('taskProgress', (data) => {
        for (let i = 0; i < cases.value.length; i++) {
          if (cases.value[i].task_id === data.taskId) {
            cases.value[i].status = data.status
            if (data.status.value === 2) {
              cases.value[i].task_progress = data.taskProgress
            } else {
              cases.value[i].task_progress = {'':100}
            }
          }
        }
        searchCases()
      })

      const getProjects = async () => {
        if (route.query.projectId) {
          projectId.value = parseInt(route.query.projectId, 10)
        }
        let res
        try {
          res = await get('/v1/project/all', { showBackend: true })
          projects.value = res
        } catch (error) {
          projects.value = []
        }
      }

      const getCases = async () => {
        loading.value = true
        let res
        try {
          res = await get('/v1/case', { showBackend: true })
          cases.value = res
        } catch (error) {
          cases.value = []
        }
        searchCases()
      }

      const editClose = () => {
        showEdit.value = false
        getCases()
      }
      const searchCases = () => {
        loading.value = true
        casesRes.value = []
        for (let i = 0; i < cases.value.length; i++) {
          if (cases.value[i].name.includes(name.value, { ignoreCase: true }) && (cases.value[i].project == projectId.value || projectId.value=='')) {
            casesRes.value.push(cases.value[i])
          }
        }
        loading.value = false
      }
      
      const handleCreate = () => {
        showEdit.value = true
        editCaseId.value = null
      }

      const handleEdit = id => {
        showEdit.value = true
        editCaseId.value = id
      }

      const handleDebug = id => {
        debugVisible.value = true
        debugCaseId.value = id
      }

      const handleDetail = item => {
        if (!item.task_id) {
          ElMessage.warning('请先执行用例')
          return
        }
        caseMouseout()
        router.push({
          path: '/case/detail',
          state: {detail: {caseId: item.id, taskId: item.task_id, latest: true}}
        })
      }

      const handleHistory = name => {
        router.push({
          path: '/task/history',
          state: {case: {caseName: name}}
        })
      }

      const executeCase = id => {
        taskVisible.value = true
        taskCaseId.value = id
      }

      const modifyQPSLimit = (id) => {
        qpsLimitVisible.value = true
        taskId.value = id
      }

      const closeTask = () => {
        taskVisible.value = false
        taskCaseId.value = null
        getCases()
      }

      const closeQPSLimit = () => {
        qpsLimitVisible.value = false
        taskId.value = null
      }

      const closeDebug = () => {
        debugVisible.value = false
      }

      const handleDelete = id => {
        let res
        ElMessageBox.confirm('此操作将永久删除该用例, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }).then(async () => {
          loading.value = true
          res = await _delete(`/v1/case/${id}`, { showBackend: true })
          getCases()
          res.code < 9999 ? ElMessage.success(`${res.message}`) : 1
        }).catch(()=>{})
      }

      const handleStop = taskId => {
        let res
        ElMessageBox.confirm('此操作将停止该用例的执行, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }).then(async () => {
          res = await put(`/v1/task/stop/`, { task_id: taskId }, { showBackend: true })
          res.code < 9999 ? ElMessage.success(`${res.message}`) : 1
        }).catch(()=>{})
      }

      const _debounce =Utils.debounce(()=>{
        searchCases()
      }, 800)

      watch(name, () => {
        _debounce()
      })

      watch(projectId, () => {
        _debounce()
      })

      const getProgressColor = c => {
        if (hoverCaseId.value === c.id) {
          return '#f5faff'
        }
        if (!c.status) {
          return '#b3d8ff'  // 默认颜色
        }
        switch (c.status.value) {
          case 0:
            return '#b3d8ff'
          case 1:
            return '#f1ca17'
          case 2:
            return '#96c24e'
          case 3:
            return '#ffa60f'
          case 4:
            return '#5e616d'
          case 5:
            return '#ce5777'
          default:
            return '#b3d8ff'
        }
      }

      const caseMouseover = c => {
        hoverCaseId.value = c.id
      }

      const caseMouseout = () => {
        hoverCaseId.value = null
      }

      const getProgressNum = progress => {
        if (progress == null) {
          return 100
        } else {
          return Math.min(...Object.values(progress))
        }
      }

      const setProgressStriped = c => {
        if (hoverCaseId.value === c.id) {
          return false
        }
        if (!c.status) {
          return false  // 默认不显示条纹
        }
        switch (c.status.value) {
          case 0:
            return false
          case 1:
            return true
          case 2:
            return true
          case 3:
            return true
          case 4:
            return true
          case 5:
            return true
          default:
            return false
        }
      }

      const getStatusIcon = c => {
        if (!c.status) {
          return 'iconfont icon-time'
        }
        switch (c.status.value) {
          case 0:
            return 'iconfont icon-coffee'  // 空闲
          case 1:
            return 'iconfont icon-setting'  // 配置
          case 2:
            return 'iconfont icon-loading'  // 运行
          case 3:
            return 'iconfont icon-download'  // 收集
          case 4:
            return 'iconfont icon-delete'  // 清理
          case 5:
            return 'iconfont icon-close-circle'  // 中断
          default:
            return 'iconfont icon-time'
        }
      }

      const getStatusClass = c => {
        if (!c.status) {
          return 'status-idle'
        }
        switch (c.status.value) {
          case 0:
            return 'status-idle'  // 空闲 - 蓝色
          case 1:
            return 'status-config'  // 配置 - 橙色
          case 2:
            return 'status-running'  // 运行 - 绿色
          case 3:
            return 'status-collect'  // 收集 - 橙色
          case 4:
            return 'status-clean'  // 清理 - 灰色
          case 5:
            return 'status-interrupt'  // 中断 - 红色
          default:
            return 'status-idle'
        }
      }

      return {
        cases,
        loading,
        editCaseId,
        projects,
        projectId,
        name,
        showEdit,
        handleCreate,
        casesRes,
        searchCases,
        editClose,
        handleEdit,
        handleDelete,
        taskVisible,
        qpsLimitVisible,
        taskCaseId,
        taskId,
        executeCase,
        modifyQPSLimit,
        closeTask,
        getProgressColor,
        getProgressNum,
        setProgressStriped,
        handleStop,
        closeQPSLimit,
        handleDetail,
        handleHistory,
        handleDebug,
        debugVisible,
        debugCaseId,
        closeDebug,
        hoverCaseId,
        caseMouseover,
        caseMouseout,
        getStatusIcon,
        getStatusClass,
    }

    },
  }
</script>

<style lang="scss" scoped>
.container {
  padding: 0 30px;
  min-height: calc(100vh - 200px);

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 24px 0 32px 0;
    padding-bottom: 20px;
    border-bottom: 1px solid #e4e7ed;

    .newBtn {
      :deep(.el-button) {
        padding: 12px 24px;
        font-size: 14px;
        font-weight: 500;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
        transition: all 0.3s ease;
        
        &:hover {
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
          transform: translateY(-1px);
        }
      }
    }

    .search {
      display: flex;
      align-items: center;
      gap: 16px;
      color: $parent-title-color;
      font-size: 14px;
      
      .el-select {
        width: 280px;
        
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          transition: all 0.3s ease;
          
          &:hover {
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
          }
        }
      }
      
      .el-input {
        width: 300px;
        
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          transition: all 0.3s ease;
          
          &:hover {
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
          }
        }
      }
    }
  }

  .list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(480px, 1fr));
    gap: 24px;
    padding-bottom: 24px;
    
    @media (max-width: 1200px) {
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
    }
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }
  
  .case-card {
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    border: 1px solid #e8eef5;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    height: 100%;
    position: relative;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, #409eff 0%, #67c23a 50%, #e6a23c 100%);
      opacity: 0;
      transition: opacity 0.4s ease;
    }
    
    &:hover {
      box-shadow: 0 12px 32px rgba(64, 158, 255, 0.15);
      transform: translateY(-6px);
      border-color: #409eff;
      
      &::before {
        opacity: 1;
      }
    }
    
    .card-header {
      padding: 24px 24px 20px;
      border-bottom: 1px solid #f0f2f5;
      background: linear-gradient(135deg, #f5f8fc 0%, #ffffff 100%);
      
      .case-title {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        
        .title-icon {
          font-size: 20px;
          color: #409eff;
          margin-right: 10px;
          display: inline-block;
        }
        
        .name {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
          white-space: nowrap;
          text-overflow: ellipsis;
          overflow: hidden;
          flex: 1;
          line-height: 1.4;
        }
      }
      
      .case-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 12px;
        color: #909399;
        
        .meta-icon {
          font-size: 14px;
          color: #c0c4cc;
          display: inline-block;
        }
        
        .creator, .create-time {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
    
    .card-body {
      padding: 20px 24px;
      flex: 1;
      background: #ffffff;
      
      .status-section {
        .status-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
        }
        
        .status-label {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          font-weight: 500;
          color: #606266;
          
          .status-icon-label {
            font-size: 16px;
            color: #409eff;
          }
        }
        
        .status-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          border-radius: 20px;
          font-size: 12px;
          font-weight: 600;
          letter-spacing: 0.3px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          transition: all 0.3s ease;
          
          .status-icon {
            font-size: 14px;
          }
          
          &.status-idle {
            background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
            color: #1976d2;
            border: 1px solid #90caf9;
            
            .status-icon {
              animation: none;
            }
          }
          
          &.status-config {
            background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
            color: #f57c00;
            border: 1px solid #ffb74d;
            
            .status-icon {
              animation: spin 2s linear infinite;
            }
          }
          
          &.status-running {
            background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
            color: #388e3c;
            border: 1px solid #81c784;
            
            .status-icon {
              animation: spin 1s linear infinite;
            }
          }
          
          &.status-collect {
            background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%);
            color: #f57f17;
            border: 1px solid #ffd54f;
            
            .status-icon {
              animation: bounce 1s ease-in-out infinite;
            }
          }
          
          &.status-clean {
            background: linear-gradient(135deg, #f5f5f5 0%, #e0e0e0 100%);
            color: #616161;
            border: 1px solid #bdbdbd;
          }
          
          &.status-interrupt {
            background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
            color: #c62828;
            border: 1px solid #ef5350;
          }
        }
        
        .status-progress {
          position: relative;
          margin-bottom: 8px;
          
          :deep(.el-progress-bar__outer) {
            border-radius: 10px;
            overflow: hidden;
            background: #f0f2f5;
            box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.08);
            border: 1px solid #e4e7ed;
          }
          
          :deep(.el-progress-bar__inner) {
            border-radius: 10px;
            position: relative;
            overflow: hidden;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
            
            &::before {
              content: '';
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              height: 40%;
              background: linear-gradient(180deg, rgba(255, 255, 255, 0.4) 0%, transparent 100%);
            }
          }
        }
        
        .progress-info {
          display: flex;
          justify-content: flex-end;
          margin-top: 4px;
          
          .progress-text {
            font-size: 11px;
            color: #909399;
            font-weight: 500;
          }
        }
      }
    }
    
    @keyframes spin {
      from {
        transform: rotate(0deg);
      }
      to {
        transform: rotate(360deg);
      }
    }
    
    @keyframes bounce {
      0%, 100% {
        transform: translateY(0);
      }
      50% {
        transform: translateY(-3px);
      }
    }
    
    .card-footer {
      padding: 18px 24px;
      background: linear-gradient(135deg, #fafbfc 0%, #f8f9fa 100%);
      border-top: 1px solid #e8eef5;
      
      .action-buttons {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        
        .action-btn {
          transition: all 0.2s ease;
          
          &:hover {
            transform: scale(1.1);
          }
          
          .iconfont {
            font-size: 16px;
            display: inline-block;
          }
        }
      }
    }
  }
}
</style>