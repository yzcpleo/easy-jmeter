<template>
  <div class="csv-dataset-editor">
    <h4>CSV Settings</h4>

    <el-form-item label="File Path">
      <el-input
        v-model="localProps.filename"
        placeholder="/path/to/file.csv"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="File Encoding">
      <el-input
        v-model="localProps.fileEncoding"
        placeholder="UTF-8"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="Variable Names">
      <el-input
        v-model="localProps.variableNames"
        placeholder="col1,col2,col3"
        @change="emitUpdate"
      />
      <span class="hint">Leave empty to use the first row</span>
    </el-form-item>

    <el-form-item label="Delimiter">
      <el-input
        v-model="localProps.delimiter"
        maxlength="1"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-divider />

    <el-form-item label="Ignore First Line">
      <el-switch
        v-model="localProps.ignoreFirstLine"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="Recycle on EOF">
      <el-switch
        v-model="localProps.recycle"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="Stop Thread on EOF">
      <el-switch
        v-model="localProps.stopThread"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="Sharing Mode">
      <el-select
        v-model="localProps.shareMode"
        @change="emitUpdate"
      >
        <el-option
          v-for="mode in shareModes"
          :key="mode.value"
          :label="mode.label"
          :value="mode.value"
        />
      </el-select>
    </el-form-item>
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

const defaultProps = {
  filename: '',
  fileEncoding: 'UTF-8',
  variableNames: '',
  delimiter: ',',
  ignoreFirstLine: false,
  recycle: true,
  stopThread: false,
  shareMode: 'shareMode.all'
}

const shareModes = [
  { label: 'All Threads', value: 'shareMode.all' },
  { label: 'Current Thread Group', value: 'shareMode.group' },
  { label: 'Current Thread', value: 'shareMode.thread' }
]

const localProps = ref({
  ...defaultProps,
  ...(props.node.properties || {})
})

watch(
  () => props.node.properties,
  (newVal) => {
    localProps.value = {
      ...defaultProps,
      ...(newVal || {})
    }
  },
  { deep: true }
)

const emitUpdate = () => {
  props.node.properties = {
    ...(props.node.properties || {}),
    ...localProps.value
  }
  emit('update')
}
</script>

<style scoped lang="scss">
.csv-dataset-editor {
  h4 {
    margin: 0 0 15px 0;
    color: #303133;
  }

  .hint {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }
}
</style>


