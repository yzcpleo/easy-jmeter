<template>
  <div class="chain-edit">
    <div class="title">{{ editChainId ? '编辑链路' : '新增链路' }}</div>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="链路名称" prop="chain_name">
        <el-input v-model="form.chain_name" placeholder="请输入链路名称"></el-input>
      </el-form-item>
      <el-form-item label="关联任务ID" prop="task_id">
        <el-input v-model="form.task_id" placeholder="请输入关联的测试任务ID"></el-input>
      </el-form-item>
      <el-form-item label="链路描述" prop="chain_description">
        <el-input 
          v-model="form.chain_description" 
          type="textarea" 
          :rows="4"
          placeholder="请输入链路描述">
        </el-input>
      </el-form-item>
      <el-form-item label="版本" prop="version">
        <el-input v-model="form.version" placeholder="版本号，默认1.0"></el-input>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <div class="submit">
      <el-button type="primary" @click="submitForm" :loading="loading">提 交</el-button>
      <el-button @click="cancel">返 回</el-button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { get, post, put } from '@/lin/plugin/axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'ChainEdit',
  props: {
    editChainId: {
      type: Number,
      default: null
    }
  },
  emits: ['editClose'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const loading = ref(false)
    
    const form = reactive({
      chain_name: '',
      task_id: '',
      chain_description: '',
      version: '1.0',
      status: 1
    })

    const rules = {
      chain_name: [
        { required: true, message: '请输入链路名称', trigger: 'blur' }
      ],
      task_id: [
        { required: true, message: '请输入关联任务ID', trigger: 'blur' }
      ]
    }

    onMounted(() => {
      if (props.editChainId) {
        getChainDetail()
      }
    })

    const getChainDetail = async () => {
      try {
        loading.value = true
        const res = await get(`/v1/chain/trace/${props.editChainId}`, {}, { showBackend: true })
        Object.assign(form, res)
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('获取链路详情失败:', error)
      }
    }

    const submitForm = () => {
      formRef.value.validate(async valid => {
        if (valid) {
          try {
            loading.value = true
            if (props.editChainId) {
              await put(`/v1/chain/trace/${props.editChainId}`, form, { showBackend: true })
              ElMessage.success('更新成功')
            } else {
              await post('/v1/chain/trace', form, { showBackend: true })
              ElMessage.success('创建成功')
            }
            loading.value = false
            emit('editClose')
          } catch (error) {
            loading.value = false
            console.error('提交失败:', error)
          }
        }
      })
    }

    const cancel = () => {
      emit('editClose')
    }

    return {
      formRef,
      form,
      rules,
      loading,
      submitForm,
      cancel
    }
  }
}
</script>

<style lang="scss" scoped>
.chain-edit {
  padding: 0 30px;

  .title {
    height: 59px;
    line-height: 59px;
    color: #606266;
    font-size: 16px;
    font-weight: 500;
    text-indent: 40px;
    border-bottom: 1px solid #EBEEF5;
  }

  .submit {
    float: left;
    margin-left: 120px;
    margin-top: 20px;
  }
}
</style>
