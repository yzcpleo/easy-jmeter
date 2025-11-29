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
        <el-form-item label="指标类型">
          <el-input v-model="queryForm.metricType" placeholder="输入指标类型（可选）" style="width: 200px;" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query" :loading="loading">查询</el-button>
          <el-button @click="handleCreate" :disabled="!queryForm.chainId">新增路径</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 路径列表 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>性能指标路径列表</span>
      </template>
      <el-table :data="paths" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="metricName" label="指标名称" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="metricType" label="指标类型" width="150" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.metricType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startNodeName" label="起点节点" width="150"></el-table-column>
        <el-table-column prop="startTimeField" label="起点时间" width="120">
          <template #default="scope">
            <el-tag size="small" v-if="scope.row.startTimeField === 'REQUEST_START_TIME'">收到时间</el-tag>
            <el-tag size="small" type="info" v-else-if="scope.row.startTimeField === 'REQUEST_END_TIME'">发出时间</el-tag>
            <span v-else>{{ scope.row.startTimeField || '收到时间' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endNodeName" label="终点节点" width="150"></el-table-column>
        <el-table-column prop="endTimeField" label="终点时间" width="120">
          <template #default="scope">
            <el-tag size="small" v-if="scope.row.endTimeField === 'REQUEST_START_TIME'">收到时间</el-tag>
            <el-tag size="small" type="info" v-else-if="scope.row.endTimeField === 'REQUEST_END_TIME'">发出时间</el-tag>
            <span v-else>{{ scope.row.endTimeField || '发出时间' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="scope">
            <el-button plain size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button plain size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      @close="handleDialogClose">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="链路" prop="chainId">
          <el-select v-model="form.chainId" placeholder="选择链路" style="width: 100%;" :disabled="!!form.id">
            <el-option
              v-for="chain in chains"
              :key="chain.id"
              :label="chain.chain_name"
              :value="chain.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="指标名称" prop="metricName">
          <el-input v-model="form.metricName" placeholder="如：订单上行穿透时延"></el-input>
        </el-form-item>
        <el-form-item label="指标类型" prop="metricType">
          <el-input v-model="form.metricType" placeholder="请输入指标类型，如：订单上行、订单下行、行情等"></el-input>
        </el-form-item>
        <el-form-item label="起点节点" prop="startNodeId">
          <el-select v-model="form.startNodeId" placeholder="选择起点节点" style="width: 100%;" :disabled="!form.chainId">
            <el-option
              v-for="node in nodes"
              :key="node.id"
              :label="node.node_name"
              :value="node.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="起点时间字段">
          <el-select v-model="form.startTimeField" placeholder="选择起点时间字段" style="width: 100%;">
            <el-option label="收到时间 (REQUEST_START_TIME)" value="REQUEST_START_TIME"></el-option>
            <el-option label="发出时间 (REQUEST_END_TIME)" value="REQUEST_END_TIME"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="终点节点" prop="endNodeId">
          <el-select v-model="form.endNodeId" placeholder="选择终点节点" style="width: 100%;" :disabled="!form.chainId">
            <el-option
              v-for="node in nodes"
              :key="node.id"
              :label="node.node_name"
              :value="node.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="终点时间字段">
          <el-select v-model="form.endTimeField" placeholder="选择终点时间字段" style="width: 100%;">
            <el-option label="收到时间 (REQUEST_START_TIME)" value="REQUEST_START_TIME"></el-option>
            <el-option label="发出时间 (REQUEST_END_TIME)" value="REQUEST_END_TIME"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="路径描述"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put, _delete } from '@/lin/plugin/axios'
import {
  getMetricPathsByChainId,
  getMetricPathsByMetricType,
  createMetricPath,
  updateMetricPath,
  deleteMetricPath
} from '@/api/chain'

export default {
  name: 'ChainMetricPath',
  setup() {
    const loading = ref(false)
    const chains = ref([])
    const paths = ref([])
    const nodes = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增路径')
    const formRef = ref(null)
    const queryForm = ref({
      chainId: null,
      metricType: null
    })
    const form = ref({
      id: null,
      chainId: null,
      metricName: '',
      metricType: '',
      startNodeId: null,
      endNodeId: null,
      description: '',
      status: 1
    })
    const rules = {
      chainId: [{ required: true, message: '请选择链路', trigger: 'change' }],
      metricName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
      metricType: [{ required: true, message: '请输入指标类型', trigger: 'blur' }],
      startNodeId: [{ required: true, message: '请选择起点节点', trigger: 'change' }],
      endNodeId: [{ required: true, message: '请选择终点节点', trigger: 'change' }]
    }

    onMounted(() => {
      loadChains()
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
        // API 返回的数据可能是直接数组，也可能在 data 字段中
        nodes.value = Array.isArray(res) ? res : (res.data || res || [])
      } catch (error) {
        console.error('获取节点列表失败:', error)
        nodes.value = []
      }
    }

    const query = async () => {
      if (!queryForm.value.chainId) {
        ElMessage.warning('请先选择链路')
        return
      }
      try {
        loading.value = true
        let res
        if (queryForm.value.metricType) {
          res = await getMetricPathsByMetricType(queryForm.value.chainId, queryForm.value.metricType)
        } else {
          res = await getMetricPathsByChainId(queryForm.value.chainId)
        }
        // API 返回的数据可能是直接数组，也可能在 data 字段中
        paths.value = Array.isArray(res) ? res : (res.data || res || [])
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('查询路径失败:', error)
        ElMessage.error('查询路径失败: ' + (error.message || '未知错误'))
        paths.value = []
      }
    }

    const handleChainChange = () => {
      paths.value = []
      if (queryForm.value.chainId) {
        loadNodes(queryForm.value.chainId)
      }
    }

    const handleCreate = () => {
      if (!queryForm.value.chainId) {
        ElMessage.warning('请先选择链路')
        return
      }
      dialogTitle.value = '新增路径'
      form.value = {
        id: null,
        chainId: queryForm.value.chainId,
        metricName: '',
        metricType: '',
        startNodeId: null,
        startTimeField: 'REQUEST_START_TIME',
        endNodeId: null,
        endTimeField: 'REQUEST_END_TIME',
        description: '',
        status: 1
      }
      loadNodes(queryForm.value.chainId)
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑路径'
      form.value = {
        id: row.id,
        chainId: row.chainId,
        metricName: row.metricName,
        metricType: row.metricType,
        startNodeId: row.startNodeId,
        startTimeField: row.startTimeField || 'REQUEST_START_TIME',
        endNodeId: row.endNodeId,
        endTimeField: row.endTimeField || 'REQUEST_END_TIME',
        description: row.description || '',
        status: row.status
      }
      loadNodes(row.chainId)
      dialogVisible.value = true
    }

    const handleDelete = (id) => {
      ElMessageBox.confirm('此操作将永久删除该路径配置, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(async () => {
        try {
          loading.value = true
          await deleteMetricPath(id)
          ElMessage.success('删除成功')
          query()
        } catch (error) {
          loading.value = false
          console.error('删除路径失败:', error)
        }
      }).catch(() => {})
    }

    const handleSubmit = async () => {
      if (!formRef.value) return
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            loading.value = true
            // 准备提交的数据
            const submitData = {
              id: form.value.id,
              chainId: form.value.chainId,
              metricName: form.value.metricName,
              metricType: form.value.metricType,
              startNodeId: form.value.startNodeId,
              startTimeField: form.value.startTimeField || 'REQUEST_START_TIME',
              endNodeId: form.value.endNodeId,
              endTimeField: form.value.endTimeField || 'REQUEST_END_TIME',
              description: form.value.description || '',
              status: form.value.status || 1
            }
            console.log('准备提交的数据:', submitData)
            
            if (form.value.id) {
              const res = await updateMetricPath(form.value.id, submitData)
              console.log('更新响应:', res)
              ElMessage.success('更新成功')
            } else {
              const res = await createMetricPath(submitData)
              console.log('创建响应:', res)
              ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            query()
            loading.value = false
          } catch (error) {
            loading.value = false
            console.error('保存路径失败:', error)
            console.error('错误详情:', error.response?.data || error.message)
            const errorMsg = error.response?.data?.message || error.message || '未知错误'
            ElMessage.error('保存路径失败: ' + (typeof errorMsg === 'string' ? errorMsg : JSON.stringify(errorMsg)))
          }
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    watch(() => form.value.chainId, (newVal) => {
      if (newVal) {
        loadNodes(newVal)
      }
    })

    return {
      loading,
      chains,
      paths,
      nodes,
      dialogVisible,
      dialogTitle,
      formRef,
      queryForm,
      form,
      rules,
      query,
      handleChainChange,
      handleCreate,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleDialogClose
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}
</style>

