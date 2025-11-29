package io.github.guojiaxing1995.easyJmeter.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 路径穿透时延统计数据VO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathLatencyStatsVO {

    /**
     * 路径ID
     */
    private Long pathId;

    /**
     * 路径名称
     */
    private String pathName;

    /**
     * 总请求数
     */
    private Long totalRequests;

    /**
     * 成功请求数
     */
    private Long successfulRequests;

    /**
     * 失败请求数
     */
    private Long failedRequests;

    /**
     * 平均时延（毫秒）
     */
    private Double avgLatency;

    /**
     * 最大时延（毫秒）
     */
    private Long maxLatency;

    /**
     * 最小时延（毫秒）
     */
    private Long minLatency;

    /**
     * P95时延（毫秒）
     */
    private Double p95Latency;

    /**
     * P99时延（毫秒）
     */
    private Double p99Latency;

    /**
     * 成功率（百分比）
     */
    private Double successRate;
}

