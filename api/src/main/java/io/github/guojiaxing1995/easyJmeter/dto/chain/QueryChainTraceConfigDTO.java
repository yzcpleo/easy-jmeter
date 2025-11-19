package io.github.guojiaxing1995.easyJmeter.dto.chain;

import io.github.guojiaxing1995.easyJmeter.dto.query.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询链路追踪配置DTO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryChainTraceConfigDTO extends BasePageDTO {

    /**
     * 测试任务ID
     */
    private Long taskId;

    /**
     * 链路名称（模糊查询）
     */
    private String chainName;

    /**
     * 链路版本
     */
    private String version;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 开始创建时间
     */
    private String startCreatedTime;

    /**
     * 结束创建时间
     */
    private String endCreatedTime;
}