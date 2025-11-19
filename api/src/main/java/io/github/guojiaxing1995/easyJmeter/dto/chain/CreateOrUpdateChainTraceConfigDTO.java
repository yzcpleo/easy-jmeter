package io.github.guojiaxing1995.easyJmeter.dto.chain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;

/**
 * 创建或更新链路追踪配置DTO
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateChainTraceConfigDTO {

    /**
     * 链路ID（更新时需要）
     */
    private Long id;

    /**
     * 关联的测试任务ID
     */
    @NotNull(message = "测试任务ID不能为空")
    private Long taskId;

    /**
     * 链路名称
     */
    @NotBlank(message = "链路名称不能为空")
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
     * 链路状态：1-启用，0-禁用
     */
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    /**
     * 节点配置列表
     */
    private List<NodeConfigDTO> nodeConfigs;

    /**
     * 节点配置内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeConfigDTO {

        /**
         * 节点ID（更新时需要）
         */
        private Long id;

        /**
         * 节点名称
         */
        @NotBlank(message = "节点名称不能为空")
        private String nodeName;

        /**
         * 节点类型：APPLICATION-应用，DATABASE-数据库，CACHE-缓存等
         */
        @NotBlank(message = "节点类型不能为空")
        private String nodeType;

        /**
         * 节点描述
         */
        private String nodeDescription;

        /**
         * 执行顺序
         */
        @Min(value = 1, message = "执行顺序必须大于0")
        private Integer sequenceOrder;

        /**
         * 日志文件路径
         */
        private String logPath;

        /**
         * 日志匹配模式（正则表达式）
         */
        private String logPattern;

        /**
         * 时间戳提取模式（正则表达式）
         */
        private String timestampPattern;

        /**
         * 延时提取模式（正则表达式）
         */
        private String latencyPattern;

        /**
         * 请求ID提取模式（正则表达式）
         */
        private String requestIdPattern;

        /**
         * 数据映射配置（JSON格式）
         */
        private String dataMapping;

        /**
         * 节点状态：1-启用，0-禁用
         */
        @Min(value = 0, message = "状态值不合法")
        @Max(value = 1, message = "状态值不合法")
        private Integer status;
    }
}