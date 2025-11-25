<template>
  <div class="http-sampler-editor">
    <h4>HTTP Request Settings</h4>
    
    <el-form-item label="Protocol">
      <el-select v-model="localProps.protocol" @change="emitUpdate">
        <el-option label="HTTP" value="http" />
        <el-option label="HTTPS" value="https" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="Server/IP">
      <el-input v-model="localProps.domain" placeholder="example.com" @change="emitUpdate" />
    </el-form-item>
    
    <el-form-item label="Port">
      <el-input-number
        v-model="localProps.port"
        :min="0"
        :max="65535"
        @change="emitUpdate"
      />
      <span class="hint">0 = default port</span>
    </el-form-item>
    
    <el-form-item label="Method">
      <el-select v-model="localProps.method" @change="emitUpdate">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
        <el-option label="PATCH" value="PATCH" />
        <el-option label="HEAD" value="HEAD" />
        <el-option label="OPTIONS" value="OPTIONS" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="Path">
      <el-input v-model="localProps.path" placeholder="/api/v1/resource" @change="emitUpdate" />
    </el-form-item>
    
    <el-form-item label="Content Encoding">
      <el-input v-model="localProps.contentEncoding" @change="emitUpdate" />
    </el-form-item>
    
    <el-divider />
    
    <h4>Parameters</h4>
    <div class="params-list">
      <div v-for="(param, index) in localProps.parameters" :key="index" class="param-item">
        <el-input v-model="param.key" placeholder="Key" @change="emitUpdate" />
        <el-input v-model="param.value" placeholder="Value" @change="emitUpdate" />
        <el-button type="danger" size="small" @click="removeParameter(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
      <el-button type="primary" size="small" @click="addParameter">
        <el-icon><Plus /></el-icon>
        Add Parameter
      </el-button>
    </div>
    
    <el-divider />
    
    <h4>Body Data</h4>
    <el-form-item>
      <el-input
        v-model="localProps.bodyData"
        type="textarea"
        :rows="6"
        placeholder="Request body (JSON, XML, etc.)"
        @change="emitUpdate"
      />
    </el-form-item>
    
    <el-divider />
    
    <h4>Options</h4>
    <el-form-item label="Follow Redirects">
      <el-switch v-model="localProps.followRedirects" @change="emitUpdate" />
    </el-form-item>
    
    <el-form-item label="Auto Redirects">
      <el-switch v-model="localProps.autoRedirects" @change="emitUpdate" />
    </el-form-item>
    
    <el-form-item label="Use KeepAlive">
      <el-switch v-model="localProps.useKeepAlive" @change="emitUpdate" />
    </el-form-item>
    
    <el-form-item label="Connect Timeout">
      <el-input-number
        v-model="localProps.connectTimeout"
        :min="0"
        @change="emitUpdate"
      />
      <span class="hint">Milliseconds, 0 = no timeout</span>
    </el-form-item>
    
    <el-form-item label="Response Timeout">
      <el-input-number
        v-model="localProps.responseTimeout"
        :min="0"
        @change="emitUpdate"
      />
      <span class="hint">Milliseconds, 0 = no timeout</span>
    </el-form-item>
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

// Helper function to parse JMeter properties to friendly format
const parseJMeterProperties = (properties) => {
  if (!properties) return {}
  
  // Extract values from JMeter property names
  const parsed = {
    protocol: properties['HTTPSampler.protocol'] || properties.protocol || 'https',
    domain: properties['HTTPSampler.domain'] || properties.domain || '',
    port: properties['HTTPSampler.port'] || properties.port || (properties['HTTPSampler.protocol'] === 'https' ? 443 : 80),
    path: properties['HTTPSampler.path'] || properties.path || '',
    method: properties['HTTPSampler.method'] || properties.method || 'GET',
    contentEncoding: properties['HTTPSampler.contentEncoding'] || properties.contentEncoding || 'UTF-8',
    followRedirects: parseBool(properties['HTTPSampler.follow_redirects'] || properties.followRedirects, true),
    autoRedirects: parseBool(properties['HTTPSampler.auto_redirects'] || properties.autoRedirects, false),
    useKeepAlive: parseBool(properties['HTTPSampler.use_keepalive'] || properties.useKeepAlive, true),
    connectTimeout: parseInt(properties['HTTPSampler.connect_timeout'] || properties.connectTimeout || 0),
    responseTimeout: parseInt(properties['HTTPSampler.response_timeout'] || properties.responseTimeout || 0),
    parameters: properties.parameters || [],
    bodyData: properties['HTTPSampler.postBodyRaw'] || properties.bodyData || ''
  }
  
  return parsed
}

// Helper function to parse boolean values
const parseBool = (value, defaultValue) => {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') {
    return value.toLowerCase() === 'true'
  }
  return defaultValue
}

// Local properties
const localProps = ref({
  protocol: 'https',
  domain: '',
  port: 443,
  path: '',
  method: 'GET',
  contentEncoding: 'UTF-8',
  followRedirects: true,
  autoRedirects: false,
  useKeepAlive: true,
  connectTimeout: 0,
  responseTimeout: 0,
  parameters: [],
  bodyData: '',
  ...parseJMeterProperties(props.node.properties)
})

// Ensure arrays exist
if (!localProps.value.parameters) {
  localProps.value.parameters = []
}

// Watch for external changes
watch(() => props.node.properties, (newVal) => {
  const parsed = parseJMeterProperties(newVal)
  localProps.value = {
    ...localProps.value,
    ...parsed,
    parameters: parsed.parameters || []
  }
}, { deep: true })

// Add parameter
const addParameter = () => {
  localProps.value.parameters.push({
    key: '',
    value: '',
    description: ''
  })
  emitUpdate()
}

// Remove parameter
const removeParameter = (index) => {
  localProps.value.parameters.splice(index, 1)
  emitUpdate()
}

// Emit update
const emitUpdate = () => {
  props.node.properties = { ...localProps.value }
  emit('update')
}
</script>

<style scoped lang="scss">
.http-sampler-editor {
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
  
  .params-list {
    .param-item {
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

