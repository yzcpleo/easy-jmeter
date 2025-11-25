<template>
  <el-form-item :label="field.label">
    <el-select
      v-model="model"
      style="width: 100%;"
    >
      <el-option
        v-for="option in field.options || []"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
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
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})
</script>


