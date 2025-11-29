package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdatePerformanceMetricPathDTO;
import io.github.guojiaxing1995.easyJmeter.service.ChainPerformanceMetricPathService;
import io.github.guojiaxing1995.easyJmeter.vo.CreatedVO;
import io.github.guojiaxing1995.easyJmeter.vo.DeletedVO;
import io.github.guojiaxing1995.easyJmeter.vo.PathLatencyStatsVO;
import io.github.guojiaxing1995.easyJmeter.vo.PerformanceMetricPathVO;
import io.github.guojiaxing1995.easyJmeter.vo.UpdatedVO;
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
import java.util.List;

/**
 * 性能指标路径配置控制器
 *
 * @author Assistant
 * @version 1.0.0
 */
@RestController
@RequestMapping("/v1/chain/metric-path")
@Api(tags = "性能指标路径配置")
@Validated
public class ChainPerformanceMetricPathController {

    @Autowired
    private ChainPerformanceMetricPathService pathService;

    @PostMapping("")
    @ApiOperation(value = "创建性能指标路径配置", notes = "创建新的性能指标路径配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "性能指标路径管理", module = "链路追踪")
    public CreatedVO createPath(@RequestBody @Validated CreateOrUpdatePerformanceMetricPathDTO dto) {
        pathService.createPath(dto);
        return new CreatedVO(24);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新性能指标路径配置", notes = "更新指定ID的性能指标路径配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "性能指标路径管理", module = "链路追踪")
    public UpdatedVO updatePath(
            @ApiParam(value = "路径ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id,
            @RequestBody @Validated CreateOrUpdatePerformanceMetricPathDTO dto) {
        pathService.updatePath(id, dto);
        return new UpdatedVO(25);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除性能指标路径配置", notes = "删除指定ID的性能指标路径配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "性能指标路径管理", module = "链路追踪")
    public DeletedVO deletePath(
            @ApiParam(value = "路径ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id) {
        pathService.deletePath(id);
        return new DeletedVO(26);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取性能指标路径配置", notes = "根据ID获取性能指标路径配置详情")
    @LoginRequired
    @PermissionMeta(value = "性能指标路径查看", module = "链路追踪")
    public PerformanceMetricPathVO getPath(
            @ApiParam(value = "路径ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id) {
        return pathService.getPathById(id);
    }

    @GetMapping("/chain/{chainId}")
    @ApiOperation(value = "获取链路的所有性能指标路径", notes = "根据链路ID获取所有性能指标路径配置")
    @LoginRequired
    @PermissionMeta(value = "性能指标路径查看", module = "链路追踪")
    public List<PerformanceMetricPathVO> getPathsByChainId(
            @ApiParam(value = "链路ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long chainId) {
        return pathService.getPathsByChainId(chainId);
    }

    @GetMapping("/chain/{chainId}/metric-type/{metricType}")
    @ApiOperation(value = "根据指标类型获取路径", notes = "根据链路ID和指标类型获取性能指标路径配置")
    @LoginRequired
    @PermissionMeta(value = "性能指标路径查看", module = "链路追踪")
    public List<PerformanceMetricPathVO> getPathsByMetricType(
            @ApiParam(value = "链路ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long chainId,
            @ApiParam(value = "指标类型", required = true) @PathVariable String metricType) {
        return pathService.getPathsByChainIdAndMetricType(chainId, metricType);
    }

    @GetMapping("/{id}/latency-stats")
    @ApiOperation(value = "获取路径穿透时延统计数据", notes = "根据路径ID和时间范围获取路径穿透时延统计数据")
    @LoginRequired
    @PermissionMeta(value = "性能指标路径查看", module = "链路追踪")
    public PathLatencyStatsVO getPathLatencyStats(
            @ApiParam(value = "路径ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return pathService.getPathLatencyStats(id, startTime, endTime);
    }

    @GetMapping("/chain/{chainId}/latency-stats")
    @ApiOperation(value = "批量获取路径穿透时延统计数据", notes = "根据链路ID和时间范围批量获取所有路径的穿透时延统计数据")
    @LoginRequired
    @PermissionMeta(value = "性能指标路径查看", module = "链路追踪")
    public List<PathLatencyStatsVO> getPathsLatencyStats(
            @ApiParam(value = "链路ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long chainId,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return pathService.getPathsLatencyStats(chainId, startTime, endTime);
    }
}

