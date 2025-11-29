package io.github.guojiaxing1995.easyJmeter.service.impl;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdatePerformanceMetricPathDTO;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.autoconfigure.exception.ParameterException;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainPerformanceMetricPathMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainTraceConfigMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainPerformanceMetricPathDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.service.ChainPerformanceMetricPathService;
import io.github.guojiaxing1995.easyJmeter.vo.PathLatencyStatsVO;
import io.github.guojiaxing1995.easyJmeter.vo.PerformanceMetricPathVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 性能指标路径配置服务实现
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class ChainPerformanceMetricPathServiceImpl implements ChainPerformanceMetricPathService {

    @Autowired
    private ChainPerformanceMetricPathMapper pathMapper;

    @Autowired
    private ChainTraceConfigMapper chainTraceConfigMapper;

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceMetricPathVO createPath(CreateOrUpdatePerformanceMetricPathDTO dto) {
        // 1. 验证链路是否存在
        ChainTraceConfigDO chain = chainTraceConfigMapper.selectById(dto.getChainId());
        if (chain == null || chain.getDeleteTime() != null) {
            throw new NotFoundException("链路配置不存在，链路ID: " + dto.getChainId(), 20001);
        }

        // 2. 验证起点节点是否存在
        ChainNodeConfigDO startNode = chainNodeConfigMapper.selectById(dto.getStartNodeId());
        if (startNode == null || startNode.getDeleteTime() != null) {
            throw new NotFoundException("起点节点不存在，节点ID: " + dto.getStartNodeId(), 20003);
        }
        if (!startNode.getChainId().equals(dto.getChainId())) {
            throw new ParameterException("起点节点不属于该链路", 20004);
        }

        // 3. 验证终点节点是否存在
        ChainNodeConfigDO endNode = chainNodeConfigMapper.selectById(dto.getEndNodeId());
        if (endNode == null || endNode.getDeleteTime() != null) {
            throw new NotFoundException("终点节点不存在，节点ID: " + dto.getEndNodeId(), 20003);
        }
        if (!endNode.getChainId().equals(dto.getChainId())) {
            throw new ParameterException("终点节点不属于该链路", 20004);
        }

        // 4. 验证起点和终点不能相同
        if (dto.getStartNodeId().equals(dto.getEndNodeId())) {
            throw new ParameterException("起点节点和终点节点不能相同", 20005);
        }

        // 5. 创建路径配置
        ChainPerformanceMetricPathDO path = new ChainPerformanceMetricPathDO();
        BeanUtils.copyProperties(dto, path);
        path.setStartNodeName(startNode.getNodeName());
        path.setEndNodeName(endNode.getNodeName());
        // 设置默认时间字段
        if (path.getStartTimeField() == null || path.getStartTimeField().trim().isEmpty()) {
            path.setStartTimeField("REQUEST_START_TIME");
        }
        if (path.getEndTimeField() == null || path.getEndTimeField().trim().isEmpty()) {
            path.setEndTimeField("REQUEST_END_TIME");
        }
        path.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        path.setCreatedTime(LocalDateTime.now());
        path.setUpdatedTime(LocalDateTime.now());

        pathMapper.insert(path);
        log.info("创建性能指标路径成功，路径ID: {}, 指标名称: {}", path.getId(), path.getMetricName());

        return convertToVO(path);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceMetricPathVO updatePath(Long id, CreateOrUpdatePerformanceMetricPathDTO dto) {
        // 1. 验证路径是否存在
        ChainPerformanceMetricPathDO existingPath = pathMapper.selectById(id);
        if (existingPath == null || existingPath.getDeleteTime() != null) {
            throw new NotFoundException("路径配置不存在，路径ID: " + id, 20006);
        }

        // 2. 验证链路是否存在
        ChainTraceConfigDO chain = chainTraceConfigMapper.selectById(dto.getChainId());
        if (chain == null || chain.getDeleteTime() != null) {
            throw new NotFoundException("链路配置不存在，链路ID: " + dto.getChainId(), 20001);
        }

        // 3. 验证起点节点是否存在
        ChainNodeConfigDO startNode = chainNodeConfigMapper.selectById(dto.getStartNodeId());
        if (startNode == null || startNode.getDeleteTime() != null) {
            throw new NotFoundException("起点节点不存在，节点ID: " + dto.getStartNodeId(), 20003);
        }
        if (!startNode.getChainId().equals(dto.getChainId())) {
            throw new ParameterException("起点节点不属于该链路", 20004);
        }

        // 4. 验证终点节点是否存在
        ChainNodeConfigDO endNode = chainNodeConfigMapper.selectById(dto.getEndNodeId());
        if (endNode == null || endNode.getDeleteTime() != null) {
            throw new NotFoundException("终点节点不存在，节点ID: " + dto.getEndNodeId(), 20003);
        }
        if (!endNode.getChainId().equals(dto.getChainId())) {
            throw new ParameterException("终点节点不属于该链路", 20004);
        }

        // 5. 验证起点和终点不能相同
        if (dto.getStartNodeId().equals(dto.getEndNodeId())) {
            throw new ParameterException("起点节点和终点节点不能相同", 20005);
        }

        // 6. 更新路径配置
        BeanUtils.copyProperties(dto, existingPath, "id", "createdTime");
        existingPath.setStartNodeName(startNode.getNodeName());
        existingPath.setEndNodeName(endNode.getNodeName());
        existingPath.setUpdatedTime(LocalDateTime.now());

        pathMapper.updateById(existingPath);
        log.info("更新性能指标路径成功，路径ID: {}, 指标名称: {}", existingPath.getId(), existingPath.getMetricName());

        return convertToVO(existingPath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePath(Long id) {
        ChainPerformanceMetricPathDO path = pathMapper.selectById(id);
        if (path == null || path.getDeleteTime() != null) {
            throw new NotFoundException("路径配置不存在，路径ID: " + id, 20006);
        }

        path.setDeleteTime(LocalDateTime.now());
        pathMapper.updateById(path);
        log.info("删除性能指标路径成功，路径ID: {}", id);
    }

    @Override
    public PerformanceMetricPathVO getPathById(Long id) {
        ChainPerformanceMetricPathDO path = pathMapper.selectById(id);
        if (path == null || path.getDeleteTime() != null) {
            throw new NotFoundException("路径配置不存在，路径ID: " + id, 20006);
        }

        return convertToVO(path);
    }

    @Override
    public List<PerformanceMetricPathVO> getPathsByChainId(Long chainId) {
        List<ChainPerformanceMetricPathDO> paths = pathMapper.selectByChainId(chainId);
        return paths.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<PerformanceMetricPathVO> getPathsByChainIdAndMetricType(Long chainId, String metricType) {
        List<ChainPerformanceMetricPathDO> paths = pathMapper.selectByChainIdAndMetricType(chainId, metricType);
        return paths.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PathLatencyStatsVO getPathLatencyStats(Long pathId, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 验证路径是否存在
        ChainPerformanceMetricPathDO path = pathMapper.selectById(pathId);
        if (path == null || path.getDeleteTime() != null) {
            throw new NotFoundException("路径配置不存在，路径ID: " + pathId, 20006);
        }

        // 2. 查询时延统计数据
        ChainPerformanceMetricPathMapper.PathLatencyStats stats = pathMapper.selectPathLatencyStats(
                pathId, startTime, endTime);

        // 3. 转换为VO
        PathLatencyStatsVO vo = new PathLatencyStatsVO();
        vo.setPathId(pathId);
        vo.setPathName(path.getMetricName());
        vo.setTotalRequests(stats.getTotalRequests() != null ? stats.getTotalRequests() : 0L);
        vo.setSuccessfulRequests(stats.getSuccessfulRequests() != null ? stats.getSuccessfulRequests() : 0L);
        vo.setFailedRequests(stats.getFailedRequests() != null ? stats.getFailedRequests() : 0L);
        vo.setAvgLatency(stats.getAvgLatency() != null ? stats.getAvgLatency() : 0.0);
        vo.setMaxLatency(stats.getMaxLatency() != null ? stats.getMaxLatency() : 0L);
        vo.setMinLatency(stats.getMinLatency() != null ? stats.getMinLatency() : 0L);
        vo.setP95Latency(stats.getP95Latency() != null ? stats.getP95Latency() : 0.0);
        vo.setP99Latency(stats.getP99Latency() != null ? stats.getP99Latency() : 0.0);

        // 4. 计算成功率
        if (vo.getTotalRequests() > 0) {
            vo.setSuccessRate((double) vo.getSuccessfulRequests() / vo.getTotalRequests() * 100);
        } else {
            vo.setSuccessRate(0.0);
        }

        return vo;
    }

    @Override
    public List<PathLatencyStatsVO> getPathsLatencyStats(Long chainId, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 获取链路下所有启用的路径
        List<ChainPerformanceMetricPathDO> paths = pathMapper.selectByChainId(chainId);

        // 2. 批量查询每个路径的时延统计数据
        return paths.stream()
                .map(path -> getPathLatencyStats(path.getId(), startTime, endTime))
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private PerformanceMetricPathVO convertToVO(ChainPerformanceMetricPathDO path) {
        PerformanceMetricPathVO vo = new PerformanceMetricPathVO();
        BeanUtils.copyProperties(path, vo);
        return vo;
    }
}

