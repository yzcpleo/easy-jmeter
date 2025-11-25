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

// Local properties
const localProps = ref({
  classname: '',
  arguments: [],
  ...props.node.properties
})

// Ensure arrays exist
if (!localProps.value.arguments) {
  localProps.value.arguments = []
}

// Watch for external changes
watch(() => props.node.properties, (newVal) => {
  localProps.value = {
    ...localProps.value,
    ...newVal,
    arguments: newVal.arguments || []
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

