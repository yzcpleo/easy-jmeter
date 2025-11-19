package io.github.guojiaxing1995.easyJmeter.chain.service;

import io.github.guojiaxing1995.easyJmeter.model.ChainLatencyDataDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 链路数据收集服务接口
 *
 * @author Assistant
 * @version 1.0.0
 */
public interface ChainDataCollectionService {

    /**
     * 启动数据收集
     *
     * @param taskId 任务ID
     * @param chainId 链路ID
     * @return 收集任务ID
     */
    String startDataCollection(Long taskId, Long chainId);

    /**
     * 停止数据收集
     *
     * @param taskId 任务ID
     * @param chainId 链路ID
     * @return 是否成功
     */
    boolean stopDataCollection(Long taskId, Long chainId);

    /**
     * 解析日志文件并收集数据
     *
     * @param taskId 任务ID
     * @param nodeConfig 节点配置
     * @param logFilePath 日志文件路径
     * @return 收集的数据列表
     */
    List<ChainLatencyDataDO> collectLogData(Long taskId, ChainNodeConfigDO nodeConfig, String logFilePath);

    /**
     * 实时处理日志数据
     *
     * @param taskId 任务ID
     * @param nodeConfig 节点配置
     * @param logContent 日志内容
     * @return 处理后的延时数据
     */
    ChainLatencyDataDO processLogData(Long taskId, ChainNodeConfigDO nodeConfig, String logContent);

    /**
     * 关联同一请求的多个节点数据
     *
     * @param requestId 请求ID
     * @return 关联的延时数据列表
     */
    List<ChainLatencyDataDO> correlateRequestData(String requestId);

    /**
     * 计算链路总延时
     *
     * @param requestId 请求ID
     * @return 链路总延时
     */
    Long calculateChainLatency(String requestId);

    /**
     * 获取数据收集状态
     *
     * @param taskId 任务ID
     * @param chainId 链路ID
     * @return 收集状态信息
     */
    DataCollectionStatus getDataCollectionStatus(Long taskId, Long chainId);

    /**
     * 批量保存延时数据
     *
     * @param latencyDataList 延时数据列表
     * @return 保存的记录数
     */
    int batchSaveLatencyData(List<ChainLatencyDataDO> latencyDataList);

    /**
     * 清理过期的延时数据
     *
     * @param beforeTime 时间点
     * @return 清理的记录数
     */
    int cleanupExpiredData(LocalDateTime beforeTime);

    /**
     * 获取节点性能统计
     *
     * @param nodeId 节点ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性能统计信息
     */
    NodePerformanceStats getNodePerformanceStats(Long nodeId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取链路性能统计
     *
     * @param chainId 链路ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 链路性能统计信息
     */
    ChainPerformanceStats getChainPerformanceStats(Long chainId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 数据收集状态枚举
     */
    enum CollectionStatus {
        IDLE,           // 空闲
        COLLECTING,     // 收集中
        STOPPED,        // 已停止
        ERROR           // 错误
    }

    /**
     * 数据收集状态信息
     */
    class DataCollectionStatus {
        private Long taskId;
        private Long chainId;
        private String collectionTaskId;
        private CollectionStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long totalRecords;
        private Long successRecords;
        private Long errorRecords;
        private String errorMessage;

        // Getters and Setters
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public Long getChainId() { return chainId; }
        public void setChainId(Long chainId) { this.chainId = chainId; }

        public String getCollectionTaskId() { return collectionTaskId; }
        public void setCollectionTaskId(String collectionTaskId) { this.collectionTaskId = collectionTaskId; }

        public CollectionStatus getStatus() { return status; }
        public void setStatus(CollectionStatus status) { this.status = status; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public Long getTotalRecords() { return totalRecords; }
        public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }

        public Long getSuccessRecords() { return successRecords; }
        public void setSuccessRecords(Long successRecords) { this.successRecords = successRecords; }

        public Long getErrorRecords() { return errorRecords; }
        public void setErrorRecords(Long errorRecords) { this.errorRecords = errorRecords; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * 节点性能统计信息
     */
    class NodePerformanceStats {
        private Long nodeId;
        private String nodeName;
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double avgLatency;
        private Long maxLatency;
        private Long minLatency;
        private Double p95Latency;
        private Double p99Latency;
        private Double successRate;

        // Getters and Setters
        public Long getNodeId() { return nodeId; }
        public void setNodeId(Long nodeId) { this.nodeId = nodeId; }

        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }

        public Long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }

        public Long getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(Long successfulRequests) { this.successfulRequests = successfulRequests; }

        public Long getFailedRequests() { return failedRequests; }
        public void setFailedRequests(Long failedRequests) { this.failedRequests = failedRequests; }

        public Double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(Double avgLatency) { this.avgLatency = avgLatency; }

        public Long getMaxLatency() { return maxLatency; }
        public void setMaxLatency(Long maxLatency) { this.maxLatency = maxLatency; }

        public Long getMinLatency() { return minLatency; }
        public void setMinLatency(Long minLatency) { this.minLatency = minLatency; }

        public Double getP95Latency() { return p95Latency; }
        public void setP95Latency(Double p95Latency) { this.p95Latency = p95Latency; }

        public Double getP99Latency() { return p99Latency; }
        public void setP99Latency(Double p99Latency) { this.p99Latency = p99Latency; }

        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    }

    /**
     * 链路性能统计信息
     */
    class ChainPerformanceStats {
        private Long chainId;
        private String chainName;
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double avgChainLatency;
        private Long maxChainLatency;
        private Long minChainLatency;
        private Double p95ChainLatency;
        private Double p99ChainLatency;
        private Double successRate;
        private Map<Long, NodePerformanceStats> nodeStats;
        private List<String> bottleneckNodes;

        // Getters and Setters
        public Long getChainId() { return chainId; }
        public void setChainId(Long chainId) { this.chainId = chainId; }

        public String getChainName() { return chainName; }
        public void setChainName(String chainName) { this.chainName = chainName; }

        public Long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }

        public Long getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(Long successfulRequests) { this.successfulRequests = successfulRequests; }

        public Long getFailedRequests() { return failedRequests; }
        public void setFailedRequests(Long failedRequests) { this.failedRequests = failedRequests; }

        public Double getAvgChainLatency() { return avgChainLatency; }
        public void setAvgChainLatency(Double avgChainLatency) { this.avgChainLatency = avgChainLatency; }

        public Long getMaxChainLatency() { return maxChainLatency; }
        public void setMaxChainLatency(Long maxChainLatency) { this.maxChainLatency = maxChainLatency; }

        public Long getMinChainLatency() { return minChainLatency; }
        public void setMinChainLatency(Long minChainLatency) { this.minChainLatency = minChainLatency; }

        public Double getP95ChainLatency() { return p95ChainLatency; }
        public void setP95ChainLatency(Double p95ChainLatency) { this.p95ChainLatency = p95ChainLatency; }

        public Double getP99ChainLatency() { return p99ChainLatency; }
        public void setP99ChainLatency(Double p99ChainLatency) { this.p99ChainLatency = p99ChainLatency; }

        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }

        public Map<Long, NodePerformanceStats> getNodeStats() { return nodeStats; }
        public void setNodeStats(Map<Long, NodePerformanceStats> nodeStats) { this.nodeStats = nodeStats; }

        public List<String> getBottleneckNodes() { return bottleneckNodes; }
        public void setBottleneckNodes(List<String> bottleneckNodes) { this.bottleneckNodes = bottleneckNodes; }
    }
}