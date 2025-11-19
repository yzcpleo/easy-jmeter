<template>
  <div class="container" v-if="!showEdit">
    <div class="header">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/chain/list' }">链路管理</el-breadcrumb-item>
          <el-breadcrumb-item>节点配置</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="newBtn"><el-button type="primary" @click="handleCreate">新增节点</el-button></div>
    </div>
    <el-table :data="nodes" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="node_name" label="节点名称" width="150"></el-table-column>
      <el-table-column prop="node_type" label="节点类型" width="120"></el-table-column>
      <el-table-column prop="sequence_order" label="顺序" width="80"></el-table-column>
      <el-table-column prop="log_path" label="日志路径" show-overflow-tooltip></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="info">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="200">
        <template #default="scope">
          <el-button plain size="small" type="primary" @click="handleEdit(scope.row.id)">编辑</el-button>
          <el-button plain size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
  <node-edit v-else @editClose="editClose" :editNodeId="editNodeId" :chainId="chainId"></node-edit>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, _delete } from '@/lin/plugin/axios'
import { ElMessageBox, ElMessage } from 'element-plus'
import NodeEdit from './node-edit'

export default {
  name: 'ChainNodes',
  components: {
    NodeEdit,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const showEdit = ref(false)
    const nodes = ref([])
    const loading = ref(false)
    const editNodeId = ref(null)
    const chainId = ref(route.query.chainId)

    onMounted(() => {
      if (!chainId.value) {
        ElMessage.error('缺少链路ID参数')
        router.push('/chain/list')
        return
      }
      getNodes()
    })

    const getNodes = async () => {
      try {
        loading.value = true
        const res = await get(`/v1/chain/trace/${chainId.value}`, {}, { showBackend: true })
        nodes.value = res.node_configs || []
        loading.value = false
      } catch (error) {
        loading.value = false
        nodes.value = []
        console.error('获取节点列表失败:', error)
      }
    }

    const handleDelete = id => {
      ElMessageBox.confirm('此操作将永久删除该节点配置, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(async () => {
        try {
          loading.value = true
          const res = await _delete(`/v1/chain/node/${id}`, { showBackend: true })
          // 删除成功后刷新列表
          await getNodes()
          // 显示成功消息
          if (res && res.code !== undefined) {
            if (res.code < 9999) {
              ElMessage.success(res.message || '删除成功')
            } else {
              ElMessage.error(res.message || '删除失败')
            }
          } else {
            ElMessage.success('删除成功')
          }
        } catch (error) {
          console.error('删除节点失败:', error)
          ElMessage.error(error.message || '删除失败，请重试')
        } finally {
          loading.value = false
        }
      }).catch(() => {
        // 用户取消删除，不做任何操作
      })
    }

    const editClose = () => {
      showEdit.value = false
      getNodes()
    }

    const handleEdit = id => {
      showEdit.value = true
      editNodeId.value = id
    }

    const handleCreate = () => {
      showEdit.value = true
      editNodeId.value = null
    }

    return {
      nodes,
      showEdit,
      loading,
      editNodeId,
      chainId,
      handleDelete,
      editClose,
      handleEdit,
      handleCreate,
    }
  },
}
</script>

<style lang="scss" scoped>
.container {
  padding: 0 30px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 20px 0;

    .breadcrumb {
      font-size: 14px;
    }
  }
}
</style>
