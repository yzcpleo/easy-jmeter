package io.github.guojiaxing1995.easyJmeter.chain.service;

import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志解析服务
 * 支持Windows（Java正则表达式）和Linux（AWK脚本）两种方式
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class LogParserService {

    @Autowired
    private AwkScriptExecutor awkScriptExecutor;

    /**
     * 解析日志内容
     *
     * @param logContent 日志内容（如果是自定义脚本，logContent已经是解析后的结果）
     * @param nodeConfig 节点配置
     * @return 解析结果列表，格式：[requestId, timestamp, latency, originalLine]
     */
    public List<String[]> parseLogContent(String logContent, ChainNodeConfigDO nodeConfig) {
        if (logContent == null || logContent.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 如果使用自定义脚本，logContent已经是解析后的结果，直接解析输出格式
        if (nodeConfig.getUseCustomScript() != null && nodeConfig.getUseCustomScript() == 1) {
            return parseCustomScriptOutput(logContent);
        }

        // 根据解析方式选择解析方法
        String parseMethod = nodeConfig.getParseMethod();
        if (parseMethod == null || parseMethod.trim().isEmpty() || "AUTO".equalsIgnoreCase(parseMethod)) {
            // 自动模式：根据操作系统类型选择
            String osType = nodeConfig.getOsType() != null ? nodeConfig.getOsType().toUpperCase() : "LINUX";
            if ("WINDOWS".equals(osType)) {
                return parseLogWithJavaRegex(logContent, nodeConfig);
            } else {
                return parseLogWithAwk(logContent, nodeConfig);
            }
        } else if ("JAVA_REGEX".equalsIgnoreCase(parseMethod)) {
            // 强制使用Java正则表达式
            return parseLogWithJavaRegex(logContent, nodeConfig);
        } else if ("AWK".equalsIgnoreCase(parseMethod)) {
            // 强制使用AWK脚本（仅Linux支持）
            String osType = nodeConfig.getOsType() != null ? nodeConfig.getOsType().toUpperCase() : "LINUX";
            if ("WINDOWS".equals(osType)) {
                log.warn("Windows系统不支持AWK解析，自动切换到Java正则表达式，节点: {}", nodeConfig.getNodeName());
                return parseLogWithJavaRegex(logContent, nodeConfig);
            }
            return parseLogWithAwk(logContent, nodeConfig);
        } else {
            // 未知的解析方式，使用自动模式
            log.warn("未知的解析方式: {}，使用自动模式，节点: {}", parseMethod, nodeConfig.getNodeName());
            String osType = nodeConfig.getOsType() != null ? nodeConfig.getOsType().toUpperCase() : "LINUX";
            if ("WINDOWS".equals(osType)) {
                return parseLogWithJavaRegex(logContent, nodeConfig);
            } else {
                return parseLogWithAwk(logContent, nodeConfig);
            }
        }
    }

    /**
     * 解析自定义脚本的输出
     * 输出格式：requestId|timestamp|latency|originalLine
     *
     * @param scriptOutput 脚本输出内容
     * @return 解析结果列表
     */
    private List<String[]> parseCustomScriptOutput(String scriptOutput) {
        List<String[]> result = new ArrayList<>();

        try {
            String[] lines = scriptOutput.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    // 跳过空行和注释行
                    continue;
                }

                // 按管道符分割：requestId|timestamp|latency|originalLine
                String[] fields = line.split("\\|", -1);
                if (fields.length >= 3) {
                    // 确保至少有4个字段，不足的用空字符串填充
                    String[] normalizedFields = new String[4];
                    normalizedFields[0] = fields.length > 0 ? fields[0].trim() : "";
                    normalizedFields[1] = fields.length > 1 ? fields[1].trim() : "";
                    normalizedFields[2] = fields.length > 2 ? fields[2].trim() : "";
                    normalizedFields[3] = fields.length > 3 ? fields[3].trim() : line; // 原始行
                    result.add(normalizedFields);
                }
            }

            log.debug("自定义脚本输出解析完成，记录数: {}", result.size());

        } catch (Exception e) {
            log.error("解析自定义脚本输出失败", e);
        }

        return result;
    }

    /**
     * 使用Java正则表达式解析日志（Windows系统）
     */
    private List<String[]> parseLogWithJavaRegex(String logContent, ChainNodeConfigDO nodeConfig) {
        List<String[]> result = new ArrayList<>();

        try {
            // 编译正则表达式
            Pattern logPattern = null;
            if (nodeConfig.getLogPattern() != null && !nodeConfig.getLogPattern().trim().isEmpty()) {
                logPattern = Pattern.compile(nodeConfig.getLogPattern());
            }

            Pattern timestampPattern = null;
            if (nodeConfig.getTimestampPattern() != null && !nodeConfig.getTimestampPattern().trim().isEmpty()) {
                timestampPattern = Pattern.compile(nodeConfig.getTimestampPattern());
            }

            Pattern requestIdPattern = null;
            if (nodeConfig.getRequestIdPattern() != null && !nodeConfig.getRequestIdPattern().trim().isEmpty()) {
                requestIdPattern = Pattern.compile(nodeConfig.getRequestIdPattern());
            }

            Pattern latencyPattern = null;
            if (nodeConfig.getLatencyPattern() != null && !nodeConfig.getLatencyPattern().trim().isEmpty()) {
                latencyPattern = Pattern.compile(nodeConfig.getLatencyPattern());
            }

            // 逐行解析
            String[] lines = logContent.split("\\r?\\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 检查日志匹配模式
                if (logPattern != null) {
                    Matcher logMatcher = logPattern.matcher(line);
                    if (!logMatcher.find()) {
                        continue;
                    }
                }

                // 提取时间戳
                String timestamp = "";
                if (timestampPattern != null) {
                    Matcher tsMatcher = timestampPattern.matcher(line);
                    if (tsMatcher.find()) {
                        timestamp = tsMatcher.group(1);
                    }
                }

                // 提取请求ID
                String requestId = "";
                if (requestIdPattern != null) {
                    Matcher ridMatcher = requestIdPattern.matcher(line);
                    if (ridMatcher.find()) {
                        requestId = ridMatcher.group(1);
                    }
                }

                // 提取延时
                String latency = "";
                if (latencyPattern != null) {
                    Matcher latMatcher = latencyPattern.matcher(line);
                    if (latMatcher.find()) {
                        latency = latMatcher.group(1);
                    }
                }

                // 如果至少提取到一个字段，则添加到结果
                if (!timestamp.isEmpty() || !requestId.isEmpty() || !latency.isEmpty()) {
                    result.add(new String[]{requestId, timestamp, latency, line});
                }
            }

            log.debug("Java正则表达式解析完成，节点: {}, 解析记录数: {}", nodeConfig.getNodeName(), result.size());

        } catch (Exception e) {
            log.error("Java正则表达式解析失败，节点: {}", nodeConfig.getNodeName(), e);
        }

        return result;
    }

    /**
     * 使用AWK脚本解析日志（Linux系统）
     */
    private List<String[]> parseLogWithAwk(String logContent, ChainNodeConfigDO nodeConfig) {
        try {
            // 生成AWK脚本
            String awkScript = awkScriptExecutor.generateAwkScript(nodeConfig);
            log.debug("生成的AWK脚本: {}", awkScript);

            // 执行AWK脚本解析日志
            String awkOutput = awkScriptExecutor.executeAwkScript(awkScript, logContent);

            // 解析AWK输出结果
            List<String[]> parsedData = awkScriptExecutor.parseAwkResult(awkOutput, "\\|");

            log.debug("AWK脚本解析完成，节点: {}, 解析记录数: {}", nodeConfig.getNodeName(), parsedData.size());

            return parsedData;

        } catch (Exception e) {
            log.error("AWK脚本解析失败，节点: {}", nodeConfig.getNodeName(), e);
            return new ArrayList<>();
        }
    }
}

