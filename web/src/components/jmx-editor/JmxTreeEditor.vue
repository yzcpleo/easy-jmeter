<template>
  <div class="jmx-tree-editor">
    <div class="editor-layout">
      <!-- Left Panel: Tree View -->
      <div class="tree-panel">
        <div class="panel-header">
          <h3>Test Plan Structure</h3>
          <div class="toolbar">
            <el-button
              size="small"
              type="primary"
              @click="addElement"
              :disabled="!canAddChild"
            >
              <el-icon><Plus /></el-icon>
              Add Element
            </el-button>
            <el-button
              size="small"
              @click="deleteElement"
              :disabled="!canDeleteCurrentNode"
            >
              <el-icon><Delete /></el-icon>
              Delete
            </el-button>
          </div>
        </div>
        
        <div class="tree-container">
          <el-tree
            ref="treeRef"
            :data="treeData"
            node-key="id"
            :props="treeProps"
            :highlight-current="true"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
            draggable
            :allow-drop="allowDrop"
            :allow-drag="allowDrag"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <el-icon :class="getIconClass(data.type)">
                  <component :is="getIconComponent(data.type)" />
                </el-icon>
                <span class="node-label">{{ data.name }}</span>
                <span v-if="!data.enabled" class="disabled-tag">(Disabled)</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>
      
      <!-- Right Panel: Property Editor -->
      <div class="property-panel">
        <div class="panel-header">
          <h3>Properties</h3>
        </div>
        
        <div class="property-container" v-if="currentNode">
          <el-form :model="currentNode" label-width="150px" label-position="left">
            <!-- Common Properties -->
            <el-form-item label="Name">
              <el-input v-model="currentNode.name" @change="onPropertyChange" />
            </el-form-item>
            
            <el-form-item label="Enabled">
              <el-switch v-model="currentNode.enabled" @change="onPropertyChange" />
            </el-form-item>
            
            <el-form-item label="Comments">
              <el-input
                v-model="currentNode.comments"
                type="textarea"
                :rows="2"
                @change="onPropertyChange"
              />
            </el-form-item>
            
            <el-divider />
            
            <!-- Type-specific Properties -->
            <component
              :is="getEditorComponent(currentNode.type)"
              v-if="getEditorComponent(currentNode.type)"
              :node="currentNode"
              @update="onPropertyChange"
            />
            
            <div v-else>
              <el-alert type="info" :closable="false">
                No specific properties for this element type.
              </el-alert>
            </div>

            <el-alert
              v-if="isUnsupportedCurrentNode"
              type="warning"
              :closable="false"
              style="margin-top: 12px;"
            >
              This element type is not fully supported in the editor. It will be preserved
              as-is when saving, but cannot be modified here.
            </el-alert>
          </el-form>
        </div>
        
        <div class="property-container empty" v-else>
          <el-empty description="Select an element to edit its properties" />
        </div>
      </div>
    </div>
    
    <!-- Add Element Dialog -->
    <el-dialog v-model="showAddDialog" title="Add Element" width="500px">
      <el-form label-width="120px">
        <el-form-item label="Element Type">
          <el-select v-model="newElementType" placeholder="Select type" style="width: 100%;">
            <el-option
              v-for="type in availableElementTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="Name">
          <el-input v-model="newElementName" placeholder="Enter element name" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDialog = false">Cancel</el-button>
        <el-button type="primary" @click="confirmAddElement">Add</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, defineProps, defineEmits } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Document, FolderOpened, Link, Coffee } from '@element-plus/icons-vue'

import ThreadGroupEditor from './ThreadGroupEditor.vue'
import HttpSamplerEditor from './HttpSamplerEditor.vue'
import JavaSamplerEditor from './JavaSamplerEditor.vue'
import HeaderManagerEditor from './HeaderManagerEditor.vue'
import CSVDataSetEditor from './CSVDataSetEditor.vue'
import SchemaDrivenEditor from './SchemaDrivenEditor.vue'
import { schemaElements, getSchemaForType } from './schema/jmxElementSchema'

const builtinClassMetadata = {
  TestPlan: { guiClass: 'TestPlanGui', testClass: 'TestPlan' },
  ThreadGroup: { guiClass: 'ThreadGroupGui', testClass: 'ThreadGroup' },
  HTTPSampler: { guiClass: 'HttpTestSampleGui', testClass: 'HTTPSamplerProxy' },
  HTTPSamplerProxy: { guiClass: 'HttpTestSampleGui', testClass: 'HTTPSamplerProxy' },
  JavaSampler: { guiClass: 'JavaTestSamplerGui', testClass: 'JavaSampler' },
  CSVDataSet: { guiClass: 'TestBeanGUI', testClass: 'CSVDataSet' },
  HeaderManager: { guiClass: 'HeaderPanel', testClass: 'HeaderManager' },
  ResultCollector: { guiClass: 'ViewResultsFullVisualizer', testClass: 'ResultCollector' }
}

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// Tree data
const treeData = ref([])
const treeRef = ref(null)
const currentNode = ref(null)

