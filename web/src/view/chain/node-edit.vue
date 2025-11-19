<template>
  <div class="node-edit">
    <div class="title">{{ editNodeId ? '编辑节点' : '新增节点' }}</div>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
      <!-- 基本信息 -->
      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">基本信息</span>
          </div>
        </template>
        <el-form-item label="节点名称" prop="node_name">
          <el-input v-model="form.node_name" placeholder="请输入节点名称"></el-input>
        </el-form-item>
        <el-form-item label="节点类型" prop="node_type">
          <el-select v-model="form.node_type" placeholder="请选择节点类型">
            <el-option label="应用服务" value="APPLICATION"></el-option>
            <el-option label="数据库" value="DATABASE"></el-option>
            <el-option label="缓存" value="CACHE"></el-option>
            <el-option label="消息队列" value="MQ"></el-option>
            <el-option label="其他" value="OTHER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="执行顺序" prop="sequence_order">
          <el-input-number v-model="form.sequence_order" :min="1" :max="100"></el-input-number>
        </el-form-item>
        <el-form-item label="节点描述" prop="node_description">
          <el-input 
            v-model="form.node_description" 
            type="textarea"
            :rows="3"
            placeholder="请输入节点描述">
          </el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>

      <!-- 连接配置 -->
      <el-card class="config-card connection-card" shadow="never" :class="{ 'remote-card': form.connection_type !== 'LOCAL' }">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <i class="el-icon-link"></i>
              连接配置
            </span>
          </div>
        </template>
        <el-form-item label="连接方式">
          <el-radio-group v-model="form.connection_type" @change="handleConnectionTypeChange">
            <el-radio label="LOCAL">
              <span>本地节点</span>
              <span class="radio-desc">直接访问本地文件系统</span>
            </el-radio>
            <el-radio label="SSH">
              <span>远程Linux节点</span>
              <span class="radio-desc">通过SSH连接远程Linux服务器</span>
            </el-radio>
            <el-radio label="RDP">
              <span>远程Windows节点</span>
              <span class="radio-desc">通过SMB共享访问Windows服务器</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        
        <template v-if="form.connection_type !== 'LOCAL'">
          <el-divider content-position="left">
            <span class="divider-text">
              <i class="el-icon-server"></i>
              {{ form.connection_type === 'SSH' ? 'SSH连接配置' : 'Windows连接配置' }}
            </span>
          </el-divider>
          
          <div class="remote-config-group">
            <el-form-item label="主机地址" prop="node_host">
              <el-input v-model="form.node_host" placeholder="192.168.1.100 或 example.com">
                <template #prefix>
                  <i class="el-icon-server"></i>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="端口" prop="node_port">
              <el-input-number v-model="form.node_port" :min="1" :max="65535" :precision="0"></el-input-number>
              <span class="form-tip">
                {{ form.connection_type === 'SSH' ? 'SSH端口（默认22）' : 'RDP端口（默认3389）' }}
              </span>
            </el-form-item>
            <el-form-item label="操作系统类型" prop="os_type">
              <el-select v-model="form.os_type" placeholder="请选择操作系统类型">
                <el-option label="Linux" value="LINUX">
                  <span>Linux</span>
                  <span style="margin-left: 10px; color: #409EFF;">🐧</span>
                </el-option>
                <el-option label="Windows" value="WINDOWS">
                  <span>Windows</span>
                  <span style="margin-left: 10px; color: #67C23A;">🪟</span>
                </el-option>
              </el-select>
            </el-form-item>
          </div>

          <el-divider content-position="left">
            <span class="divider-text">
              <i class="el-icon-user"></i>
              认证配置
            </span>
          </el-divider>
          
          <div class="auth-config-group">
            <el-form-item label="用户名" prop="node_username">
              <el-input v-model="form.node_username" placeholder="远程连接用户名">
                <template #prefix>
                  <i class="el-icon-user"></i>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="node_password">
              <el-input v-model="form.node_password" type="password" placeholder="远程连接密码" show-password>
                <template #prefix>
                  <i class="el-icon-lock"></i>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="form.connection_type === 'SSH'" label="SSH私钥路径（可选）" prop="ssh_key_path">
              <el-input v-model="form.ssh_key_path" placeholder="/path/to/private/key">
                <template #prefix>
                  <i class="el-icon-lock"></i>
                </template>
              </el-input>
              <span class="form-tip">
                如果配置了私钥，将优先使用密钥认证
              </span>
            </el-form-item>
          </div>

          <el-divider content-position="left">
            <span class="divider-text">
              <i class="el-icon-timer"></i>
              超时配置
            </span>
          </el-divider>
          
          <div class="timeout-config-group">
            <el-form-item label="连接超时（秒）" prop="connection_timeout">
              <el-input-number v-model="form.connection_timeout" :min="5" :max="300" :precision="0"></el-input-number>
            </el-form-item>
            <el-form-item label="读取超时（秒）" prop="read_timeout">
              <el-input-number v-model="form.read_timeout" :min="10" :max="600" :precision="0"></el-input-number>
            </el-form-item>
          </div>
        </template>
      </el-card>
      
      <!-- 日志配置 -->
      <el-card class="config-card log-config-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <i class="el-icon-document-copy"></i>
              日志配置
            </span>
          </div>
        </template>
        <el-form-item label="日志文件路径" prop="log_path">
          <el-input v-model="form.log_path" :placeholder="form.connection_type === 'LOCAL' ? '/var/log/app/app.log' : (form.os_type === 'WINDOWS' ? 'C:\\logs\\app.log 或 \\\\192.168.1.100\\share\\app.log' : '/var/log/app/app.log')">
            <template #prefix>
              <i class="el-icon-folder-opened"></i>
            </template>
          </el-input>
          <div class="form-tip">
            <div v-if="form.os_type === 'WINDOWS'">
              Windows节点支持：本地路径（如 C:\logs\app.log）或SMB共享路径（如 \\\\192.168.1.100\share\app.log）
            </div>
            <div v-else>
              Linux节点支持：本地路径（如 /var/log/app/app.log）或远程路径（通过SSH访问）
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- 解析规则配置 -->
      <el-card class="config-card parse-config-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <i class="el-icon-setting"></i>
              解析规则配置
            </span>
          </div>
        </template>
        
        <el-form-item label="日志匹配模式" prop="log_pattern">
          <el-input 
            v-model="form.log_pattern" 
            type="textarea"
            :rows="3"
            placeholder="正则表达式或关键字，用于过滤日志行">
          </el-input>
        </el-form-item>
        
        <el-divider content-position="left">
          <span class="divider-text">字段提取规则</span>
        </el-divider>
        
        <div class="pattern-group">
          <el-form-item label="时间戳提取模式" prop="timestamp_pattern">
            <el-input 
              v-model="form.timestamp_pattern" 
              type="textarea"
              :rows="2"
              placeholder="时间戳提取的正则表达式，例如：(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})">
            </el-input>
          </el-form-item>
          <el-form-item label="延时提取模式" prop="latency_pattern">
            <el-input 
              v-model="form.latency_pattern" 
              type="textarea"
              :rows="2"
              placeholder="延时值提取的正则表达式，例如：latency=(\d+)ms">
            </el-input>
          </el-form-item>
          <el-form-item label="请求ID提取模式" prop="request_id_pattern">
            <el-input 
              v-model="form.request_id_pattern" 
              type="textarea"
              :rows="2"
              placeholder="请求ID提取的正则表达式，例如：requestId=([^,]+)">
            </el-input>
          </el-form-item>
        </div>
      </el-card>

      <!-- 解析方式配置 -->
      <el-card class="config-card parse-method-card" shadow="never" :class="{ 'custom-script-card': form.use_custom_script === 1 }">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <i class="el-icon-setting"></i>
              解析方式配置
            </span>
          </div>
        </template>
        
        <el-form-item label="日志解析类型">
          <el-radio-group v-model="form.use_custom_script" @change="handleParseMethodChange">
            <el-radio :label="0">
              <span>标准解析（正则/AWK）</span>
            </el-radio>
            <el-radio :label="1">
              <span>自定义Shell脚本</span>
            </el-radio>
          </el-radio-group>
          <div class="form-tip">
            <div v-if="form.use_custom_script === 1">
              自定义脚本适用于非标准格式的日志，脚本输出格式：requestId|timestamp|latency|originalLine
            </div>
            <div v-else>
              标准解析：根据操作系统和解析方法自动选择AWK脚本或Java正则表达式
            </div>
          </div>
        </el-form-item>
        
        <template v-if="form.use_custom_script === 0">
          <el-divider content-position="left">
            <span class="divider-text">标准解析方法</span>
          </el-divider>
          <el-form-item label="解析方法" prop="parse_method">
            <el-select v-model="form.parse_method" placeholder="请选择解析方法" style="width: 100%;">
              <el-option label="自动（推荐）" value="AUTO">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>自动（推荐）</span>
                  <span style="color: #8492a6; font-size: 12px;">Linux使用AWK，Windows使用Java正则</span>
                </div>
              </el-option>
              <el-option label="AWK脚本" value="AWK">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>AWK脚本</span>
                  <span style="color: #8492a6; font-size: 12px;">仅Linux系统支持</span>
                </div>
              </el-option>
              <el-option label="Java正则表达式" value="JAVA_REGEX">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>Java正则表达式</span>
                  <span style="color: #8492a6; font-size: 12px;">Linux和Windows都支持</span>
                </div>
              </el-option>
            </el-select>
            <div class="form-tip">
              <div v-if="form.parse_method === 'AUTO'">
                系统会根据操作系统类型自动选择：Linux使用AWK脚本，Windows使用Java正则表达式
              </div>
              <div v-else-if="form.parse_method === 'AWK'">
                AWK脚本解析，性能较好，仅Linux系统支持。Windows系统会自动切换到Java正则表达式
              </div>
              <div v-else-if="form.parse_method === 'JAVA_REGEX'">
                Java正则表达式解析，Linux和Windows系统都支持，兼容性好
              </div>
            </div>
          </el-form-item>
        </template>
        
        <template v-if="form.use_custom_script === 1">
          <el-divider content-position="left">
            <span class="divider-text">自定义Shell脚本</span>
          </el-divider>
          <el-form-item label="Shell脚本内容" prop="custom_shell_script">
            <el-input 
              v-model="form.custom_shell_script" 
              type="textarea"
              :rows="15"
              placeholder="请输入Shell脚本，脚本中可以使用 $LOG_PATH 变量表示日志文件路径&#10;输出格式：requestId|timestamp|latency|originalLine&#10;&#10;示例：&#10;#!/bin/bash&#10;while IFS= read -r line; do&#10;  # 解析日志行&#10;  request_id=$(echo &quot;$line&quot; | grep -oP 'requestId=\K[^,]+')&#10;  timestamp=$(echo &quot;$line&quot; | grep -oP '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}')&#10;  latency=$(echo &quot;$line&quot; | grep -oP 'latency=\K\d+')&#10;  echo &quot;$request_id|$timestamp|$latency|$line&quot;&#10;done &lt; &quot;$LOG_PATH&quot;">
            </el-input>
            <div class="form-tip">
              <div><strong>脚本要求：</strong></div>
              <div>1. 脚本必须是有效的bash脚本</div>
              <div>2. 使用 $LOG_PATH 变量表示日志文件路径</div>
              <div>3. 每行输出格式：requestId|timestamp|latency|originalLine</div>
              <div>4. 如果某个字段无法提取，输出空字符串</div>
              <div>5. 脚本会在远程服务器上执行（仅Linux节点支持）</div>
            </div>
          </el-form-item>
        </template>
      </el-card>
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
  name: 'NodeEdit',
  props: {
    editNodeId: {
      type: Number,
      default: null
    },
    chainId: {
      type: [String, Number],
      required: true
    }
  },
  emits: ['editClose'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const loading = ref(false)
    
    const form = reactive({
      chain_id: props.chainId,
      node_name: '',
      node_type: 'APPLICATION',
      sequence_order: 1,
      log_path: '',
      log_pattern: '',
      timestamp_pattern: '',
      latency_pattern: '',
      request_id_pattern: '',
      node_description: '',
      connection_type: 'LOCAL',
      node_host: '',
      node_port: 22,
      node_username: '',
      node_password: '',
      os_type: 'LINUX',
      ssh_key_path: '',
      connection_timeout: 30,
      read_timeout: 60,
      use_custom_script: 0,
      custom_shell_script: '',
      parse_method: 'JAVA_REGEX',
      status: 1
    })
    
    const handleConnectionTypeChange = (value) => {
      if (value === 'LOCAL') {
        form.os_type = 'LINUX'
        form.node_port = 22
        form.use_custom_script = 0 // 本地节点不支持自定义脚本
      } else if (value === 'SSH') {
        form.os_type = 'LINUX'
        form.node_port = form.node_port || 22
      } else if (value === 'RDP') {
        form.os_type = 'WINDOWS'
        form.node_port = form.node_port || 3389
        form.use_custom_script = 0 // Windows节点不支持自定义脚本
      }
    }
    
    const handleParseMethodChange = (value) => {
      if (value === 1) {
        // 使用自定义脚本时，必须是远程Linux节点
        if (form.connection_type !== 'SSH') {
          ElMessage.warning('自定义Shell脚本仅支持远程Linux节点，请先选择"远程Linux节点"')
          form.use_custom_script = 0
        }
      }
    }

    const rules = {
      node_name: [
        { required: true, message: '请输入节点名称', trigger: 'blur' }
      ],
      node_type: [
        { required: true, message: '请选择节点类型', trigger: 'change' }
      ],
      sequence_order: [
        { required: true, message: '请输入执行顺序', trigger: 'blur' }
      ]
    }

    onMounted(() => {
      if (props.editNodeId) {
        getNodeDetail()
      }
    })

    const getNodeDetail = async () => {
      try {
        loading.value = true
        const res = await get(`/v1/chain/node/${props.editNodeId}`, {}, { showBackend: true })
        Object.assign(form, res)
        loading.value = false
      } catch (error) {
        loading.value = false
        console.error('获取节点详情失败:', error)
      }
    }

    const submitForm = () => {
      formRef.value.validate(async valid => {
        if (valid) {
          try {
            loading.value = true
            if (props.editNodeId) {
              await put(`/v1/chain/node/${props.editNodeId}`, form, { showBackend: true })
              ElMessage.success('更新成功')
            } else {
              await post('/v1/chain/node', form, { showBackend: true })
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
      cancel,
      handleConnectionTypeChange,
      handleParseMethodChange
    }
  }
}
</script>

<style lang="scss" scoped>
.node-edit {
  padding: 0 30px;

  .title {
    height: 59px;
    line-height: 59px;
    color: #606266;
    font-size: 16px;
    font-weight: 500;
    text-indent: 40px;
    border-bottom: 1px solid #EBEEF5;
    margin-bottom: 20px;
  }

  .config-card {
    margin-bottom: 20px;
    border: 1px solid #EBEEF5;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;

        i {
          font-size: 18px;
        }
      }
    }
  }

  // 基本信息卡片 - 蓝色主题
  .config-card:first-of-type {
    border-left: 4px solid #409EFF;
    
    .card-title {
      color: #409EFF;
    }
  }

  // 连接配置卡片 - 绿色主题（远程时）
  .connection-card {
    border-left: 4px solid #67C23A;
    
    .card-title {
      color: #67C23A;
    }

    &.remote-card {
      border-left: 4px solid #E6A23C;
      background: linear-gradient(to right, rgba(230, 162, 60, 0.05), transparent);
      
      .card-title {
        color: #E6A23C;
      }
    }
  }

  // 日志配置卡片 - 紫色主题
  .log-config-card {
    border-left: 4px solid #909399;
    
    .card-title {
      color: #909399;
    }
  }

  // 解析规则配置卡片 - 青色主题
  .parse-config-card {
    border-left: 4px solid #409EFF;
    
    .card-title {
      color: #409EFF;
    }
  }

  // 解析方式配置卡片 - 橙色主题（自定义脚本时）
  .parse-method-card {
    border-left: 4px solid #F56C6C;
    
    .card-title {
      color: #F56C6C;
    }

    &.custom-script-card {
      border-left: 4px solid #E6A23C;
      background: linear-gradient(to right, rgba(230, 162, 60, 0.05), transparent);
      
      .card-title {
        color: #E6A23C;
      }
    }
  }

  // 分组样式
  .remote-config-group,
  .auth-config-group,
  .timeout-config-group,
  .pattern-group {
    padding: 10px 0;
    background: #FAFAFA;
    border-radius: 4px;
    margin: 10px 0;
    padding: 15px;
  }

  // 分隔线样式
  .el-divider {
    margin: 20px 0;

    .divider-text {
      color: #606266;
      font-size: 14px;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }

  // 单选框描述
  .el-radio {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    margin-right: 20px;
    margin-bottom: 10px;

    .radio-desc {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
      margin-left: 0;
    }
  }

  // 表单提示文字
  .form-tip {
    margin-top: 5px;
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
  }

  .submit {
    float: left;
    margin-left: 140px;
    margin-top: 20px;
    margin-bottom: 30px;
  }
}
</style>
