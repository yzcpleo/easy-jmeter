package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 链路节点配置实体类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_node_config")
public class ChainNodeConfigDO {

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
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型：APPLICATION-应用，DATABASE-数据库，CACHE-缓存等
     */
    private String nodeType;

    /**
     * 节点描述
     */
    private String nodeDescription;

    /**
     * 执行顺序
     */
    private Integer sequenceOrder;

    /**
     * 日志文件路径
     */
    private String logPath;

    /**
     * 日志匹配模式
     */
    private String logPattern;

    /**
     * 时间戳提取模式
     */
    private String timestampPattern;

    /**
     * 延时提取模式
     */
    private String latencyPattern;

    /**
     * 请求ID提取模式
     */
    private String requestIdPattern;

    /**
     * 数据映射配置（JSON格式）
     */
    private String dataMapping;

    /**
     * 节点主机地址（IP或域名），为空表示本地节点
     */
    private String nodeHost;

    /**
     * SSH端口（Linux）或RDP端口（Windows），默认22
     */
    private Integer nodePort;

    /**
     * 远程连接用户名
     */
    private String nodeUsername;

    /**
     * 远程连接密码（加密存储）
     */
    private String nodePassword;

    /**
     * 操作系统类型：LINUX- Linux系统，WINDOWS- Windows系统
     */
    private String osType;

    /**
     * 连接类型：LOCAL-本地，SSH- SSH连接，RDP- RDP连接（Windows）
     */
    private String connectionType;

    /**
     * SSH私钥路径（可选，优先使用密钥认证）
     */
    private String sshKeyPath;

    /**
     * 连接超时时间（秒），默认30
     */
    private Integer connectionTimeout;

    /**
     * 读取超时时间（秒），默认60
     */
    private Integer readTimeout;

    /**
     * 自定义Shell脚本（用于解析非标准格式日志）
     * 输出格式：requestId|timestamp|latency|originalLine
     */
    private String customShellScript;

    /**
     * 是否使用自定义脚本：1-是，0-否（使用AWK或正则）
     */
    private Integer useCustomScript;

    /**
     * 解析方式：AUTO-自动（Linux用AWK，Windows用Java正则），AWK-AWK脚本，JAVA_REGEX-Java正则表达式
     */
    private String parseMethod;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

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