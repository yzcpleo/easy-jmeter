package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 链路延时数据实体类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_latency_data")
public class ChainLatencyDataDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 测试任务ID
     */
    private Long taskId;

    /**
     * 链路ID
     */
    private Long chainId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 请求开始时间戳
     */
    private LocalDateTime requestStartTime;

    /**
     * 请求结束时间戳
     */
    private LocalDateTime requestEndTime;

    /**
     * 延时（毫秒）
     */
    private Long latency;

    /**
     * 是否成功：1-成功，0-失败
     */
    private Integer success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 扩展数据（JSON格式）
     */
    private String extendedData;

    /**
     * 数据收集时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime collectionTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}