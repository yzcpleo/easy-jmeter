package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.chain.service.ChainDataCollectionService;
import io.github.guojiaxing1995.easyJmeter.dto.chain.QueryChainLatencyDataDTO;
import io.github.guojiaxing1995.easyJmeter.model.ChainLatencyDataDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainLatencyDataMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainTraceConfigMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.vo.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TreeMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.core.annotation.GroupRequired;
import io.github.talelin.core.annotation.LoginRequired;
import io.github.talelin.core.annotation.PermissionMeta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import io.github.guojiaxing1995.easyJmeter.common.util.TimeZoneUtil;
import java.util.Map;

@RestController
@RequestMapping("/v1/chain/collection")
@Api(tags = "链路数据收集")
@Validated
public class ChainDataCollectionController {

    @Autowired
    private ChainDataCollectionService chainDataCollectionService;

    @Autowired
    private ChainLatencyDataMapper chainLatencyDataMapper;

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    @Autowired
    private ChainTraceConfigMapper chainTraceConfigMapper;

    @PostMapping("/start/{taskId}/{chainId}")
    @ApiOperation(value = "启动数据收集", notes = "启动指定链路的数据收集任务")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据收集", module = "链路追踪")
    public Map<String, Object> startDataCollection(
            @PathVariable @Positive(message = "任务ID必须为正数") Long taskId,
            @PathVariable @Positive(message = "链路ID必须为正数") Long chainId) {

        String collectionTaskId = chainDataCollectionService.startDataCollection(taskId, chainId);

        Map<String, Object> result = new HashMap<>();
        result.put("collectionTaskId", collectionTaskId);
        result.put("taskId", taskId);
        result.put("chainId", chainId);
        result.put("message", "数据收集任务已启动");

        return result;
    }

    @PostMapping("/stop/{taskId}/{chainId}")
    @ApiOperation(value = "停止数据收集", notes = "停止指定链路的数据收集任务")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据收集", module = "链路追踪")
    public UpdatedVO stopDataCollection(
            @PathVariable @Positive(message = "任务ID必须为正数") Long taskId,
            @PathVariable @Positive(message = "链路ID必须为正数") Long chainId) {

        boolean stopped = chainDataCollectionService.stopDataCollection(taskId, chainId);
        if (stopped) {
            return new UpdatedVO(31); // 数据收集停止成功码
        } else {
            throw new RuntimeException("数据收集任务不存在或已停止");
        }
    }

    @GetMapping("/status/{taskId}/{chainId}")
    @ApiOperation(value = "获取数据收集状态", notes = "获取指定链路的数据收集状态")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public ChainDataCollectionService.DataCollectionStatus getCollectionStatus(
            @PathVariable @Positive(message = "任务ID必须为正数") Long taskId,
            @PathVariable @Positive(message = "链路ID必须为正数") Long chainId) {

        ChainDataCollectionService.DataCollectionStatus status =
                chainDataCollectionService.getDataCollectionStatus(taskId, chainId);

        if (status == null) {
            throw new NotFoundException(20004, "无数据：数据收集任务不存在，taskId=" + taskId + ", chainId=" + chainId);
        }

        return status;
    }

