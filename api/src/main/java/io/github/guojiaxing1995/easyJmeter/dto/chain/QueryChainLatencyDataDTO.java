package io.github.guojiaxing1995.easyJmeter.dto.chain;

import io.github.guojiaxing1995.easyJmeter.dto.query.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询链路延时数据DTO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryChainLatencyDataDTO extends BasePageDTO {

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
     * 节点名称（模糊查询）
     */
    private String nodeName;

    /**
     * 是否成功：1-成功，0-失败
     */
    private Integer success;

    /**
     * 最小延时（毫秒）
     */
    private Long minLatency;

    /**
     * 最大延时（毫秒）
     */
    private Long maxLatency;

    /**
     * 开始收集时间（yyyy-MM-dd HH:mm:ss）
     */
    private String startCollectionTime;

    /**
     * 结束收集时间（yyyy-MM-dd HH:mm:ss）
     */
    private String endCollectionTime;

    /**
     * 是否包含错误信息
     */
    private Boolean includeErrors;
}