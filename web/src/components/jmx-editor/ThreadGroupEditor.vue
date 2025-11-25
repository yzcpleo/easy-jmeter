<template>
  <div class="thread-group-editor">
    <h4>Thread Group Settings</h4>
    
    <el-form-item label="Number of Threads">
      <el-input-number
        v-model="localProps.numThreads"
        :min="1"
        :max="10000"
        @change="emitUpdate"
      />
    </el-form-item>
    
    <el-form-item label="Ramp-Up Period">
      <el-input-number
        v-model="localProps.rampTime"
        :min="0"
        :max="3600"
        @change="emitUpdate"
      />
      <span class="hint">Seconds</span>
    </el-form-item>
    
    <el-form-item label="Loop Count">
      <el-input-number
        v-model="localProps.loops"
        :min="-1"
        @change="emitUpdate"
      />
      <span class="hint">-1 for infinite</span>
    </el-form-item>
    
    <el-form-item label="Use Scheduler">
      <el-switch v-model="localProps.scheduler" @change="emitUpdate" />
    </el-form-item>
    
    <template v-if="localProps.scheduler">
      <el-form-item label="Duration">
        <el-input-number
          v-model="localProps.duration"
          :min="0"
          @change="emitUpdate"
        />
        <span class="hint">Seconds, 0 = disabled</span>
      </el-form-item>
      
      <el-form-item label="Startup Delay">
        <el-input-number
          v-model="localProps.delay"
          :min="0"
          @change="emitUpdate"
        />
        <span class="hint">Seconds</span>
      </el-form-item>
    </template>
  </div>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'

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
    numThreads: parseInt(properties['ThreadGroup.num_threads'] || properties.numThreads || 1),
    rampTime: parseInt(properties['ThreadGroup.ramp_time'] || properties.rampTime || 1),
    loops: parseInt(properties['LoopController.loops'] || properties.loops || 1),
    duration: parseInt(properties['ThreadGroup.duration'] || properties.duration || 0),
    delay: parseInt(properties['ThreadGroup.delay'] || properties.delay || 0),
    scheduler: parseBool(properties['ThreadGroup.scheduler'] || properties.scheduler, false)
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
  numThreads: 1,
  rampTime: 1,
  loops: 1,
  duration: 0,
  delay: 0,
  scheduler: false,
  ...parseJMeterProperties(props.node.properties)
})

// Watch for external changes
watch(() => props.node.properties, (newVal) => {
  const parsed = parseJMeterProperties(newVal)
  localProps.value = { ...localProps.value, ...parsed }
}, { deep: true })

// Emit update
const emitUpdate = () => {
  props.node.properties = { ...localProps.value }
  emit('update')
}
</script>

<style scoped lang="scss">
.thread-group-editor {
  h4 {
    margin: 0 0 20px 0;
    color: #303133;
  }
  
  .hint {
    margin-left: 10px;
    font-size: 12px;
    color: #909399;
  }
}
</style>

