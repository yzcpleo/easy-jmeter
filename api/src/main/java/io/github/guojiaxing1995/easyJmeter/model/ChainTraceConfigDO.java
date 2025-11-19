package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 链路追踪配置实体类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_trace_config")
public class ChainTraceConfigDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的测试任务ID
     */
    private Long taskId;

    /**
     * 链路名称
     */
    private String chainName;

    /**
     * 链路描述
     */
    private String chainDescription;

    /**
     * 链路版本
     */
    private String version;

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

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
}