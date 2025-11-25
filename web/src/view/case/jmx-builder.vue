<template>
  <div class="jmx-builder-container">
    <div class="header">
      <div class="title-section">
        <h2>JMX Builder</h2>
        <el-tag v-if="caseId">Editing Case #{{ caseId }}</el-tag>
      </div>
      
      <div class="action-section">
        <el-button @click="loadTemplate">
          <el-icon><Document /></el-icon>
          Load Template
        </el-button>
        <el-button @click="validateStructure" :loading="validating">
          <el-icon><Check /></el-icon>
          Validate
        </el-button>
        <el-button type="primary" @click="saveStructure" :loading="saving">
          <el-icon><Upload /></el-icon>
          Save
        </el-button>
        <el-button @click="back">
          <el-icon><Back /></el-icon>
          Back
        </el-button>
      </div>
    </div>
    
    <div class="editor-wrapper" v-loading="loading">
      <JmxTreeEditor
        ref="editorRef"
        v-model="jmxStructure"
        @change="onStructureChange"
      />
    </div>
    
    <!-- Template Selection Dialog -->
    <el-dialog v-model="showTemplateDialog" title="Select Template" width="600px">
      <el-table :data="templateList" @row-click="selectTemplate">
        <el-table-column prop="name" label="Template Name" />
        <el-table-column prop="description" label="Description" />
      </el-table>
      
      <template #footer>
        <el-button @click="showTemplateDialog = false">Cancel</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Check, Upload, Back } from '@element-plus/icons-vue'
import JmxTreeEditor from '@/components/jmx-editor/JmxTreeEditor.vue'
import jmxApi from '@/api/jmx'

const route = useRoute()
const router = useRouter()

const caseId = ref(null)
const jmxStructure = ref(null)
const editorRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const validating = ref(false)
const showTemplateDialog = ref(false)
const templateList = ref([])

onMounted(async () => {
  // Get case ID from route params or query and convert to integer
  const id = route.params.id || route.query.caseId
  caseId.value = id ? parseInt(id) : null
  
  console.log('JMX Builder mounted with caseId:', caseId.value)
  
  if (caseId.value) {
    // Load existing structure
    await loadExistingStructure()
  } else {
    // Create new empty test plan
    await createNew()
  }
})

// Create new test plan
const createNew = async () => {
  try {
    loading.value = true
    const res = await jmxApi.createEmptyTestPlan('New Test Plan')
    jmxStructure.value = res
    ElMessage.success('New test plan created')
  } catch (error) {
    console.error('Failed to create test plan:', error)
    ElMessage.error('Failed to create test plan')
  } finally {
    loading.value = false
  }
}

// Load existing structure
const loadExistingStructure = async () => {
  try {
    loading.value = true
    const res = await jmxApi.getJmxTree(caseId.value)
    
    if (res) {
      jmxStructure.value = res
      ElMessage.success('JMX 结构已加载')
    } else {
      // Parse JMX file first
      ElMessage.info('正在解析 JMX 文件...')
      try {
        const parsed = await jmxApi.parseJmx(caseId.value)
        jmxStructure.value = parsed
        ElMessage.success('JMX 文件解析成功')
      } catch (parseError) {
        console.error('Failed to parse JMX:', parseError)
        const errorMsg = parseError.response?.data?.message || parseError.message || '未知错误'
        
        // Check if file doesn't exist in storage
        if (errorMsg.includes('does not exist') || 
            errorMsg.includes('not found in storage') ||
            errorMsg.includes('may have been deleted')) {
          ElMessageBox.confirm(
            'JMX 文件在存储中不存在（可能已被删除）。是否创建新的 JMX 结构？',
            '文件不存在',
            {
              confirmButtonText: '创建新结构',
              cancelButtonText: '加载模板',
              type: 'warning',
            }
          ).then(async () => {
            await createNew()
          }).catch(async () => {
            await loadTemplate()
          })
        } else {
          ElMessage.error(`解析失败: ${errorMsg}`)
          // Still offer to create new
          setTimeout(async () => {
            await createNew()
          }, 1000)
        }
      }
    }
  } catch (error) {
    console.error('Failed to load structure:', error)
    ElMessage.error('加载 JMX 结构失败')
    // Create empty plan as fallback
    await createNew()
  } finally {
    loading.value = false
  }
}

