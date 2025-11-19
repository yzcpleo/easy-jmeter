package io.github.guojiaxing1995.easyJmeter.chain.service.impl;

import io.github.guojiaxing1995.easyJmeter.chain.service.ChainDataCollectionService;
import io.github.guojiaxing1995.easyJmeter.chain.service.AwkScriptExecutor;
import io.github.guojiaxing1995.easyJmeter.chain.service.RemoteLogCollector;
import io.github.guojiaxing1995.easyJmeter.chain.service.LogParserService;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainLatencyDataMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainLatencyDataDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 链路数据收集服务实现类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class ChainDataCollectionServiceImpl implements ChainDataCollectionService, DisposableBean {

    @Autowired
    private AwkScriptExecutor awkScriptExecutor;

    @Autowired
    private RemoteLogCollector remoteLogCollector;

    @Autowired
    private LogParserService logParserService;

    @Autowired
    private ChainLatencyDataMapper chainLatencyDataMapper;

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    // @Autowired
    // private ChainDataWebSocketHandler webSocketHandler;

    // 数据收集任务状态缓存
    private final Map<String, DataCollectionStatus> collectionStatusMap = new ConcurrentHashMap<>();

    // 定时任务Future引用缓存，用于取消定时任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Override
    public String startDataCollection(Long taskId, Long chainId) {
        String collectionTaskId = generateCollectionTaskId(taskId, chainId);

        DataCollectionStatus status = new DataCollectionStatus();
        status.setTaskId(taskId);
        status.setChainId(chainId);
        status.setCollectionTaskId(collectionTaskId);
        status.setStatus(CollectionStatus.COLLECTING);
        status.setStartTime(LocalDateTime.now());
        status.setTotalRecords(0L);
        status.setSuccessRecords(0L);
        status.setErrorRecords(0L);

        collectionStatusMap.put(collectionTaskId, status);

        // 启动定时数据收集任务，并保存Future引用以便后续取消
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                performDataCollection(taskId, chainId, collectionTaskId);
            } catch (Exception e) {
                log.error("数据收集任务执行失败: taskId={}, chainId={}", taskId, chainId, e);
                updateCollectionError(collectionTaskId, e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS); // 每5秒执行一次
        
        scheduledTasks.put(collectionTaskId, future);

        log.info("启动数据收集任务: taskId={}, chainId={}, collectionTaskId={}", taskId, chainId, collectionTaskId);
        return collectionTaskId;
    }

    @Override
    public boolean stopDataCollection(Long taskId, Long chainId) {
        String collectionTaskId = generateCollectionTaskId(taskId, chainId);
        DataCollectionStatus status = collectionStatusMap.get(collectionTaskId);

        if (status != null && status.getStatus() == CollectionStatus.COLLECTING) {
            status.setStatus(CollectionStatus.STOPPED);
            status.setEndTime(LocalDateTime.now());
            
            // 取消定时任务
            ScheduledFuture<?> future = scheduledTasks.remove(collectionTaskId);
            if (future != null) {
                boolean cancelled = future.cancel(false);
                log.info("停止数据收集任务: taskId={}, chainId={}, collectionTaskId={}, 定时任务取消={}", 
                        taskId, chainId, collectionTaskId, cancelled);
            } else {
                log.warn("停止数据收集任务时未找到对应的定时任务: taskId={}, chainId={}, collectionTaskId={}", 
                        taskId, chainId, collectionTaskId);
            }
            
            return true;
        }

        log.warn("停止数据收集任务失败: 任务不存在或已停止, taskId={}, chainId={}, collectionTaskId={}", 
                taskId, chainId, collectionTaskId);
        return false;
    }

    @Override
    public List<ChainLatencyDataDO> collectLogData(Long taskId, ChainNodeConfigDO nodeConfig, String logFilePath) {
        List<ChainLatencyDataDO> latencyDataList = new ArrayList<>();

        try {
            // 读取日志文件内容（支持本地和远程）
            String logContent;
            if (remoteLogCollector.isRemoteNode(nodeConfig)) {
                // 远程节点
                log.info("从远程节点读取日志: nodeId={}, nodeName={}, host={}, osType={}, logPath={}", 
                        nodeConfig.getId(), nodeConfig.getNodeName(), nodeConfig.getNodeHost(), 
                        nodeConfig.getOsType(), logFilePath);
                logContent = remoteLogCollector.readLogFile(nodeConfig);
            } else {
                // 本地节点
                log.info("从本地节点读取日志: nodeId={}, nodeName={}, logPath={}", 
                        nodeConfig.getId(), nodeConfig.getNodeName(), logFilePath);
                if (!Files.exists(Paths.get(logFilePath))) {
                    log.warn("日志文件不存在: nodeId={}, nodeName={}, logPath={}", 
                            nodeConfig.getId(), nodeConfig.getNodeName(), logFilePath);
                    return latencyDataList;
                }
                logContent = readLogFile(logFilePath);
            }

            if (logContent == null || logContent.trim().isEmpty()) {
                log.warn("日志内容为空: nodeId={}, nodeName={}, logPath={}, 可能文件为空或读取失败", 
                        nodeConfig.getId(), nodeConfig.getNodeName(), logFilePath);
                return latencyDataList;
            }

            log.debug("日志内容读取成功: nodeId={}, nodeName={}, 内容长度={}", 
                    nodeConfig.getId(), nodeConfig.getNodeName(), logContent.length());

            // 解析日志内容（根据操作系统类型选择解析方式）
            List<String[]> parsedData = logParserService.parseLogContent(logContent, nodeConfig);
            
            log.debug("日志解析完成: nodeId={}, nodeName={}, 解析行数={}", 
                    nodeConfig.getId(), nodeConfig.getNodeName(), parsedData.size());

            if (parsedData.isEmpty()) {
                log.warn("日志解析结果为空: nodeId={}, nodeName={}, 可能解析规则不匹配或日志格式不正确", 
                        nodeConfig.getId(), nodeConfig.getNodeName());
                return latencyDataList;
            }

            // 转换为数据对象
            int validCount = 0;
            for (String[] fields : parsedData) {
                ChainLatencyDataDO latencyData = parseAwkFields(taskId, nodeConfig, fields);
                if (latencyData != null) {
                    latencyDataList.add(latencyData);
                    validCount++;
                }
            }

            log.info("成功收集延时数据: nodeId={}, nodeName={}, 解析行数={}, 有效记录数={}, 节点类型={}", 
                    nodeConfig.getId(), nodeConfig.getNodeName(), parsedData.size(), validCount,
                    remoteLogCollector.isRemoteNode(nodeConfig) ? "远程" : "本地");

        } catch (Exception e) {
            log.error("收集日志数据失败: nodeId={}, nodeName={}, logFilePath={}, error={}", 
                    nodeConfig.getId(), nodeConfig.getNodeName(), logFilePath, e.getMessage(), e);
            // 不重新抛出异常，返回空列表，让上层继续处理其他节点
        }

        return latencyDataList;
    }

    @Override
    public ChainLatencyDataDO processLogData(Long taskId, ChainNodeConfigDO nodeConfig, String logContent) {
        try {
            // 使用日志解析服务（支持Windows和Linux）
            List<String[]> parsedData = logParserService.parseLogContent(logContent, nodeConfig);

            if (!parsedData.isEmpty()) {
                return parseAwkFields(taskId, nodeConfig, parsedData.get(0));
            }

        } catch (Exception e) {
            log.error("处理日志数据失败: nodeId={}", nodeConfig.getId(), e);
        }

        return null;
    }

    @Override
    public List<ChainLatencyDataDO> correlateRequestData(String requestId) {
        return chainLatencyDataMapper.selectByRequestId(requestId);
    }

    @Override
    public Long calculateChainLatency(String requestId) {
        List<ChainLatencyDataDO> requestData = correlateRequestData(requestId);

        if (requestData.isEmpty()) {
            return 0L;
        }

        // 按时间顺序排序
        requestData.sort(Comparator.comparing(ChainLatencyDataDO::getRequestStartTime));

        // 计算总延时（从第一个节点开始到最后一个节点结束）
        LocalDateTime startTime = requestData.get(0).getRequestStartTime();
        LocalDateTime endTime = requestData.get(requestData.size() - 1).getRequestEndTime();

        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    @Override
    public DataCollectionStatus getDataCollectionStatus(Long taskId, Long chainId) {
        String collectionTaskId = generateCollectionTaskId(taskId, chainId);
        return collectionStatusMap.get(collectionTaskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveLatencyData(List<ChainLatencyDataDO> latencyDataList) {
        if (latencyDataList == null || latencyDataList.isEmpty()) {
            return 0;
        }

        return chainLatencyDataMapper.batchInsert(latencyDataList);
    }

    @Override
    public int cleanupExpiredData(LocalDateTime beforeTime) {
        return chainLatencyDataMapper.deleteByCollectionTimeBefore(beforeTime);
    }

    @Override
    public NodePerformanceStats getNodePerformanceStats(Long nodeId, LocalDateTime startTime, LocalDateTime endTime) {
        ChainLatencyDataMapper.ChainLatencyStatsDO stats = null;
        try {
            stats = chainLatencyDataMapper.getLatencyStats(nodeId, startTime, endTime);
        } catch (Exception e) {
            log.warn("获取节点延时统计失败: nodeId={}", nodeId, e);
        }

        NodePerformanceStats nodeStats = new NodePerformanceStats();
        nodeStats.setNodeId(nodeId);
        
        if (stats == null) {
            // 没有数据时返回默认值
            nodeStats.setNodeName("节点-" + nodeId);
            nodeStats.setTotalRequests(0L);
            nodeStats.setSuccessfulRequests(0L);
            nodeStats.setFailedRequests(0L);
            nodeStats.setAvgLatency(0.0);
            nodeStats.setMaxLatency(0L);
            nodeStats.setMinLatency(0L);
            nodeStats.setP95Latency(0.0);
            nodeStats.setP99Latency(0.0);
            nodeStats.setSuccessRate(0.0);
            return nodeStats;
        }

        nodeStats.setNodeName(stats.getNodeName() != null ? stats.getNodeName() : "节点-" + nodeId);
        nodeStats.setTotalRequests(stats.getTotalRequests() != null ? stats.getTotalRequests() : 0L);
        nodeStats.setSuccessfulRequests(stats.getSuccessfulRequests() != null ? stats.getSuccessfulRequests() : 0L);
        nodeStats.setFailedRequests(stats.getFailedRequests() != null ? stats.getFailedRequests() : 0L);
        nodeStats.setAvgLatency(stats.getAvgLatency() != null ? stats.getAvgLatency() : 0.0);
        nodeStats.setMaxLatency(stats.getMaxLatency() != null ? stats.getMaxLatency() : 0L);
        nodeStats.setMinLatency(stats.getMinLatency() != null ? stats.getMinLatency() : 0L);
        nodeStats.setP95Latency(stats.getP95Latency() != null ? stats.getP95Latency() : 0.0);
        nodeStats.setP99Latency(stats.getP99Latency() != null ? stats.getP99Latency() : 0.0);

        // 计算成功率
        if (nodeStats.getTotalRequests() > 0) {
            nodeStats.setSuccessRate((double) nodeStats.getSuccessfulRequests() / nodeStats.getTotalRequests() * 100);
        } else {
            nodeStats.setSuccessRate(0.0);
        }

        return nodeStats;
    }

    @Override
    public ChainPerformanceStats getChainPerformanceStats(Long chainId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainId(chainId);
        Map<Long, NodePerformanceStats> nodeStatsMap = new HashMap<>();
        ChainPerformanceStats chainStats = new ChainPerformanceStats();

        chainStats.setChainId(chainId);
        chainStats.setTotalRequests(0L);
        chainStats.setSuccessfulRequests(0L);
        chainStats.setFailedRequests(0L);
        chainStats.setNodeStats(nodeStatsMap);
        chainStats.setBottleneckNodes(new ArrayList<>());

        List<Long> latencies = new ArrayList<>();

        for (ChainNodeConfigDO node : nodes) {
            NodePerformanceStats nodeStats = getNodePerformanceStats(node.getId(), startTime, endTime);
            nodeStatsMap.put(node.getId(), nodeStats);

            chainStats.setTotalRequests(chainStats.getTotalRequests() + nodeStats.getTotalRequests());
            chainStats.setSuccessfulRequests(chainStats.getSuccessfulRequests() + nodeStats.getSuccessfulRequests());
            chainStats.setFailedRequests(chainStats.getFailedRequests() + nodeStats.getFailedRequests());

            if (nodeStats.getMaxLatency() != null) {
                latencies.add(nodeStats.getMaxLatency());
            }
        }

        // 计算链路统计
        if (!latencies.isEmpty()) {
            chainStats.setMaxChainLatency(Collections.max(latencies));
            chainStats.setMinChainLatency(Collections.min(latencies));
            chainStats.setAvgChainLatency(latencies.stream().mapToLong(Long::longValue).average().orElse(0.0));

            // 计算分位数
            List<Long> sortedLatencies = latencies.stream().sorted().collect(Collectors.toList());
            int size = sortedLatencies.size();
            if (size > 0) {
                int p95Index = (int) Math.ceil(0.95 * size) - 1;
                int p99Index = (int) Math.ceil(0.99 * size) - 1;
                chainStats.setP95ChainLatency(sortedLatencies.get(Math.max(0, p95Index)).doubleValue());
                chainStats.setP99ChainLatency(sortedLatencies.get(Math.max(0, p99Index)).doubleValue());
            }
        }

        // 计算成功率
        if (chainStats.getTotalRequests() > 0) {
            chainStats.setSuccessRate((double) chainStats.getSuccessfulRequests() / chainStats.getTotalRequests() * 100);
        }

        // 找出瓶颈节点（平均延时最高的前3个）
        List<Map.Entry<Long, NodePerformanceStats>> sortedNodes = nodeStatsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getAvgLatency() != null)
                .sorted(Map.Entry.<Long, NodePerformanceStats>comparingByValue(
                        Comparator.comparing(NodePerformanceStats::getAvgLatency)).reversed())
                .limit(3)
                .collect(Collectors.toList());

        List<String> bottleneckNodes = sortedNodes.stream()
                .map(entry -> entry.getValue().getNodeName())
                .collect(Collectors.toList());

        chainStats.setBottleneckNodes(bottleneckNodes);

        return chainStats;
    }

    /**
     * 执行数据收集
     */
    private void performDataCollection(Long taskId, Long chainId, String collectionTaskId) {
        DataCollectionStatus status = collectionStatusMap.get(collectionTaskId);
        if (status == null || status.getStatus() != CollectionStatus.COLLECTING) {
            return;
        }

        try {
            List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainId(chainId);
            List<ChainLatencyDataDO> allLatencyData = new ArrayList<>();

            log.info("开始数据收集: taskId={}, chainId={}, 节点总数={}", taskId, chainId, nodes.size());

            int skippedCount = 0;
            int successCount = 0;
            int errorCount = 0;

            for (ChainNodeConfigDO node : nodes) {
                // 检查节点状态
                if (node.getStatus() == null || node.getStatus() != 1) {
                    log.warn("跳过节点（状态未启用）: nodeId={}, nodeName={}, status={}", 
                            node.getId(), node.getNodeName(), node.getStatus());
                    skippedCount++;
                    continue;
                }

                // 检查日志路径
                if (node.getLogPath() == null || node.getLogPath().trim().isEmpty()) {
                    log.warn("跳过节点（日志路径为空）: nodeId={}, nodeName={}, logPath={}", 
                            node.getId(), node.getNodeName(), node.getLogPath());
                    skippedCount++;
                    continue;
                }

                try {
                    log.info("开始收集节点数据: nodeId={}, nodeName={}, logPath={}, host={}, osType={}", 
                            node.getId(), node.getNodeName(), node.getLogPath(), 
                            node.getNodeHost(), node.getOsType());
                    
                    // 收集日志数据（支持本地和远程节点）
                    List<ChainLatencyDataDO> nodeData = collectLogData(taskId, node, node.getLogPath());
                    
                    if (nodeData != null && !nodeData.isEmpty()) {
                        allLatencyData.addAll(nodeData);
                        successCount++;
                        log.info("节点数据收集成功: nodeId={}, nodeName={}, 收集记录数={}", 
                                node.getId(), node.getNodeName(), nodeData.size());
                    } else {
                        log.warn("节点数据收集为空: nodeId={}, nodeName={}, 可能日志文件无数据或解析失败", 
                                node.getId(), node.getNodeName());
                        skippedCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("收集节点数据失败: nodeId={}, nodeName={}, host={}, logPath={}, error={}", 
                            node.getId(), node.getNodeName(), node.getNodeHost(), 
                            node.getLogPath(), e.getMessage(), e);
                    status.setErrorRecords(status.getErrorRecords() + 1);
                }
            }

            log.info("数据收集完成: taskId={}, chainId={}, 总节点数={}, 成功={}, 跳过={}, 失败={}, 收集记录数={}", 
                    taskId, chainId, nodes.size(), successCount, skippedCount, errorCount, allLatencyData.size());

            // 批量保存数据
            if (!allLatencyData.isEmpty()) {
                int savedCount = batchSaveLatencyData(allLatencyData);
                status.setTotalRecords(status.getTotalRecords() + allLatencyData.size());
                status.setSuccessRecords(status.getSuccessRecords() + savedCount);
            }
        } catch (org.springframework.jdbc.CannotGetJdbcConnectionException e) {
            // 数据源已关闭，停止收集任务
            log.warn("数据源已关闭，停止数据收集任务: taskId={}, chainId={}, collectionTaskId={}", 
                    taskId, chainId, collectionTaskId);
            status.setStatus(CollectionStatus.STOPPED);
            status.setErrorMessage("数据源已关闭");
            status.setEndTime(LocalDateTime.now());
            // 取消定时任务
            ScheduledFuture<?> future = scheduledTasks.remove(collectionTaskId);
            if (future != null) {
                future.cancel(false);
            }
        } catch (Exception e) {
            // 检查是否是数据库连接关闭的异常
            Throwable cause = e.getCause();
            if (cause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                if (sqlEx.getMessage() != null && sqlEx.getMessage().contains("has been closed")) {
                    log.warn("数据库连接已关闭，停止数据收集任务: taskId={}, chainId={}, collectionTaskId={}", 
                            taskId, chainId, collectionTaskId);
                    status.setStatus(CollectionStatus.STOPPED);
                    status.setErrorMessage("数据库连接已关闭");
                    status.setEndTime(LocalDateTime.now());
                    // 取消定时任务
                    ScheduledFuture<?> future = scheduledTasks.remove(collectionTaskId);
                    if (future != null) {
                        future.cancel(false);
                    }
                    return;
                }
            }
            log.error("数据收集任务执行失败: taskId={}, chainId={}", taskId, chainId, e);
            updateCollectionError(collectionTaskId, e.getMessage());
        }
    }

    /**
     * 生成收集任务ID
     * 注意：不使用时间戳，确保同一taskId和chainId总是生成相同的ID
     * 这样才能在停止时找到对应的任务
     */
    private String generateCollectionTaskId(Long taskId, Long chainId) {
        return String.format("collection_%d_%d", taskId, chainId);
    }

    /**
     * 更新收集错误信息
     */
    private void updateCollectionError(String collectionTaskId, String errorMessage) {
        DataCollectionStatus status = collectionStatusMap.get(collectionTaskId);
        if (status != null) {
            status.setStatus(CollectionStatus.ERROR);
            status.setErrorMessage(errorMessage);
            status.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 读取日志文件内容
     */
    private String readLogFile(String logFilePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
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
     * 解析AWK字段数据
     */
    private ChainLatencyDataDO parseAwkFields(Long taskId, ChainNodeConfigDO nodeConfig, String[] fields) {
        if (fields.length < 4) {
            return null;
        }

        try {
            String requestId = fields[0].trim();
            String timestamp = fields[1].trim();
            String latency = fields[2].trim();

            if (requestId.isEmpty() && latency.isEmpty()) {
                return null;
            }

            // 解析时间戳
            LocalDateTime requestStartTime = parseTimestamp(timestamp);
            if (requestStartTime == null) {
                requestStartTime = LocalDateTime.now();
            }

            // 解析延时
            Long latencyMs = parseLatency(latency);
            LocalDateTime requestEndTime = requestStartTime.plusNanos(latencyMs * 1_000_000);

            ChainLatencyDataDO data = ChainLatencyDataDO.builder()
                    .taskId(taskId)
                    .chainId(nodeConfig.getChainId())
                    .nodeId(nodeConfig.getId())
                    .requestId(requestId.isEmpty() ? generateRequestId() : requestId)
                    .nodeName(nodeConfig.getNodeName())
                    .requestStartTime(requestStartTime)
                    .requestEndTime(requestEndTime)
                    .latency(latencyMs)
                    .success(1)
                    .errorMessage("")
                    .extendedData("")
                    .collectionTime(LocalDateTime.now())
                    .build();

            return data;

        } catch (Exception e) {
            log.error("解析AWK字段数据失败: {}", Arrays.toString(fields), e);
            return null;
        }
    }

    /**
     * 解析时间戳
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试多种时间格式
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(timestamp, formatter);
                } catch (Exception ignored) {
                    // 继续尝试下一个格式
                }
            }

            log.warn("无法解析时间戳: {}", timestamp);
            return null;

        } catch (Exception e) {
            log.error("解析时间戳异常: {}", timestamp, e);
            return null;
        }
    }

    /**
     * 解析延时值
     */
    private Long parseLatency(String latency) {
        if (latency == null || latency.trim().isEmpty()) {
            return 0L;
        }

        try {
            // 移除可能的单位
            latency = latency.replaceAll("[^0-9]", "");
            return Long.parseLong(latency);
        } catch (NumberFormatException e) {
            log.warn("无法解析延时值: {}", latency);
            return 0L;
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 应用关闭时清理资源
     */
    @Override
    public void destroy() throws Exception {
        log.info("开始清理数据收集服务资源...");
        
        // 停止所有正在运行的收集任务
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            String collectionTaskId = entry.getKey();
            ScheduledFuture<?> future = entry.getValue();
            
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
                log.info("取消定时任务: collectionTaskId={}", collectionTaskId);
            }
            
            // 更新状态
            DataCollectionStatus status = collectionStatusMap.get(collectionTaskId);
            if (status != null && status.getStatus() == CollectionStatus.COLLECTING) {
                status.setStatus(CollectionStatus.STOPPED);
                status.setEndTime(LocalDateTime.now());
                status.setErrorMessage("应用关闭");
            }
        }
        
        scheduledTasks.clear();
        
        // 关闭线程池
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // 等待正在执行的任务完成，最多等待5秒
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    log.warn("线程池未能在5秒内正常关闭，强制关闭");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("等待线程池关闭时被中断", e);
            }
            log.info("数据收集服务线程池已关闭");
        }
        
        log.info("数据收集服务资源清理完成");
    }
}