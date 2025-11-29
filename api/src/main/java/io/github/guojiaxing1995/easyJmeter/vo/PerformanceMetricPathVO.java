package io.github.guojiaxing1995.easyJmeter.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 性能指标路径配置VO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricPathVO {

    /**
     * 路径ID
     */
    private Long id;

    /**
     * 所属链路ID
     */
    private Long chainId;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 起点节点ID
     */
    private Long startNodeId;

    /**
     * 起点节点名称
     */
    private String startNodeName;

    /**
     * 起点时间字段
     */
    private String startTimeField;

    /**
     * 终点节点ID
     */
    private Long endNodeId;

    /**
     * 终点节点名称
     */
    private String endNodeName;

    /**
     * 终点时间字段
     */
    private String endTimeField;

    /**
     * 路径描述
     */
    private String description;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}

