package io.github.guojiaxing1995.easyJmeter.chain.service;

import com.jcraft.jsch.*;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 远程日志收集服务
 * 支持通过SSH连接远程Linux节点收集日志
 * 支持Windows节点通过SMB或本地文件系统收集日志
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class RemoteLogCollector {

    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000; // 30秒

    /**
     * 判断节点是否为远程节点
     */
    public boolean isRemoteNode(ChainNodeConfigDO nodeConfig) {
        return nodeConfig.getNodeHost() != null && !nodeConfig.getNodeHost().trim().isEmpty();
    }

    /**
     * 读取日志文件内容（支持本地和远程）
     *
     * @param nodeConfig 节点配置
     * @return 日志文件内容
     */
    public String readLogFile(ChainNodeConfigDO nodeConfig) throws Exception {
        if (!isRemoteNode(nodeConfig)) {
            // 本地节点，直接读取文件
            return readLocalLogFile(nodeConfig.getLogPath());
        }

        // 远程节点
        String osType = nodeConfig.getOsType() != null ? nodeConfig.getOsType().toUpperCase() : "LINUX";
        
        if ("LINUX".equals(osType)) {
            // Linux节点，使用SSH连接
            return readRemoteLogFileViaSSH(nodeConfig);
        } else if ("WINDOWS".equals(osType)) {
            // Windows节点，使用SMB或PowerShell
            return readRemoteLogFileViaWindows(nodeConfig);
        } else {
            throw new IllegalArgumentException("不支持的操作系统类型: " + osType);
        }
    }

    /**
     * 读取本地日志文件
     */
    private String readLocalLogFile(String logPath) throws IOException {
        if (!Files.exists(Paths.get(logPath))) {
            log.warn("日志文件不存在: {}", logPath);
            return "";
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append(System.lineSeparator());
                }
                content.append(line);
            }
        }
        return content.toString();
    }

    /**
     * 通过SSH读取远程Linux节点的日志文件
     */
    private String readRemoteLogFileViaSSH(ChainNodeConfigDO nodeConfig) throws Exception {
        // 如果使用自定义脚本，执行脚本并返回结果
        if (nodeConfig.getUseCustomScript() != null && nodeConfig.getUseCustomScript() == 1 
                && nodeConfig.getCustomShellScript() != null && !nodeConfig.getCustomShellScript().trim().isEmpty()) {
            return executeCustomShellScript(nodeConfig);
        }

        // 否则，使用cat命令读取日志文件
        Session session = null;
        Channel channel = null;

        try {
            // 创建SSH会话
            session = createSshSession(nodeConfig);

            // 执行cat命令读取日志文件
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("cat " + escapeShellPath(nodeConfig.getLogPath()));
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            // 读取输出
            InputStream in = channel.getInputStream();
            channel.connect();

            StringBuilder content = new StringBuilder();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    content.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }

            int exitStatus = ((ChannelExec) channel).getExitStatus();
            if (exitStatus != 0) {
                log.warn("SSH命令执行失败，退出码: {}, 节点: {}", exitStatus, nodeConfig.getNodeName());
            }

            return content.toString();

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * 执行自定义Shell脚本
     * 脚本输出格式：requestId|timestamp|latency|originalLine
     *
     * @param nodeConfig 节点配置
     * @return 脚本输出结果
     */
    private String executeCustomShellScript(ChainNodeConfigDO nodeConfig) throws Exception {
        Session session = null;
        Channel channel = null;

        try {
            // 创建SSH会话
            session = createSshSession(nodeConfig);

            // 将脚本写入临时文件并执行
            String scriptContent = nodeConfig.getCustomShellScript();
            String logPath = nodeConfig.getLogPath();
            
            // 构建执行命令：将脚本内容通过heredoc传递给bash执行
            // 注意：脚本中可以使用 $LOG_PATH 环境变量表示日志文件路径
            // 使用环境变量方式，避免替换脚本中的其他$LOG_PATH引用
            String escapedLogPath = escapeShellPath(logPath);
            String command = String.format(
                "LOG_PATH='%s' bash << 'SCRIPT_EOF'\n" +
                "%s\n" +
                "SCRIPT_EOF",
                escapedLogPath, scriptContent
            );

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            // 读取输出
            InputStream in = channel.getInputStream();
            InputStream errIn = ((ChannelExec) channel).getErrStream();
            channel.connect();

            StringBuilder content = new StringBuilder();
            StringBuilder errorContent = new StringBuilder();
            byte[] tmp = new byte[1024];
            
            while (true) {
                // 读取标准输出
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    content.append(new String(tmp, 0, i));
                }
                
                // 读取错误输出
                while (errIn.available() > 0) {
                    int i = errIn.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errorContent.append(new String(tmp, 0, i));
                }
                
                if (channel.isClosed()) {
                    if (in.available() > 0 || errIn.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }

            int exitStatus = ((ChannelExec) channel).getExitStatus();
            if (exitStatus != 0) {
                log.error("自定义Shell脚本执行失败，退出码: {}, 节点: {}, 错误输出: {}", 
                        exitStatus, nodeConfig.getNodeName(), errorContent.toString());
                throw new RuntimeException("Shell脚本执行失败: " + errorContent.toString());
            }

            if (errorContent.length() > 0) {
                log.warn("Shell脚本警告输出，节点: {}, 警告: {}", nodeConfig.getNodeName(), errorContent.toString());
            }

            log.debug("自定义Shell脚本执行成功，节点: {}, 输出行数: {}", 
                    nodeConfig.getNodeName(), content.toString().split("\n").length);

            return content.toString();

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * 创建SSH会话
     */
    private Session createSshSession(ChainNodeConfigDO nodeConfig) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(
                nodeConfig.getNodeUsername(),
                nodeConfig.getNodeHost(),
                nodeConfig.getNodePort() != null ? nodeConfig.getNodePort() : 22
        );

        // 设置密码
        if (nodeConfig.getNodePassword() != null && !nodeConfig.getNodePassword().trim().isEmpty()) {
            session.setPassword(nodeConfig.getNodePassword());
        }

        // 设置SSH密钥（如果配置了）
        if (nodeConfig.getSshKeyPath() != null && !nodeConfig.getSshKeyPath().trim().isEmpty()) {
            jsch.addIdentity(nodeConfig.getSshKeyPath());
        }

        // 配置SSH连接
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,password");
        session.setConfig(config);

        // 设置超时
        int timeout = nodeConfig.getConnectionTimeout() != null 
                ? nodeConfig.getConnectionTimeout() * 1000 
                : DEFAULT_CONNECTION_TIMEOUT;
        session.setTimeout(timeout);
        session.connect();

        return session;
    }

    /**
     * 通过Windows方式读取远程日志文件
     * Windows节点可以使用SMB共享或PowerShell远程执行
     */
    private String readRemoteLogFileViaWindows(ChainNodeConfigDO nodeConfig) throws Exception {
        // 方案1: 如果日志文件在SMB共享路径上，直接读取
        String logPath = nodeConfig.getLogPath();
        if (logPath.startsWith("\\\\") || logPath.startsWith("//")) {
            // SMB共享路径，尝试直接读取
            try {
                return readLocalLogFile(logPath);
            } catch (Exception e) {
                log.warn("无法通过SMB读取日志文件: {}", logPath, e);
            }
        }

        // 方案2: 使用PowerShell远程执行（需要配置WinRM）
        // 这里先返回空，后续可以实现PowerShell远程执行
        log.warn("Windows远程节点暂不支持PowerShell远程执行，请使用SMB共享路径");
        throw new UnsupportedOperationException("Windows远程节点请使用SMB共享路径或本地文件路径");
    }

    /**
     * 转义Shell路径中的特殊字符
     */
    private String escapeShellPath(String path) {
        return path.replace(" ", "\\ ")
                  .replace("(", "\\(")
                  .replace(")", "\\)")
                  .replace("[", "\\[")
                  .replace("]", "\\]")
                  .replace("&", "\\&")
                  .replace(";", "\\;")
                  .replace("|", "\\|")
                  .replace("<", "\\<")
                  .replace(">", "\\>")
                  .replace("*", "\\*")
                  .replace("?", "\\?")
                  .replace("$", "\\$")
                  .replace("`", "\\`")
                  .replace("\"", "\\\"")
                  .replace("'", "\\'");
    }

    /**
     * 测试SSH连接
     *
     * @param nodeConfig 节点配置
     * @return 是否连接成功
     */
    public boolean testSshConnection(ChainNodeConfigDO nodeConfig) {
        if (!isRemoteNode(nodeConfig) || !"LINUX".equalsIgnoreCase(nodeConfig.getOsType())) {
            return false;
        }

        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(
                    nodeConfig.getNodeUsername(),
                    nodeConfig.getNodeHost(),
                    nodeConfig.getNodePort() != null ? nodeConfig.getNodePort() : 22
            );

            if (nodeConfig.getNodePassword() != null && !nodeConfig.getNodePassword().trim().isEmpty()) {
                session.setPassword(nodeConfig.getNodePassword());
            }

            if (nodeConfig.getSshKeyPath() != null && !nodeConfig.getSshKeyPath().trim().isEmpty()) {
                jsch.addIdentity(nodeConfig.getSshKeyPath());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            int timeout = nodeConfig.getConnectionTimeout() != null 
                    ? nodeConfig.getConnectionTimeout() * 1000 
                    : DEFAULT_CONNECTION_TIMEOUT;
            session.setTimeout(timeout);
            session.connect();

            return true;
        } catch (Exception e) {
            log.error("SSH连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}