// Tree props
const treeProps = {
  children: 'children',
  label: 'name'
}

// Add element dialog
const showAddDialog = ref(false)
const newElementType = ref('')
const newElementName = ref('')

const schemaTypeSet = new Set(Object.keys(schemaElements))

const supportedNodeTypes = new Set([
  'TestPlan',
  'ThreadGroup',
  'HTTPSampler',
  'HTTPSamplerProxy',
  'JavaSampler',
  'CSVDataSet',
  'HeaderManager',
  'ResultCollector',
  ...schemaTypeSet
])

const schemaTypeOptions = (parentType) => {
  return Object.entries(schemaElements)
    .filter(([, meta]) => meta.allowedParents?.includes(parentType))
    .map(([type, meta]) => ({
      label: meta.label,
      value: type
    }))
}

const baseElementTypes = computed(() => {
  if (!currentNode.value) {
    return []
  }
  
  const type = currentNode.value.type
  
  if (type === 'TestPlan') {
    return [
      { label: 'Thread Group', value: 'ThreadGroup' },
      { label: 'Result Collector', value: 'ResultCollector' }
    ]
  } else if (type === 'ThreadGroup') {
    return [
      { label: 'HTTP Request', value: 'HTTPSamplerProxy' },
      { label: 'Java Request', value: 'JavaSampler' },
      { label: 'CSV Data Set', value: 'CSVDataSet' },
      { label: 'Header Manager', value: 'HeaderManager' }
    ]
  }
  
  return []
})

// Available element types based on current selection
const availableElementTypes = computed(() => {
  if (!currentNode.value) {
    return []
  }
  const schemaOptions = schemaTypeOptions(currentNode.value.type)
  const merged = [...baseElementTypes.value, ...schemaOptions]
  const map = new Map()
  merged.forEach((item) => {
    if (!map.has(item.value)) {
      map.set(item.value, item)
    }
  })
  return Array.from(map.values())
})

const canAddChild = computed(() => {
  if (!currentNode.value) {
    return false
  }
  if (!supportedNodeTypes.has(currentNode.value.type)) {
    return false
  }
  return availableElementTypes.value.length > 0
})

const canDeleteCurrentNode = computed(() => {
  if (!currentNode.value) {
    return false
  }
  if (currentNode.value.type === 'TestPlan') {
    return false
  }
  return supportedNodeTypes.has(currentNode.value.type)
})

const isUnsupportedCurrentNode = computed(() => {
  if (!currentNode.value) {
    return false
  }
  return !supportedNodeTypes.has(currentNode.value.type)
})

// Initialize tree data from props
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    treeData.value = [newVal]
  }
}, { immediate: true })

// Handle node click
const handleNodeClick = (data) => {
  currentNode.value = data
}

// Get icon component for element type
const getIconComponent = (type) => {
  const iconMap = {
    'TestPlan': Document,
    'ThreadGroup': FolderOpened,
    'HTTPSampler': Link,
    'HTTPSamplerProxy': Link,
    'JavaSampler': Coffee,
    'CSVDataSet': Document,
    'HeaderManager': Document,
    'ResultCollector': Document
  }
  return iconMap[type] || Document
}

// Get icon class
const getIconClass = (type) => {
  return `icon-${type.toLowerCase()}`
}

// Get editor component for element type
const editorMap = {
  'ThreadGroup': ThreadGroupEditor,
  'HTTPSampler': HttpSamplerEditor,
  'HTTPSamplerProxy': HttpSamplerEditor,  // JMeter uses HTTPSamplerProxy
  'JavaSampler': JavaSamplerEditor,
  'CSVDataSet': CSVDataSetEditor,
  'HeaderManager': HeaderManagerEditor
}
const getEditorComponent = (type) => {
  if (editorMap[type]) {
    return editorMap[type]
  }
  if (schemaTypeSet.has(type)) {
    return SchemaDrivenEditor
  }
  return null
}

// Add element
const addElement = () => {
  if (!currentNode.value) {
    ElMessage.warning('Please select a parent element first')
    return
  }
  
  showAddDialog.value = true
  newElementType.value = ''
  newElementName.value = ''
}

// Confirm add element
const confirmAddElement = () => {
  if (!newElementType.value) {
    ElMessage.warning('Please select an element type')
    return
  }
  
  const newElement = {
    id: generateUUID(),
    type: newElementType.value,
    name: newElementName.value || getDefaultName(newElementType.value),
    enabled: true,
    properties: {},
    children: []
  }
  const schemaMeta = getSchemaForType(newElementType.value)
  if (schemaMeta) {
    newElement.guiClass = schemaMeta.guiClass
    newElement.testClass = schemaMeta.testClass
  } else if (builtinClassMetadata[newElement.type]) {
    newElement.guiClass = builtinClassMetadata[newElement.type].guiClass
    newElement.testClass = builtinClassMetadata[newElement.type].testClass
  }
  
  // Initialize properties based on type
  initializeProperties(newElement)
  
  // Add to current node's children
  if (!currentNode.value.children) {
    currentNode.value.children = []
  }
  currentNode.value.children.push(newElement)
  
  showAddDialog.value = false
  emitChange()
  
  ElMessage.success('Element added successfully')
}

