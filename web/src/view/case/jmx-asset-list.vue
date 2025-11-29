<template>
  <div class="asset-container">
    <div class="asset-header">
      <div class="title">
        <i class="iconfont icon-caseStore"></i>
        <span>JMX资产管理</span>
      </div>
      <div class="filters">
        <el-select
          v-model="projectId"
          placeholder="选择项目"
          clearable
          size="default"
          @change="handleFilterChange"
        >
          <el-option
            v-for="item in projects"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="输入名称搜索"
          clearable
          size="default"
          @keyup.enter.native="handleFilterChange"
        >
          <template #suffix>
            <i class="iconfont icon-search"></i>
          </template>
        </el-input>
        <el-button @click="fetchAssets" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">
          新建资产
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="filteredAssets" v-loading="loading" border>
        <el-table-column prop="name" label="资产名称" min-width="180" />
        <el-table-column prop="projectId" label="所属项目" min-width="140">
          <template #default="{ row }">
            {{ getProjectName(row.projectId) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="latestStructureVersion" label="结构版本" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="success">
              v{{ row.latestStructureVersion || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jmxFileId" label="JMX文件" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.jmxFileId ? 'success' : 'info'">
              {{ row.jmxFileId ? '已生成' : '未生成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openBuilder(row)">
              Builder
            </el-button>
            <el-button type="success" text size="small" @click="handleGenerate(row)">
              生成JMX
            </el-button>
            <el-button type="info" text size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="warning" text size="small" @click="handleCopy(row)">
              复制
            </el-button>
            <el-button type="default" text size="small" @click="handleDownload(row)">
              下载
            </el-button>
            <el-tag size="small" type="info">{{ row.creationMode === 'BUILDER' ? '在线构建' : '上传' }}</el-tag>
            <el-popconfirm
              title="确定删除该资产吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑资产' : '新建资产'" width="480px">
      <el-form :model="assetForm" :rules="assetRules" ref="assetFormRef" label-width="90px">
        <el-form-item label="资产名称" prop="name">
          <el-input v-model="assetForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="assetForm.projectId" placeholder="请选择项目" style="width: 100%">
            <el-option v-for="item in projects" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="assetForm.description"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitAssetForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import jmxApi from '@/api/jmx'
import { get } from '@/lin/plugin/axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const assets = ref([])
const loading = ref(false)
const projectId = ref()
const keyword = ref('')
const projects = ref([])
const formDialogVisible = ref(false)
const isEdit = ref(false)
const formSubmitting = ref(false)
const assetFormRef = ref(null)
const assetForm = reactive({
  id: null,
  name: '',
  projectId: null,
  description: '',
})
const assetRules = {
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
}

const normalizeAsset = asset => {
  if (!asset) return {}
  const normalized = { ...asset }
  normalized.projectId = asset.projectId ?? asset.project_id ?? null
  normalized.jmxFileId = asset.jmxFileId ?? asset.jmx_file_id ?? null
  normalized.creationMode = asset.creationMode ?? asset.creation_mode ?? 'UPLOAD'
  normalized.latestStructureVersion = asset.latestStructureVersion ?? asset.latest_structure_version ?? 0
  normalized.jmxFile = asset.jmxFile ?? asset.jmx_file ?? null
  return normalized
}

const filteredAssets = computed(() => {
  return assets.value.filter(asset => {
    const matchProject = projectId.value ? Number(asset.projectId) === Number(projectId.value) : true
    const matchKeyword = keyword.value
      ? asset.name?.toLowerCase().includes(keyword.value.toLowerCase())
      : true
    return matchProject && matchKeyword
  })
})

const fetchProjects = async () => {
  try {
    const res = await get('/v1/project/all', { showBackend: true })
    projects.value = res || []
  } catch (error) {
    projects.value = []
    console.error(error)
  }
}

const fetchAssets = async () => {
  loading.value = true
  try {
    const res = await jmxApi.listAssets({
      projectId: projectId.value || undefined,
    })
    assets.value = (res || []).map(normalizeAsset)
  } catch (error) {
    ElMessage.error('获取JMX资产失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  fetchAssets()
}

const getProjectName = id => {
  if (id === undefined || id === null) return '-'
  const numericId = Number(id)
  const project = projects.value.find(item => Number(item.id) === numericId)
  return project ? project.name : '-'
}

const openBuilder = row => {
  router.push({ name: 'jmx-builder', query: { assetId: row.id } })
}

const viewDetail = row => {
  ElMessage.info(`资产【${row.name}】详情暂未开放，敬请期待`)
}

const openCreateDialog = () => {
  isEdit.value = false
  assetForm.id = null
  assetForm.name = ''
  assetForm.projectId = projectId.value || null
  assetForm.description = ''
  formDialogVisible.value = true
}

const openEditDialog = async row => {
  try {
    const detail = await jmxApi.getAsset(row.id)
    // 使用 normalizeAsset 确保字段格式统一（处理 projectId/project_id 等）
    const normalized = normalizeAsset(detail)
    assetForm.id = normalized.id
    assetForm.name = normalized.name || ''
    // 确保 projectId 是数字类型，与 el-select 的 value 类型匹配
    assetForm.projectId = normalized.projectId ? Number(normalized.projectId) : null
    assetForm.description = normalized.description || ''
    isEdit.value = true
    formDialogVisible.value = true
  } catch (error) {
    console.error('获取资产详情失败:', error)
    ElMessage.error('获取资产详情失败')
  }
}

const submitAssetForm = async () => {
  formSubmitting.value = true
  try {
    await assetFormRef.value.validate()
    if (isEdit.value) {
      await jmxApi.updateAsset(assetForm.id, {
        name: assetForm.name,
        projectId: assetForm.projectId,
        description: assetForm.description,
      })
      ElMessage.success('资产更新成功')
    } else {
      await jmxApi.createAsset({
        name: assetForm.name,
        projectId: assetForm.projectId,
        description: assetForm.description,
      })
      ElMessage.success('资产创建成功')
    }
    formDialogVisible.value = false
    await fetchAssets()
  } catch (error) {
    console.error('Asset save error:', error)
    // 处理错误响应
    if (error?.response?.data) {
      const errorData = error.response.data
      // 检查是否是验证错误
      if (errorData.code && errorData.message) {
        // 如果是对象类型的 message（验证错误），提取所有错误信息
        if (typeof errorData.message === 'object') {
          const errorMessages = Object.values(errorData.message).flat()
          ElMessage.error(errorMessages.join('; ') || '保存失败，请检查输入信息')
        } else {
          ElMessage.error(errorData.message || '保存失败')
        }
      } else {
        ElMessage.error('保存失败，请检查输入信息')
      }
    } else if (error?.message && error.message !== 'cancel') {
      ElMessage.error(error.message || '保存失败')
    } else {
      ElMessage.error('保存失败，请重试')
    }
  } finally {
    formSubmitting.value = false
  }
}

const handleDelete = async row => {
  try {
    await jmxApi.deleteAsset(row.id)
    ElMessage.success('删除成功')
    fetchAssets()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleGenerate = async row => {
  try {
    await jmxApi.generateAssetJmx(row.id)
    const latest = normalizeAsset(await jmxApi.getAsset(row.id))
    Object.assign(row, latest)
    ElMessage.success('JMX 文件已生成')
    await fetchAssets()
  } catch (error) {
    ElMessage.error('生成失败，请检查结构是否完整')
  }
}

const handleCopy = async row => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新资产名称', '复制JMX资产', {
      confirmButtonText: '复制',
      cancelButtonText: '取消',
      inputValue: `${row.name}-副本`,
      inputValidator: val => {
        if (!val || !val.trim()) {
          return '名称不能为空'
        }
        return true
      },
    })
    await jmxApi.copyAsset(row.id, { name: value.trim() })
    ElMessage.success('复制成功')
    fetchAssets()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('复制失败')
  }
}

const handleDownload = row => {
  if (!row.jmxFile || !row.jmxFile.url) {
    ElMessage.warning('请先生成JMX文件')
    return
  }
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = row.jmxFile.url
  link.download = row.jmxFile.name || `${row.name}.jmx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(async () => {
  await fetchProjects()
  await fetchAssets()
})
</script>

<style scoped lang="scss">
.asset-container {
  padding: 24px;

  .asset-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      display: flex;
      align-items: center;
      font-size: 18px;
      font-weight: 600;
      color: #303133;

      .iconfont {
        margin-right: 8px;
        color: #409eff;
      }
    }

    .filters {
      display: flex;
      gap: 12px;
      align-items: center;
      width: 480px;
    }
  }

  .name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .asset-name {
      font-weight: 500;
      color: #303133;
    }
  }
}
</style>

