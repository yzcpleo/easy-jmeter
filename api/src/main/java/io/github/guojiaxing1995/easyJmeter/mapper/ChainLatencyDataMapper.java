package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainLatencyDataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 链路延时数据Mapper
 *
 * @author Assistant
 * @version 1.0.0
 */
@Mapper
public interface ChainLatencyDataMapper extends BaseMapper<ChainLatencyDataDO> {

    /**
     * 根据请求ID查询延时数据
     *
     * @param requestId 请求ID
     * @return 延时数据列表
     */
    List<ChainLatencyDataDO> selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据任务ID和时间范围查询延时数据
     *
     * @param taskId 任务ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 延时数据列表
     */
    List<ChainLatencyDataDO> selectByTaskIdAndTimeRange(@Param("taskId") Long taskId,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 根据链路ID和时间范围查询延时数据
     *
     * @param chainId 链路ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 延时数据列表
     */
    List<ChainLatencyDataDO> selectByChainIdAndTimeRange(@Param("chainId") Long chainId,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 根据节点ID和时间范围查询延时数据
     *
     * @param nodeId 节点ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 延时数据列表
     */
    List<ChainLatencyDataDO> selectByNodeIdAndTimeRange(@Param("nodeId") Long nodeId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 批量插入延时数据
     *
     * @param dataList 延时数据列表
     * @return 插入记录数
     */
    int batchInsert(@Param("dataList") List<ChainLatencyDataDO> dataList);

    /**
     * 删除指定时间之前的延时数据（用于数据清理）
     *
     * @param beforeTime 时间点
     * @return 删除记录数
     */
    int deleteByCollectionTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计节点延时统计信息
     *
     * @param nodeId 节点ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果（包含平均延时、最大延时、最小延时等）
     */
    ChainLatencyStatsDO getLatencyStats(@Param("nodeId") Long nodeId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 延时统计结果内部类
     */
    class ChainLatencyStatsDO {
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
    }
}