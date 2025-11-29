package io.github.guojiaxing1995.easyJmeter.dto.chain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建或更新性能指标路径配置DTO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdatePerformanceMetricPathDTO {

    /**
     * 路径ID（更新时需要）
     */
    private Long id;

    /**
     * 所属链路ID
     */
    @NotNull(message = "链路ID不能为空")
    private Long chainId;

    /**
     * 指标名称，如：订单上行穿透时延、订单下行时延、行情时延
     */
    @NotBlank(message = "指标名称不能为空")
    private String metricName;

    /**
     * 指标类型：用户自定义填写，如：订单上行、订单下行、行情等
     */
    @NotBlank(message = "指标类型不能为空")
    private String metricType;

    /**
     * 起点节点ID
     */
    @NotNull(message = "起点节点ID不能为空")
    private Long startNodeId;

    /**
     * 起点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间
     */
    private String startTimeField;

    /**
     * 终点节点ID
     */
    @NotNull(message = "终点节点ID不能为空")
    private Long endNodeId;

    /**
     * 终点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间
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
}

