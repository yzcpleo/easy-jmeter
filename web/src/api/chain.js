import { get, post, put, _delete } from '@/lin/plugin/axios'

// 获取链路列表
export function getChains(query) {
  return get('v1/chain/trace/page', query)
}

// 创建链路
export function createChain(data) {
  return post('v1/chain/trace', data)
}

// 更新链路
export function updateChain(data) {
  return put(`v1/chain/trace/${data.id}`, data)
}

// 删除链路
export function deleteChain(id) {
  return _delete(`v1/chain/trace/${id}`)
}

// 获取节点列表 - 通过链路ID获取节点配置
export function getNodes(query) {
  // 节点数据包含在链路配置中，这里返回空数组或通过链路ID获取
  return Promise.resolve({ data: { items: [] } })
}

// 创建节点 - 暂不支持，节点通过链路配置管理
export function createNode(data) {
  return Promise.resolve({ data: {} })
}

// 更新节点 - 暂不支持，节点通过链路配置管理
export function updateNode(data) {
  return Promise.resolve({ data: {} })
}

// 删除节点 - 暂不支持，节点通过链路配置管理
export function deleteNode(id) {
  return Promise.resolve({ data: {} })
}

// 启动数据收集
export function startCollection(data) {
  const { taskId, chainId } = data
  return post(`v1/chain/collection/start/${taskId}/${chainId}`)
}

// 停止数据收集
export function stopCollection(data) {
  const { taskId, chainId } = data
  return post(`v1/chain/collection/stop/${taskId}/${chainId}`)
}

// 获取收集状态
export function getCollectionStatus(taskId, chainId) {
  return get(`v1/chain/collection/status/${taskId}/${chainId}`)
}

// 获取链路延时数据
export function getChainLatency(requestId) {
  return get(`v1/chain/collection/latency/${requestId}`)
}

// 获取节点性能统计
export function getNodeStats(nodeId, startTime, endTime) {
  return get(`v1/chain/collection/node-stats/${nodeId}`, { startTime, endTime })
}

// 获取链路性能统计
export function getChainStats(chainId, startTime, endTime) {
  return get(`v1/chain/collection/chain-stats/${chainId}`, { startTime, endTime })
}

// 获取延时数据列表
export function getLatencyData(query) {
  return get('v1/chain/collection/data', query)
}