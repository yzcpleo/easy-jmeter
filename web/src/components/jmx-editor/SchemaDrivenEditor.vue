<template>
  <div class="schema-driven-editor" v-if="schema">
    <h4>{{ schema.label }} Properties</h4>

    <template v-for="field in schema.fields" :key="field.key">
      <component
        :is="getFieldComponent(field)"
        :field="field"
        v-model="localValues[field.key]"
        @change="emitUpdate"
      />
    </template>
  </div>
  <el-alert
    v-else
    type="warning"
    :closable="false"
    title="Schema missing"
    description="No schema metadata found for this element."
  />
</template>

<script setup>
import { computed, defineProps, defineEmits, reactive, watch } from 'vue'
import StringField from './schema-fields/StringField.vue'
import NumberField from './schema-fields/NumberField.vue'
import BooleanField from './schema-fields/BooleanField.vue'
import SelectField from './schema-fields/SelectField.vue'
import KeyValueListField from './schema-fields/KeyValueListField.vue'
import { getSchemaForType } from './schema/jmxElementSchema'

const props = defineProps({
  node: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update'])

const schema = computed(() => getSchemaForType(props.node.type))

const localValues = reactive({})

const initializeValues = () => {
  if (!schema.value) return

  schema.value.fields.forEach((field) => {
    if (props.node.properties && props.node.properties[field.key] !== undefined) {
      localValues[field.key] = props.node.properties[field.key]
    } else if (field.default !== undefined) {
      localValues[field.key] = field.default
    } else if (schema.value.defaults && schema.value.defaults[field.key] !== undefined) {
      localValues[field.key] = schema.value.defaults[field.key]
    } else {
      localValues[field.key] = field.type === 'kv-list' ? [] : ''
    }
  })
}

watch(
  () => props.node.properties,
  () => initializeValues(),
  { immediate: true, deep: true }
)

const getFieldComponent = (field) => {
  const map = {
    string: StringField,
    number: NumberField,
    boolean: BooleanField,
    select: SelectField,
    'kv-list': KeyValueListField
  }
  return map[field.type] || StringField
}

const emitUpdate = () => {
  props.node.properties = {
    ...(props.node.properties || {}),
    ...localValues
  }
  emit('update')
}
</script>

<style scoped lang="scss">
.schema-driven-editor {
  h4 {
    margin: 0 0 15px 0;
    color: #303133;
  }

  & > * {
    margin-bottom: 12px;
  }
}
</style>


