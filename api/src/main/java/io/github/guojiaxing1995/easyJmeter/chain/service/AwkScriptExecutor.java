package io.github.guojiaxing1995.easyJmeter.chain.service;

import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AWK脚本执行器
 * 用于执行AWK脚本进行日志解析和数据处理
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class AwkScriptExecutor {

    private static final String AWK_EXECUTABLE = "awk";
    private static final String GAWK_EXECUTABLE = "gawk";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_SCRIPT_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_OUTPUT_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 生成AWK脚本
     *
     * @param nodeConfig 节点配置
     * @return AWK脚本内容
     */
    public String generateAwkScript(ChainNodeConfigDO nodeConfig) {
        if (nodeConfig == null) {
            throw new IllegalArgumentException("节点配置不能为空");
        }

        StringBuilder script = new StringBuilder();
        script.append("#!/usr/bin/gawk -f\n");
        script.append("BEGIN {\n");
        script.append("    FS = \" \";\n");
        script.append("    OFS = \"|\";\n");
        script.append("    timestampPattern = \"").append(escapeRegex(nodeConfig.getTimestampPattern())).append("\";\n");
        script.append("    latencyPattern = \"").append(escapeRegex(nodeConfig.getLatencyPattern())).append("\";\n");
        script.append("    requestIdPattern = \"").append(escapeRegex(nodeConfig.getRequestIdPattern())).append("\";\n");
        script.append("    logPattern = \"").append(escapeRegex(nodeConfig.getLogPattern())).append("\";\n");
        script.append("    count = 0;\n");
        script.append("}\n\n");

        // 主处理逻辑
        script.append("{\n");
        script.append("    line = $0;\n");

        // 检查日志匹配模式
        if (nodeConfig.getLogPattern() != null && !nodeConfig.getLogPattern().trim().isEmpty()) {
            script.append("    if (line !~ logPattern) next;\n");
        }

        script.append("    \n");
        script.append("    timestamp = \"\";\n");
        script.append("    latency = \"\";\n");
        script.append("    requestId = \"\";\n");
        script.append("    \n");

        // 提取时间戳
        if (nodeConfig.getTimestampPattern() != null && !nodeConfig.getTimestampPattern().trim().isEmpty()) {
            script.append("    if (match(line, timestampPattern, ts)) {\n");
            script.append("        timestamp = ts[1];\n");
            script.append("    }\n");
        }

        // 提取延时
        if (nodeConfig.getLatencyPattern() != null && !nodeConfig.getLatencyPattern().trim().isEmpty()) {
            script.append("    if (match(line, latencyPattern, lat)) {\n");
            script.append("        latency = lat[1];\n");
            script.append("    }\n");
        }

        // 提取请求ID
        if (nodeConfig.getRequestIdPattern() != null && !nodeConfig.getRequestIdPattern().trim().isEmpty()) {
            script.append("    if (match(line, requestIdPattern, rid)) {\n");
            script.append("        requestId = rid[1];\n");
            script.append("    }\n");
        }

        script.append("    \n");
        script.append("    if ((timestamp != \"\" || latency != \"\" || requestId != \"\") && ");
        script.append("(timestampPattern == \"\" || timestamp != \"\") && ");
        script.append("(latencyPattern == \"\" || latency != \"\") && ");
        script.append("(requestIdPattern == \"\" || requestId != \"\")) {\n");
        script.append("        print requestId \"|\" timestamp \"|\" latency \"|\" line;\n");
        script.append("        count++;\n");
        script.append("    }\n");
        script.append("}\n\n");

        // 结尾处理
        script.append("END {\n");
        script.append("    print \"TOTAL_RECORDS|\" count;\n");
        script.append("}\n");

        return script.toString();
    }

    /**
     * 执行AWK脚本
     *
     * @param script AWK脚本内容
     * @param input 输入数据
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public String executeAwkScript(String script, String input) throws Exception {
        return executeAwkScript(script, input, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行AWK脚本（带超时）
     *
     * @param script AWK脚本内容
     * @param input 输入数据
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public String executeAwkScript(String script, String input, int timeoutSeconds) throws Exception {
        if (script == null || script.trim().isEmpty()) {
            return "";
        }

        if (script.length() > MAX_SCRIPT_SIZE) {
            throw new IllegalArgumentException("脚本内容过大，超过最大限制 " + MAX_SCRIPT_SIZE + " 字节");
        }

        // 检查AWK是否可用
        if (!isAwkAvailable()) {
            throw new IllegalStateException("系统中未找到AWK或GAWK命令");
        }

        File scriptFile = null;
        File inputFile = null;

        try {
            // 创建临时脚本文件
            scriptFile = createTempScriptFile(script);

            // 创建临时输入文件
            if (input != null && !input.trim().isEmpty()) {
                inputFile = createTempInputFile(input);
            }

            // 构建命令
            List<String> command = new ArrayList<>();
            command.add(GAWK_EXECUTABLE);
            command.add("-f");
            command.add(scriptFile.getAbsolutePath());

            if (inputFile != null) {
                command.add(inputFile.getAbsolutePath());
            }

            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("AWK脚本执行超时");
            }

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append(System.lineSeparator());
                    }
                    output.append(line);

                    // 限制输出大小
                    if (output.length() > MAX_OUTPUT_SIZE) {
                        throw new RuntimeException("AWK脚本输出过大，超过最大限制 " + MAX_OUTPUT_SIZE + " 字节");
                    }
                }
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("AWK脚本执行失败，退出码: " + exitCode + ", 输出: " + output);
            }

            return output.toString();

        } finally {
            // 清理临时文件
            cleanupTempFile(scriptFile);
            cleanupTempFile(inputFile);
        }
    }

    /**
     * 创建临时脚本文件
     *
     * @param scriptContent 脚本内容
     * @return 临时文件
     * @throws IOException IO异常
     */
    public File createTempScriptFile(String scriptContent) throws IOException {
        Path tempDir = Files.createTempDirectory("awk_script_");
        File scriptFile = tempDir.resolve("script.awk").toFile();

        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write(scriptContent);
        }

        // 设置执行权限
        scriptFile.setExecutable(true);

        log.debug("创建临时AWK脚本文件: {}", scriptFile.getAbsolutePath());
        return scriptFile;
    }

    /**
     * 创建临时输入文件
     *
     * @param inputContent 输入内容
     * @return 临时文件
     * @throws IOException IO异常
     */
    public File createTempInputFile(String inputContent) throws IOException {
        Path tempDir = Files.createTempDirectory("awk_input_");
        File inputFile = tempDir.resolve("input.txt").toFile();

        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(inputContent);
        }

        log.debug("创建临时输入文件: {}", inputFile.getAbsolutePath());
        return inputFile;
    }

    /**
     * 执行脚本文件
     *
     * @param scriptFile 脚本文件
     * @param inputContent 输入内容
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public String executeScriptFile(File scriptFile, String inputContent) throws Exception {
        if (!scriptFile.exists() || !scriptFile.canRead()) {
            throw new IllegalArgumentException("脚本文件不存在或不可读: " + scriptFile.getAbsolutePath());
        }

        String scriptContent;
        try {
            scriptContent = new String(Files.readAllBytes(scriptFile.toPath()));
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取脚本文件: " + scriptFile.getAbsolutePath(), e);
        }

        return executeAwkScript(scriptContent, inputContent);
    }

    /**
     * 检查AWK是否可用
     *
     * @return 是否可用
     */
    public boolean isAwkAvailable() {
        try {
            Process process = new ProcessBuilder(GAWK_EXECUTABLE, "--version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return true;
            }
        } catch (Exception e) {
            log.debug("GAWK不可用，尝试AWK: {}", e.getMessage());
        }

        try {
            Process process = new ProcessBuilder(AWK_EXECUTABLE, "--version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("AWK也不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证AWK脚本语法
     *
     * @param scriptFile 脚本文件
     * @return 语法是否正确
     */
    public boolean validateAwkScript(File scriptFile) {
        try {
            // 尝试解析脚本而不执行
            String command = GAWK_EXECUTABLE + " -f " + scriptFile.getAbsolutePath() + " '/BEGIN{exit}'";
            Process process = Runtime.getRuntime().exec(command);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("AWK脚本语法验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析AWK结果
     *
     * @param output AWK输出
     * @param delimiter 分隔符
     * @return 解析后的数据列表
     */
    public List<String[]> parseAwkResult(String output, String delimiter) {
        List<String[]> result = new ArrayList<>();

        if (output == null || output.trim().isEmpty()) {
            return result;
        }

        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("TOTAL_RECORDS|")) {
                continue;
            }

            String[] fields = line.split(Pattern.quote(delimiter));
            result.add(fields);
        }

        return result;
    }

    /**
     * 从日志行中提取时间戳
     *
     * @param logLine 日志行
     * @param pattern 时间戳模式
     * @return 时间戳
     */
    public String extractTimestamp(String logLine, String pattern) {
        if (logLine == null || pattern == null || pattern.trim().isEmpty()) {
            return null;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(logLine);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("时间戳提取失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从日志行中提取延时值
     *
     * @param logLine 日志行
     * @param pattern 延时模式
     * @return 延时值
     */
    public String extractLatency(String logLine, String pattern) {
        if (logLine == null || pattern == null || pattern.trim().isEmpty()) {
            return null;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(logLine);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("延时提取失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从日志行中提取请求ID
     *
     * @param logLine 日志行
     * @param pattern 请求ID模式
     * @return 请求ID
     */
    public String extractRequestId(String logLine, String pattern) {
        if (logLine == null || pattern == null || pattern.trim().isEmpty()) {
            return null;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(logLine);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("请求ID提取失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 格式化延时数据
     *
     * @param latency 延时值（毫秒）
     * @return 格式化后的延时字符串
     */
    public String formatLatency(Long latency) {
        if (latency == null) {
            return "0ms";
        }
        return latency + "ms";
    }

    /**
     * 清理临时文件
     */
    public void cleanupTempFiles() {
        // 这个方法可以被调用来清理所有临时文件
        // 实际的清理在各个方法的finally块中进行
        log.debug("清理临时文件");
    }

    /**
     * 获取当前内存使用情况
     *
     * @return 内存使用率（百分比）
     */
    public double getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / totalMemory * 100;
    }

    /**
     * 转义正则表达式特殊字符
     *
     * @param regex 正则表达式
     * @return 转义后的表达式
     */
    private String escapeRegex(String regex) {
        if (regex == null) {
            return "";
        }
        return regex.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 清理临时文件
     *
     * @param file 要清理的文件
     */
    private void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
                log.debug("清理临时文件: {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", file.getAbsolutePath(), e);
            }
        }
    }
}