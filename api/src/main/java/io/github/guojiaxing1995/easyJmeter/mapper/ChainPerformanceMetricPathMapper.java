package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainPerformanceMetricPathDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能指标路径配置Mapper
 *
 * @author Assistant
 * @version 1.0.0
 */
@Mapper
public interface ChainPerformanceMetricPathMapper extends BaseMapper<ChainPerformanceMetricPathDO> {

    /**
     * 根据链路ID查询所有启用的路径配置
     *
     * @param chainId 链路ID
     * @return 路径配置列表
     */
    List<ChainPerformanceMetricPathDO> selectByChainId(@Param("chainId") Long chainId);

    /**
     * 根据指标类型查询路径配置
     *
     * @param chainId 链路ID
     * @param metricType 指标类型
     * @return 路径配置列表
     */
    List<ChainPerformanceMetricPathDO> selectByChainIdAndMetricType(
            @Param("chainId") Long chainId,
            @Param("metricType") String metricType);

    /**
     * 查询路径穿透时延统计数据
     * 计算从起点节点的request_start_time到终点节点的request_end_time的时间差
     *
     * @param pathId 路径ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时延统计数据
     */
    PathLatencyStats selectPathLatencyStats(
            @Param("pathId") Long pathId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 路径时延统计数据内部类
     */
    class PathLatencyStats {
        private Long totalRequests;
        private Long successfulRequests;
        private Long failedRequests;
        private Double avgLatency;
        private Long maxLatency;
        private Long minLatency;
        private Double p95Latency;
        private Double p99Latency;

        // Getters and Setters
        public Long getTotalRequests() {
            return totalRequests;
        }

        public void setTotalRequests(Long totalRequests) {
            this.totalRequests = totalRequests;
        }

        public Long getSuccessfulRequests() {
            return successfulRequests;
        }

        public void setSuccessfulRequests(Long successfulRequests) {
            this.successfulRequests = successfulRequests;
        }

        public Long getFailedRequests() {
            return failedRequests;
        }

        public void setFailedRequests(Long failedRequests) {
            this.failedRequests = failedRequests;
        }

        public Double getAvgLatency() {
            return avgLatency;
        }

        public void setAvgLatency(Double avgLatency) {
            this.avgLatency = avgLatency;
        }

        public Long getMaxLatency() {
            return maxLatency;
        }

        public void setMaxLatency(Long maxLatency) {
            this.maxLatency = maxLatency;
        }

        public Long getMinLatency() {
            return minLatency;
        }

        public void setMinLatency(Long minLatency) {
            this.minLatency = minLatency;
        }

        public Double getP95Latency() {
            return p95Latency;
        }

        public void setP95Latency(Double p95Latency) {
            this.p95Latency = p95Latency;
        }

        public Double getP99Latency() {
            return p99Latency;
        }

        public void setP99Latency(Double p99Latency) {
            this.p99Latency = p99Latency;
        }
    }
}