// Load template
const loadTemplate = async () => {
  try {
    const templates = await jmxApi.listTemplates()
    
    templateList.value = Object.keys(templates).map(key => ({
      name: key,
      description: templates[key]
    }))
    
    showTemplateDialog.value = true
  } catch (error) {
    console.error('Failed to load templates:', error)
    ElMessage.error('Failed to load templates')
  }
}

// Select template
const selectTemplate = async (row) => {
  try {
    showTemplateDialog.value = false
    loading.value = true
    
    const res = await jmxApi.getTemplate(row.name)
    jmxStructure.value = res
    
    ElMessage.success(`Template "${row.name}" loaded`)
  } catch (error) {
    console.error('Failed to load template:', error)
    ElMessage.error('Failed to load template')
  } finally {
    loading.value = false
  }
}

// Validate structure
const validateStructure = async () => {
  if (!jmxStructure.value) {
    ElMessage.warning('没有可验证的结构')
    return
  }
  
  if (!caseId.value) {
    ElMessage.warning('请先创建或选择一个用例')
    return
  }
  
  try {
    validating.value = true
    console.log('Validating structure for caseId:', caseId.value)
    const res = await jmxApi.validateStructure(caseId.value, jmxStructure.value)
    
    if (res.valid) {
      ElMessage.success('结构验证通过')
    } else {
      ElMessage.error(res.message || '结构验证失败')
    }
  } catch (error) {
    console.error('Validation error:', error)
    const errorMsg = error.response?.data?.message || error.message || '验证失败'
    ElMessage.error(`验证失败: ${JSON.stringify(errorMsg)}`)
  } finally {
    validating.value = false
  }
}

// Save structure
const saveStructure = async () => {
  if (!jmxStructure.value) {
    ElMessage.warning('没有可保存的结构')
    return
  }
  
  if (!caseId.value) {
    ElMessage.warning('请先创建或选择一个用例')
    return
  }
  
  try {
    saving.value = true
    
    console.log('Saving structure for caseId:', caseId.value)
    console.log('Structure:', jmxStructure.value)
    
    // Validate first
    const validation = await jmxApi.validateStructure(caseId.value, jmxStructure.value)
    if (!validation.valid) {
      ElMessage.error('结构验证失败，请修复错误后再保存')
      return
    }
    
    // Save structure
    await jmxApi.saveJmxStructure(caseId.value, jmxStructure.value)
    
    // Generate JMX file
    await jmxApi.generateJmx(caseId.value)
    
    ElMessage.success('JMX 结构保存成功')
  } catch (error) {
    console.error('Save error:', error)
    const errorMsg = error.response?.data?.message || error.message || '保存失败'
    ElMessage.error(`保存失败: ${JSON.stringify(errorMsg)}`)
  } finally {
    saving.value = false
  }
}

// Handle structure change
const onStructureChange = (newStructure) => {
  // Structure changed
  console.log('Structure changed:', newStructure)
}

// Go back
const back = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.jmx-builder-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  
  .header {
    background: #fff;
    padding: 20px 30px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .title-section {
      display: flex;
      align-items: center;
      gap: 15px;
      
      h2 {
        margin: 0;
        font-size: 24px;
        font-weight: 600;
        color: #303133;
      }
    }
    
    .action-section {
      display: flex;
      gap: 10px;
    }
  }
  
  .editor-wrapper {
    flex: 1;
    padding: 20px 30px;
    overflow: hidden;
  }
}
</style>

