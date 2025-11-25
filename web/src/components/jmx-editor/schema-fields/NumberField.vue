<template>
  <el-form-item :label="field.label">
    <el-input-number
      v-model="model"
      :min="field.min"
      :max="field.max"
      :step="field.step || 1"
    />
  </el-form-item>
</template>

<script setup>
import { computed, defineProps } from 'vue'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [Number, String], default: 0 }
})

const emit = defineEmits(['update:modelValue', 'change'])

const model = computed({
  get: () => Number(props.modelValue ?? 0),
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})
</script>


