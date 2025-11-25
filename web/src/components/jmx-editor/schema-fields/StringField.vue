<template>
  <el-form-item :label="field.label">
    <el-input
      v-model="model"
      :placeholder="field.placeholder || ''"
    />
  </el-form-item>
</template>

<script setup>
import { computed, defineProps } from 'vue'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number], default: '' }
})

const emit = defineEmits(['update:modelValue', 'change'])

const model = computed({
  get: () => props.modelValue ?? '',
  set: (val) => emitChange(val)
})

const emitChange = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>


