<template>
  <div class="java-sampler-editor">
    <h4>Java Request Settings</h4>
    
    <el-form-item label="Classname">
      <el-input
        v-model="localProps.classname"
        placeholder="com.example.MyJavaSampler"
        @change="emitUpdate"
      />
      <span class="hint">Fully qualified class name</span>
    </el-form-item>
    
    <el-divider />
    
    <h4>Arguments</h4>
    <div class="args-list">
      <div v-for="(arg, index) in localProps.arguments" :key="index" class="arg-item">
        <el-input v-model="arg.name" placeholder="Argument Name" @change="emitUpdate" />
        <el-input v-model="arg.value" placeholder="Argument Value" @change="emitUpdate" />
        <el-button type="danger" size="small" @click="removeArgument(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
      <el-button type="primary" size="small" @click="addArgument">
        <el-icon><Plus /></el-icon>
        Add Argument
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  node: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update'])

// Parse arguments string to array format
// Format: name\nvalue\n=\n (repeated for each argument)
const parseArgumentsString = (argsStr) => {
  if (!argsStr || typeof argsStr !== 'string') {
    return []
  }
  
  const lines = argsStr.split('\n')
  const args = []
  let currentName = null
  let currentValue = []
  let expectingValue = false
  
  for (const line of lines) {
    const trimmed = line.trim()
    
    // Skip empty lines
    if (!trimmed) {
      continue
    }
    
    // If we see "=", it's a separator - save current argument and reset
    if (trimmed === '=') {
      if (currentName) {
        args.push({
          name: currentName,
          value: currentValue.join('\n').trim(),
          description: ''
        })
      }
      currentName = null
      currentValue = []
      expectingValue = false
      continue
    }
    
    // If we don't have a name yet, this is the name
    if (!currentName) {
      currentName = trimmed
      expectingValue = true
    } else if (expectingValue) {
      // This is the value (could be multi-line, especially for JSON)
      currentValue.push(trimmed)
    }
  }
  
  // Handle last argument if there's no trailing "="
  if (currentName) {
    args.push({
      name: currentName,
      value: currentValue.join('\n').trim(),
      description: ''
    })
  }
  
  return args
}

// Local properties
const localProps = ref({
  classname: '',
  arguments: [],
  ...props.node.properties
})

// Ensure arrays exist and convert string format if needed
if (!localProps.value.arguments) {
  localProps.value.arguments = []
} else if (typeof localProps.value.arguments === 'string') {
  // Convert string format to array
  localProps.value.arguments = parseArgumentsString(localProps.value.arguments)
}

// Watch for external changes
watch(() => props.node.properties, (newVal) => {
  let args = newVal.arguments || []
  // Convert string format to array if needed
  if (typeof args === 'string') {
    args = parseArgumentsString(args)
  }
  
  localProps.value = {
    ...localProps.value,
    ...newVal,
    arguments: args
  }
}, { deep: true })

// Add argument
const addArgument = () => {
  localProps.value.arguments.push({
    name: '',
    value: '',
    description: ''
  })
  emitUpdate()
}

// Remove argument
const removeArgument = (index) => {
  localProps.value.arguments.splice(index, 1)
  emitUpdate()
}

// Emit update
const emitUpdate = () => {
  props.node.properties = { ...localProps.value }
  emit('update')
}
</script>

<style scoped lang="scss">
.java-sampler-editor {
  h4 {
    margin: 20px 0 15px 0;
    color: #303133;
    
    &:first-child {
      margin-top: 0;
    }
  }
  
  .hint {
    margin-left: 10px;
    font-size: 12px;
    color: #909399;
  }
  
  .args-list {
    .arg-item {
      display: flex;
      gap: 10px;
      margin-bottom: 10px;
      
      .el-input {
        flex: 1;
      }
    }
  }
}
</style>

