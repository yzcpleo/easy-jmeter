<template>
  <div class="header-manager-editor">
    <h4>HTTP Headers</h4>

    <div v-if="localHeaders.length" class="headers-list">
      <div
        v-for="(header, index) in localHeaders"
        :key="index"
        class="header-item"
      >
        <el-input
          v-model="header.key"
          placeholder="Header Name"
          @change="emitUpdate"
        />
        <el-input
          v-model="header.value"
          placeholder="Header Value"
          @change="emitUpdate"
        />
        <el-button
          type="danger"
          class="remove-btn"
          @click="removeHeader(index)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
    <el-empty
      v-else
      description="No headers defined"
      :image-size="80"
    />

    <el-button
      type="primary"
      size="small"
      class="add-btn"
      @click="addHeader"
    >
      <el-icon><Plus /></el-icon>
      Add Header
    </el-button>
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

const normalizeHeaders = (headers) => {
  if (!Array.isArray(headers)) {
    return []
  }
  return headers.map((header) => ({
    key: header?.key || '',
    value: header?.value || ''
  }))
}

const localHeaders = ref(normalizeHeaders(props.node.properties?.headers))

watch(
  () => props.node.properties?.headers,
  (newVal) => {
    localHeaders.value = normalizeHeaders(newVal)
  },
  { deep: true }
)

const addHeader = () => {
  localHeaders.value.push({ key: '', value: '' })
  emitUpdate()
}

const removeHeader = (index) => {
  localHeaders.value.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  props.node.properties = {
    ...(props.node.properties || {}),
    headers: localHeaders.value
  }
  emit('update')
}
</script>

<style scoped lang="scss">
.header-manager-editor {
  h4 {
    margin: 0 0 15px 0;
    color: #303133;
  }

  .headers-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 15px;

    .header-item {
      display: flex;
      gap: 10px;

      .el-input {
        flex: 1;
      }

      .remove-btn {
        flex: 0 0 40px;
      }
    }
  }

  .add-btn {
    margin-top: 10px;
  }
}
</style>


