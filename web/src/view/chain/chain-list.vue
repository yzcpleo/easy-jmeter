<template>
  <div class="container" v-if="!showEdit">
    <div class="header">
      <div class="newBtn"><el-button type="primary" @click="handleCreate">新增链路</el-button></div>
      <div class="header-right">
        <el-icon :size="25" @click="getChains"><Refresh /></el-icon>
        <div class="search">
          <el-input placeholder="请输入链路名称查询" v-model="chainName" clearable></el-input>
        </div>
      </div>
    </div>
    <el-table :data="chains" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="chain_name" label="链路名称" width="200" show-overflow-tooltip></el-table-column>
      <el-table-column prop="chain_description" label="描述" show-overflow-tooltip></el-table-column>
      <el-table-column prop="task_id" label="关联任务ID" width="130"></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="info">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_by" label="创建人" width="120"></el-table-column>
      <el-table-column prop="created_time" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作" fixed="right" width="300">
        <template #default="scope">
          <el-button plain size="small" type="primary" @click="handleNodes(scope.row.id)">节点管理</el-button>
          <el-button plain size="small" type="primary" @click="handleEdit(scope.row.id)">编辑</el-button>
          <el-button plain size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination
        background 
        :hide-on-single-page="true" 
        @current-change="handleCurrentChange" 
        layout="prev, pager, next" 
        :current-page="pageData.page + 1" 
        :page-size="10" 
        :total="pageData.total">
      </el-pagination>
    </div>
  </div>
  <chain-edit v-else @editClose="editClose" :editChainId="editChainId"></chain-edit>
</template>

<script>
import Utils from 'lin/util/util'
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { get, _delete } from '@/lin/plugin/axios'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ChainEdit from './chain-edit'

export default {
  name: 'ChainList',
  components: {
    ChainEdit,
    Refresh,
  },
  setup() {
    const showEdit = ref(false)
    const chainName = ref('')
    const chains = ref([])
    const pageData = ref({ total: 0, page: 0 })
    const loading = ref(false)
    const editChainId = ref(null)
    const router = useRouter()

    onMounted(() => {
      getChains()
    })

    const getChains = async () => {
      try {
        loading.value = true
        const res = await get('/v1/chain/trace/page', { 
          page: pageData.value.page, 
          chain_name: chainName.value 
        }, { showBackend: true })
        chains.value = res.items || []
        pageData.value.total = res.total || 0
        pageData.value.page = res.page || 0
        loading.value = false
      } catch (error) {
        loading.value = false
        chains.value = []
        console.error('获取链路列表失败:', error)
      }
    }

    const handleDelete = id => {
      ElMessageBox.confirm('此操作将永久删除该链路及所有节点配置, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(async () => {
        try {
          loading.value = true
          const res = await _delete(`/v1/chain/trace/${id}`, { showBackend: true })
          pageData.value.page = 0
          getChains()
          if (res.code < 9999) {
            ElMessage.success(`${res.message}`)
          }
        } catch (error) {
          loading.value = false
          console.error('删除链路失败:', error)
        }
      }).catch(() => {})
    }

    const handleCurrentChange = val => {
      pageData.value.page = val - 1
      getChains()
    }

    const editClose = () => {
      showEdit.value = false
      pageData.value.page = 0
      getChains()
    }

    const handleEdit = id => {
      showEdit.value = true
      editChainId.value = id
    }

    const handleCreate = () => {
      showEdit.value = true
      editChainId.value = null
    }

    const handleNodes = id => {
      router.push({ path: '/chain/nodes', query: { chainId: id } })
    }

    const _debounce = Utils.debounce(() => {
      pageData.value.page = 0
      getChains()
    }, 800)

    watch(chainName, () => {
      _debounce()
    })

    return {
      chains,
      chainName,
      showEdit,
      pageData,
      loading,
      editChainId,
      handleCurrentChange,
      handleDelete,
      editClose,
      handleEdit,
      handleCreate,
      handleNodes,
      getChains,
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

    .header-right {
      display: flex;
      align-items: center;
      gap: 15px;

      .search {
        width: 250px;
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin: 20px 0;
  }
}
</style>
