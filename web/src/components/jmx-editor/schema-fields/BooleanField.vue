<template>
  <el-form-item :label="field.label">
    <el-switch
      v-model="model"
    />
  </el-form-item>
</template>

<script setup>
import { computed, defineProps } from 'vue'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [Boolean, String], default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])

const model = computed({
  get: () => {
    if (typeof props.modelValue === 'string') {
      return props.modelValue === 'true'
    }
    return Boolean(props.modelValue)
  },
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})
</script>