    @PostMapping("/correlate")
    @ApiOperation(value = "关联请求数据", notes = "根据请求ID获取完整的链路延时数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public List<ChainLatencyDataVO> correlateRequestData(
            @RequestParam @ApiParam(value = "请求ID") String requestId) {

        List<ChainLatencyDataDO> dataList = chainDataCollectionService.correlateRequestData(requestId);

        return dataList.stream()
                .map(data -> ChainLatencyDataVO.builder()
                        .id(data.getId())
                        .taskId(data.getTaskId())
                        .chainId(data.getChainId())
                        .nodeId(data.getNodeId())
                        .requestId(data.getRequestId())
                        .nodeName(data.getNodeName())
                        .requestStartTime(data.getRequestStartTime())
                        .requestEndTime(data.getRequestEndTime())
                        .latency(data.getLatency())
                        .success(data.getSuccess())
                        .errorMessage(data.getErrorMessage())
                        .collectionTime(data.getCollectionTime())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/latency/{requestId}")
    @ApiOperation(value = "计算链路总延时", notes = "根据请求ID计算整个链路的总延时")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public Map<String, Object> calculateChainLatency(
            @PathVariable @ApiParam(value = "请求ID") String requestId) {

        Long chainLatency = chainDataCollectionService.calculateChainLatency(requestId);
        List<ChainLatencyDataDO> requestData = chainDataCollectionService.correlateRequestData(requestId);

        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("chainLatency", chainLatency);
        result.put("nodeCount", requestData.size());
        result.put("nodes", requestData.stream()
                .map(data -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("nodeId", data.getNodeId());
                    nodeMap.put("nodeName", data.getNodeName());
                    nodeMap.put("latency", data.getLatency());
                    nodeMap.put("startTime", data.getRequestStartTime());
                    nodeMap.put("endTime", data.getRequestEndTime());
                    return nodeMap;
                })
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    @GetMapping("/node-stats/{nodeId}")
    @ApiOperation(value = "获取节点性能统计", notes = "获取指定节点在指定时间范围内的性能统计")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public ChainDataCollectionService.NodePerformanceStats getNodePerformanceStats(
            @PathVariable @Positive(message = "节点ID必须为正数") Long nodeId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime) {

        // 设置默认时间范围（最近1小时，使用东八区时间）
        LocalDateTime start = parseDateTime(startTime, TimeZoneUtil.now().minusHours(1));
        LocalDateTime end = parseDateTime(endTime, TimeZoneUtil.now());

        return chainDataCollectionService.getNodePerformanceStats(nodeId, start, end);
    }

    @GetMapping("/chain-stats/{chainId}")
    @ApiOperation(value = "获取链路性能统计", notes = "获取指定链路在指定时间范围内的性能统计")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public Map<String, Object> getChainPerformanceStats(
            @PathVariable @Positive(message = "链路ID必须为正数") Long chainId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime) {

        // 设置默认时间范围（最近1小时，使用东八区时间）
        LocalDateTime start = parseDateTime(startTime, TimeZoneUtil.now().minusHours(1));
        LocalDateTime end = parseDateTime(endTime, TimeZoneUtil.now());

        ChainDataCollectionService.ChainPerformanceStats stats = 
                chainDataCollectionService.getChainPerformanceStats(chainId, start, end);
        
        // 获取链路名称
        ChainTraceConfigDO chainConfig = chainTraceConfigMapper.selectById(chainId);
        String chainName = chainConfig != null ? chainConfig.getChainName() : "未知链路";
        
        // 转换为前端兼容格式
        Map<String, Object> result = new HashMap<>();
        result.put("chainId", stats.getChainId());
        result.put("chainName", chainName);
        result.put("totalRequests", stats.getTotalRequests());
        result.put("totalCount", stats.getTotalRequests()); // 前端兼容字段
        result.put("successfulRequests", stats.getSuccessfulRequests());
        result.put("failedRequests", stats.getFailedRequests());
        result.put("errorCount", stats.getFailedRequests()); // 前端兼容字段
        result.put("avgChainLatency", stats.getAvgChainLatency());
        result.put("avgLatency", stats.getAvgChainLatency()); // 前端兼容字段
        result.put("maxChainLatency", stats.getMaxChainLatency());
        result.put("minChainLatency", stats.getMinChainLatency());
        result.put("p95ChainLatency", stats.getP95ChainLatency());
        result.put("p95Latency", stats.getP95ChainLatency()); // 前端兼容字段
        result.put("p99ChainLatency", stats.getP99ChainLatency());
        result.put("successRate", stats.getSuccessRate());
        result.put("nodeStats", stats.getNodeStats());
        result.put("bottleneckNodes", stats.getBottleneckNodes());
        
        return result;
    }

    /**
     * 解析时间字符串，支持多种格式（解析为东八区时间）
     * @param timeStr 时间字符串，可以是 ISO 8601 格式或 yyyy-MM-dd HH:mm:ss 格式
     * @param defaultValue 解析失败时的默认值（应该是东八区时间）
     * @return LocalDateTime（东八区时间）
     */
    private LocalDateTime parseDateTime(String timeStr, LocalDateTime defaultValue) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            // 使用工具类解析（自动处理为东八区时间）
            LocalDateTime parsed = TimeZoneUtil.parse(timeStr);
            return parsed != null ? parsed : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @GetMapping("/data")
    @ApiOperation(value = "查询延时数据", notes = "根据条件查询链路延时数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public PageResponseVO<ChainLatencyDataVO> queryLatencyData(QueryChainLatencyDataDTO dto) {
        // 设置默认查询范围（最近1小时，使用东八区时间）
        if (dto.getStartCollectionTime() == null) {
            dto.setStartCollectionTime(TimeZoneUtil.format(TimeZoneUtil.now().minusHours(1)));
        }
        if (dto.getEndCollectionTime() == null) {
            dto.setEndCollectionTime(TimeZoneUtil.format(TimeZoneUtil.now()));
        }

        LocalDateTime startTime = LocalDateTime.parse(dto.getStartCollectionTime(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse(dto.getEndCollectionTime(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 查询数据
        List<ChainLatencyDataDO> dataList;
        if (dto.getChainId() != null && dto.getChainId() > 0) {
            dataList = chainLatencyDataMapper.selectByChainIdAndTimeRange(dto.getChainId(), startTime, endTime);
        } else if (dto.getNodeId() != null && dto.getNodeId() > 0) {
            dataList = chainLatencyDataMapper.selectByNodeIdAndTimeRange(dto.getNodeId(), startTime, endTime);
        } else if (dto.getTaskId() != null && dto.getTaskId() > 0) {
            dataList = chainLatencyDataMapper.selectByTaskIdAndTimeRange(dto.getTaskId(), startTime, endTime);
        } else {
            dataList = java.util.Collections.emptyList();
        }

        // 过滤条件
        List<ChainLatencyDataDO> filteredList = dataList.stream()
                .filter(data -> {
                    if (dto.getRequestId() != null && !dto.getRequestId().isEmpty()
                            && !data.getRequestId().contains(dto.getRequestId())) {
                        return false;
                    }
                    if (dto.getNodeName() != null && !dto.getNodeName().isEmpty()
                            && !data.getNodeName().contains(dto.getNodeName())) {
                        return false;
                    }
                    if (dto.getSuccess() != null && !dto.getSuccess().equals(data.getSuccess())) {
                        return false;
                    }
                    if (dto.getMinLatency() != null && data.getLatency() < dto.getMinLatency()) {
                        return false;
                    }
                    if (dto.getMaxLatency() != null && data.getLatency() > dto.getMaxLatency()) {
                        return false;
                    }
                    if (dto.getIncludeErrors() != null && !dto.getIncludeErrors() && data.getSuccess() == 0) {
                        return false;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // 分页处理
        int total = filteredList.size();
        int start = dto.getPage() * dto.getCount();
        int end = Math.min(start + dto.getCount(), total);
        List<ChainLatencyDataDO> pagedList = (start >= 0 && start < total) ? filteredList.subList(start, end) : java.util.Collections.emptyList();

        // 转换为VO
        List<ChainLatencyDataVO> voList = pagedList.stream()
                .map(data -> ChainLatencyDataVO.builder()
                        .id(data.getId())
                        .taskId(data.getTaskId())
                        .chainId(data.getChainId())
                        .nodeId(data.getNodeId())
                        .requestId(data.getRequestId())
                        .nodeName(data.getNodeName())
                        .requestStartTime(data.getRequestStartTime())
                        .requestEndTime(data.getRequestEndTime())
                        .latency(data.getLatency())
                        .success(data.getSuccess())
                        .errorMessage(data.getErrorMessage())
                        .collectionTime(data.getCollectionTime())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return PageResponseVO.<ChainLatencyDataVO>builder()
                .total(total)
                .items(voList)
                .page(dto.getPage())
                .count(dto.getCount())
                .build();
    }

    @DeleteMapping("/cleanup")
    @ApiOperation(value = "清理过期数据", notes = "删除指定时间之前的延时数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据管理", module = "链路追踪")
    public Map<String, Object> cleanupExpiredData(
            @RequestParam @ApiParam(value = "时间点") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beforeTime) {

        int deletedCount = chainDataCollectionService.cleanupExpiredData(beforeTime);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("beforeTime", beforeTime);
        result.put("message", String.format("成功清理 %d 条过期数据", deletedCount));

        return result;
    }

    @GetMapping("/latency-trend")
    @ApiOperation(value = "获取延时趋势数据", notes = "获取指定链路在指定时间范围内的延时趋势数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public List<Map<String, Object>> getLatencyTrend(
            @RequestParam @Positive(message = "链路ID必须为正数") @ApiParam(value = "链路ID") Long chainId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime,
            @RequestParam(required = false, defaultValue = "5minute") @ApiParam(value = "时间粒度：minute, 5minute, hour") String granularity) {

        // 设置默认时间范围（最近24小时）
        LocalDateTime start = parseDateTime(startTime, LocalDateTime.now().minusDays(1));
        LocalDateTime end = parseDateTime(endTime, LocalDateTime.now());

        // 根据粒度计算时间间隔
        long intervalMinutes = getIntervalMinutes(granularity);
        
        List<ChainLatencyDataDO> dataList = chainLatencyDataMapper.selectByChainIdAndTimeRange(chainId, start, end);
        
        // 按时间粒度分组统计
        return calculateLatencyTrend(dataList, intervalMinutes);
    }

    @GetMapping("/throughput")
    @ApiOperation(value = "获取吞吐量数据", notes = "获取指定链路在指定时间范围内的吞吐量数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public List<Map<String, Object>> getThroughput(
            @RequestParam @Positive(message = "链路ID必须为正数") @ApiParam(value = "链路ID") Long chainId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime,
            @RequestParam(required = false, defaultValue = "5minute") @ApiParam(value = "时间粒度：minute, 5minute, hour") String granularity) {

        // 设置默认时间范围（最近24小时）
        LocalDateTime start = parseDateTime(startTime, LocalDateTime.now().minusDays(1));
        LocalDateTime end = parseDateTime(endTime, LocalDateTime.now());

        // 根据粒度计算时间间隔
        long intervalMinutes = getIntervalMinutes(granularity);
        
        List<ChainLatencyDataDO> dataList = chainLatencyDataMapper.selectByChainIdAndTimeRange(chainId, start, end);
        
        // 按时间粒度分组统计吞吐量
        return calculateThroughput(dataList, intervalMinutes);
    }

    @GetMapping("/node-performance")
    @ApiOperation(value = "获取节点性能对比数据", notes = "获取指定链路下所有节点的性能对比数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public List<Map<String, Object>> getNodePerformance(
            @RequestParam @Positive(message = "链路ID必须为正数") @ApiParam(value = "链路ID") Long chainId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime) {

        // 设置默认时间范围（最近24小时）
        LocalDateTime start = parseDateTime(startTime, LocalDateTime.now().minusDays(1));
        LocalDateTime end = parseDateTime(endTime, LocalDateTime.now());

        // 获取链路下所有节点
        List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainId(chainId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChainNodeConfigDO node : nodes) {
            ChainDataCollectionService.NodePerformanceStats nodeStats = 
                    chainDataCollectionService.getNodePerformanceStats(node.getId(), start, end);
            
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("nodeId", nodeStats.getNodeId());
            nodeMap.put("nodeName", nodeStats.getNodeName());
            nodeMap.put("avgLatency", nodeStats.getAvgLatency() != null ? nodeStats.getAvgLatency() : 0.0);
            nodeMap.put("p95Latency", nodeStats.getP95Latency() != null ? nodeStats.getP95Latency() : 0.0);
            nodeMap.put("p99Latency", nodeStats.getP99Latency() != null ? nodeStats.getP99Latency() : 0.0);
            nodeMap.put("maxLatency", nodeStats.getMaxLatency() != null ? nodeStats.getMaxLatency() : 0L);
            nodeMap.put("minLatency", nodeStats.getMinLatency() != null ? nodeStats.getMinLatency() : 0L);
            
            // 计算错误率
            double errorRate = 0.0;
            if (nodeStats.getTotalRequests() != null && nodeStats.getTotalRequests() > 0) {
                errorRate = (double) nodeStats.getFailedRequests() / nodeStats.getTotalRequests() * 100;
            }
            nodeMap.put("errorRate", errorRate);
            
            result.add(nodeMap);
        }
        
        return result;
    }

    @GetMapping("/latency-distribution")
    @ApiOperation(value = "获取延时分布数据", notes = "获取指定链路在指定时间范围内的延时分布数据")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路数据查看", module = "链路追踪")
    public List<Map<String, Object>> getLatencyDistribution(
            @RequestParam @Positive(message = "链路ID必须为正数") @ApiParam(value = "链路ID") Long chainId,
            @RequestParam(required = false) @ApiParam(value = "开始时间") String startTime,
            @RequestParam(required = false) @ApiParam(value = "结束时间") String endTime) {

        // 设置默认时间范围（最近24小时）
        LocalDateTime start = parseDateTime(startTime, LocalDateTime.now().minusDays(1));
        LocalDateTime end = parseDateTime(endTime, LocalDateTime.now());

        List<ChainLatencyDataDO> dataList = chainLatencyDataMapper.selectByChainIdAndTimeRange(chainId, start, end);
        
        // 计算延时分布
        return calculateLatencyDistribution(dataList);
    }

    /**
     * 根据粒度获取时间间隔（分钟）
     */
    private long getIntervalMinutes(String granularity) {
        if (granularity == null) {
            return 5;
        }
        switch (granularity.toLowerCase()) {
            case "minute":
                return 1;
            case "5minute":
                return 5;
            case "hour":
                return 60;
            default:
                return 5;
        }
    }

    /**
     * 计算延时趋势数据
     */
    private List<Map<String, Object>> calculateLatencyTrend(List<ChainLatencyDataDO> dataList, long intervalMinutes) {
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }

        // 按时间间隔分组
        Map<String, List<ChainLatencyDataDO>> groupedData = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ChainLatencyDataDO data : dataList) {
            LocalDateTime collectionTime = data.getCollectionTime();
            // 向下取整到时间间隔
            long minutes = collectionTime.getMinute();
            long roundedMinutes = (minutes / intervalMinutes) * intervalMinutes;
            LocalDateTime roundedTime = collectionTime.withMinute((int) roundedMinutes).withSecond(0).withNano(0);
            String timeKey = roundedTime.format(formatter);
            
            groupedData.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(data);
        }

        // 计算每个时间段的统计值
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ChainLatencyDataDO>> entry : groupedData.entrySet()) {
            List<ChainLatencyDataDO> groupData = entry.getValue();
            List<Long> latencies = groupData.stream()
                    .map(ChainLatencyDataDO::getLatency)
                    .filter(l -> l != null)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            if (latencies.isEmpty()) {
                continue;
            }

            Map<String, Object> point = new HashMap<>();
            point.put("time", entry.getKey());
            point.put("count", groupData.size());
            
            // 平均延时
            double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            point.put("avgLatency", Math.round(avgLatency * 100.0) / 100.0);
            
            // P95延时
            int p95Index = (int) Math.ceil(0.95 * latencies.size()) - 1;
            point.put("p95Latency", latencies.get(Math.max(0, p95Index)));
            
            // P99延时
            int p99Index = (int) Math.ceil(0.99 * latencies.size()) - 1;
            point.put("p99Latency", latencies.get(Math.max(0, p99Index)));
            
            result.add(point);
        }

        return result;
    }

    /**
     * 计算吞吐量数据
     */
    private List<Map<String, Object>> calculateThroughput(List<ChainLatencyDataDO> dataList, long intervalMinutes) {
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }

        // 按时间间隔分组
        Map<String, List<ChainLatencyDataDO>> groupedData = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ChainLatencyDataDO data : dataList) {
            LocalDateTime collectionTime = data.getCollectionTime();
            long minutes = collectionTime.getMinute();
            long roundedMinutes = (minutes / intervalMinutes) * intervalMinutes;
            LocalDateTime roundedTime = collectionTime.withMinute((int) roundedMinutes).withSecond(0).withNano(0);
            String timeKey = roundedTime.format(formatter);
            
            groupedData.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(data);
        }

        // 计算每个时间段的吞吐量
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ChainLatencyDataDO>> entry : groupedData.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", entry.getKey());
            point.put("count", entry.getValue().size());
            
            // QPS = 请求数 / 时间间隔（秒）
            double qps = (double) entry.getValue().size() / (intervalMinutes * 60);
            point.put("qps", Math.round(qps * 100.0) / 100.0);
            
            result.add(point);
        }

        return result;
    }

    /**
     * 计算延时分布数据
     */
    private List<Map<String, Object>> calculateLatencyDistribution(List<ChainLatencyDataDO> dataList) {
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }

        // 定义延时区间
        int[] ranges = {0, 50, 100, 200, 500, Integer.MAX_VALUE};
        String[] rangeNames = {"0-50ms", "50-100ms", "100-200ms", "200-500ms", ">500ms"};
        int[] counts = new int[ranges.length - 1];

        // 统计每个区间的数量
        for (ChainLatencyDataDO data : dataList) {
            Long latency = data.getLatency();
            if (latency == null) {
                continue;
            }
            
            for (int i = 0; i < ranges.length - 1; i++) {
                if (latency >= ranges[i] && latency < ranges[i + 1]) {
                    counts[i]++;
                    break;
                }
            }
        }

        // 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < rangeNames.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", rangeNames[i]);
            item.put("value", counts[i]);
            result.add(item);
        }

        return result;
    }

    /**
     * 链路延时数据VO类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChainLatencyDataVO {
        private Long id;
        
        @JsonProperty("taskId")
        private Long taskId;
        
        @JsonProperty("chainId")
        private Long chainId;
        
        @JsonProperty("nodeId")
        private Long nodeId;
        
        @JsonProperty("requestId")
        private String requestId;
        
        @JsonProperty("nodeName")
        private String nodeName;
        
        @JsonProperty("requestStartTime")
        private LocalDateTime requestStartTime;
        
        @JsonProperty("requestEndTime")
        private LocalDateTime requestEndTime;
        
        private Long latency;
        
        private Integer success;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
        
        @JsonProperty("collectionTime")
        private LocalDateTime collectionTime;
    }
}