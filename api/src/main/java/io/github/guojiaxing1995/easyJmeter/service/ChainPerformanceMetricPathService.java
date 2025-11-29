package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdatePerformanceMetricPathDTO;
import io.github.guojiaxing1995.easyJmeter.vo.PathLatencyStatsVO;
import io.github.guojiaxing1995.easyJmeter.vo.PerformanceMetricPathVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能指标路径配置服务接口
 *
 * @author Assistant
 * @version 1.0.0
 */
public interface ChainPerformanceMetricPathService {

    /**
     * 创建性能指标路径配置
     *
     * @param dto 路径配置DTO
     * @return 创建的路径配置
     */
    PerformanceMetricPathVO createPath(CreateOrUpdatePerformanceMetricPathDTO dto);

    /**
     * 更新性能指标路径配置
     *
     * @param id  路径ID
     * @param dto 路径配置DTO
     * @return 更新后的路径配置
     */
    PerformanceMetricPathVO updatePath(Long id, CreateOrUpdatePerformanceMetricPathDTO dto);

    /**
     * 删除性能指标路径配置
     *
     * @param id 路径ID
     */
    void deletePath(Long id);

    /**
     * 根据ID获取路径配置
     *
     * @param id 路径ID
     * @return 路径配置
     */
    PerformanceMetricPathVO getPathById(Long id);

    /**
     * 根据链路ID获取所有路径配置
     *
     * @param chainId 链路ID
     * @return 路径配置列表
     */
    List<PerformanceMetricPathVO> getPathsByChainId(Long chainId);

    /**
     * 根据链路ID和指标类型获取路径配置
     *
     * @param chainId   链路ID
     * @param metricType 指标类型
     * @return 路径配置列表
     */
    List<PerformanceMetricPathVO> getPathsByChainIdAndMetricType(Long chainId, String metricType);

    /**
     * 获取路径穿透时延统计数据
     *
     * @param pathId    路径ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 时延统计数据
     */
    PathLatencyStatsVO getPathLatencyStats(Long pathId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 批量获取路径穿透时延统计数据
     *
     * @param chainId   链路ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 时延统计数据列表
     */
    List<PathLatencyStatsVO> getPathsLatencyStats(Long chainId, LocalDateTime startTime, LocalDateTime endTime);
}

