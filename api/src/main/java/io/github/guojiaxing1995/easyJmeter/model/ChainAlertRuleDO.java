package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 链路告警规则实体类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_alert_rule")
public class ChainAlertRuleDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联链路ID
     */
    private Long chainId;

    /**
     * 关联节点ID
     */
    private Long nodeId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型：LATENCY-延时，ERROR_RATE-错误率，AVAILABILITY-可用性
     */
    private String ruleType;

    /**
     * 阈值
     */
    private BigDecimal thresholdValue;

    /**
     * 比较操作符：GT-大于，LT-小于，EQ-等于
     */
    private String comparisonOperator;

    /**
     * 时间窗口（秒）
     */
    private Integer timeWindow;

    /**
     * 告警级别：INFO，WARNING，ERROR，CRITICAL
     */
    private String alertLevel;

    /**
     * 通知配置（JSON格式）
     */
    private String notificationConfig;

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