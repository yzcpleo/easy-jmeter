package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 性能指标路径配置实体类
 * 用于定义链路中的性能指标路径，如订单上行穿透时延、订单下行时延、行情时延等
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_performance_metric_path")
public class ChainPerformanceMetricPathDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属链路ID
     */
    private Long chainId;

    /**
     * 指标名称，如：订单上行穿透时延、订单下行时延、行情时延
     */
    private String metricName;

    /**
     * 指标类型：ORDER_UPSTREAM-订单上行，ORDER_DOWNSTREAM-订单下行，MARKET-行情等
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
     * 起点时间字段：REQUEST_START_TIME-收到时间，REQUEST_END_TIME-发出时间
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

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 删除时间
     */
    @TableLogic
    private LocalDateTime deleteTime;
}