// Delete element
const deleteElement = () => {
  if (!currentNode.value) {
    return
  }
  
  if (currentNode.value.type === 'TestPlan') {
    ElMessage.warning('Cannot delete test plan')
    return
  }
  
  ElMessageBox.confirm(
    `Are you sure you want to delete "${currentNode.value.name}"?`,
    'Confirm Delete',
    {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }
  ).then(() => {
    removeNode(treeData.value[0], currentNode.value.id)
    currentNode.value = null
    emitChange()
    ElMessage.success('Element deleted successfully')
  }).catch(() => {
    // User cancelled
  })
}

// Remove node from tree
const removeNode = (parent, nodeId) => {
  if (!parent.children) return false
  
  const index = parent.children.findIndex(child => child.id === nodeId)
  if (index !== -1) {
    parent.children.splice(index, 1)
    return true
  }
  
  for (const child of parent.children) {
    if (removeNode(child, nodeId)) {
      return true
    }
  }
  
  return false
}

// Property change handler
const onPropertyChange = () => {
  emitChange()
}

// Emit change event
const emitChange = () => {
  emit('update:modelValue', treeData.value[0])
  emit('change', treeData.value[0])
}

// Drag & drop
const allowDrop = (draggingNode, dropNode, type) => {
  // Don't allow dropping as sibling of test plan
  if (dropNode.data.type === 'TestPlan' && type !== 'inner') {
    return false
  }
  return true
}

const allowDrag = (draggingNode) => {
  // Don't allow dragging test plan
  return draggingNode.data.type !== 'TestPlan'
}

// Helper functions
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

const getDefaultName = (type) => {
  const nameMap = {
    'ThreadGroup': 'Thread Group',
    'HTTPSampler': 'HTTP Request',
    'HTTPSamplerProxy': 'HTTP Request',
    'JavaSampler': 'Java Request',
    'CSVDataSet': 'CSV Data Set Config',
    'HeaderManager': 'HTTP Header Manager',
    'ResultCollector': 'View Results Tree'
  }
  if (nameMap[type]) {
    return nameMap[type]
  }
  const schemaMeta = getSchemaForType(type)
  if (schemaMeta) {
    return schemaMeta.label
  }
  return type
}

const initializeProperties = (element) => {
  switch (element.type) {
    case 'ThreadGroup':
      element.properties = {
        numThreads: 1,
        rampTime: 1,
        loops: 1,
        duration: 0,
        delay: 0,
        scheduler: false
      }
      break
    case 'HTTPSampler':
    case 'HTTPSamplerProxy':
      element.properties = {
        protocol: 'https',
        domain: '',
        port: 443,
        path: '',
        method: 'GET',
        contentEncoding: 'UTF-8',
        followRedirects: true,
        autoRedirects: false,
        useKeepAlive: true,
        parameters: [],
        headers: []
      }
      break
    case 'JavaSampler':
      element.properties = {
        classname: '',
        arguments: []
      }
      break
    case 'HeaderManager':
      element.properties = {
        headers: []
      }
      break
    case 'CSVDataSet':
      element.properties = {
        filename: '',
        fileEncoding: 'UTF-8',
        variableNames: '',
        delimiter: ',',
        recycle: true,
        stopThread: false,
        shareMode: 'shareMode.all'
      }
      break
    default: {
      const schemaMeta = getSchemaForType(element.type)
      if (schemaMeta) {
        element.properties = { ...(schemaMeta.defaults || {}) }
      }
      break
    }
  }
}

// Public methods
const getStructure = () => {
  return treeData.value[0]
}

const setStructure = (structure) => {
  treeData.value = [structure]
}

defineExpose({
  getStructure,
  setStructure
})
</script>

<style scoped lang="scss">
.jmx-tree-editor {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .editor-layout {
    display: flex;
    flex: 1;
    gap: 20px;
    overflow: hidden;
    
    .tree-panel,
    .property-panel {
      display: flex;
      flex-direction: column;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      background: #fff;
      overflow: hidden;
      
      .panel-header {
        padding: 15px 20px;
        border-bottom: 1px solid #e4e7ed;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 600;
        }
        
        .toolbar {
          display: flex;
          gap: 8px;
        }
      }
    }
    
    .tree-panel {
      flex: 0 0 400px;
      
      .tree-container {
        flex: 1;
        overflow-y: auto;
        padding: 10px;
      }
    }
    
    .property-panel {
      flex: 1;
      
      .property-container {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        
        &.empty {
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
  }
  
  .custom-tree-node {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    padding: 4px 0;
    
    .el-icon {
      font-size: 18px;
      color: #409eff;
    }
    
    .node-label {
      flex: 1;
    }
    
    .disabled-tag {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>

