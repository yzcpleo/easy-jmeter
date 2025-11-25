<template>
  <div class="kv-field">
    <label class="kv-label">{{ field.label }}</label>
    <div v-if="model.length" class="kv-list">
      <div
        v-for="(item, index) in model"
        :key="index"
        class="kv-row"
      >
        <el-input
          v-model="item.key"
          :placeholder="field.keyLabel || 'Key'"
          @change="emitChange"
        />
        <el-input
          v-model="item.value"
          :placeholder="field.valueLabel || 'Value'"
          @change="emitChange"
        />
        <el-button
          type="danger"
          size="small"
          @click="removeRow(index)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
    <el-empty v-else :image-size="80" description="No parameters" />
    <el-button
      type="primary"
      size="small"
      class="add-btn"
      @click="addRow"
    >
      <el-icon><Plus /></el-icon>
      {{ field.addLabel || 'Add Row' }}
    </el-button>
  </div>
</template>

<script setup>
import { computed, defineProps } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const model = computed({
  get: () => props.modelValue || [],
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})

const addRow = () => {
  model.value.push({ key: '', value: '' })
  emitChange()
}

const removeRow = (index) => {
  model.value.splice(index, 1)
  emitChange()
}

const emitChange = () => {
  emit('update:modelValue', model.value)
  emit('change', model.value)
}
</script>

<style scoped lang="scss">
.kv-field {
  display: flex;
  flex-direction: column;

  .kv-label {
    font-weight: 500;
    margin-bottom: 8px;
    color: #303133;
  }

  .kv-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 10px;

    .kv-row {
      display: flex;
      gap: 10px;

      .el-input {
        flex: 1;
      }
    }
  }

  .add-btn {
    align-self: flex-start;
  }
}
</style>


