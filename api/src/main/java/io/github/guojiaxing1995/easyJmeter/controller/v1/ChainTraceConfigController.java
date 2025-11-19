package io.github.guojiaxing1995.easyJmeter.controller.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.guojiaxing1995.easyJmeter.common.util.PageUtil;
import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainTraceConfigDTO;
import io.github.guojiaxing1995.easyJmeter.dto.chain.QueryChainTraceConfigDTO;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.service.ChainTraceConfigService;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.vo.*;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.autoconfigure.exception.ParameterException;
import io.github.talelin.core.annotation.GroupRequired;
import io.github.talelin.core.annotation.LoginRequired;
import io.github.talelin.core.annotation.PermissionMeta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/chain/trace")
@Api(tags = "链路追踪配置")
@Validated
public class ChainTraceConfigController {

    @Autowired
    private ChainTraceConfigService chainTraceConfigService;

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    @PostMapping("")
    @ApiOperation(value = "创建链路配置", notes = "创建新的链路追踪配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public CreatedVO createChainTraceConfig(@RequestBody @Validated CreateOrUpdateChainTraceConfigDTO dto) {
        ChainTraceConfigDO config = chainTraceConfigService.createChainTraceConfig(dto);
        return new CreatedVO(21);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新链路配置", notes = "更新现有的链路追踪配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public UpdatedVO updateChainTraceConfig(@PathVariable @Positive(message = "ID必须为正数") Long id,
                                           @RequestBody @Validated CreateOrUpdateChainTraceConfigDTO dto) {
        dto.setId(id);
        ChainTraceConfigDO config = chainTraceConfigService.updateChainTraceConfig(dto);
        return new UpdatedVO(22);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除链路配置", notes = "删除指定的链路追踪配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public DeletedVO deleteChainTraceConfig(@PathVariable @Positive(message = "ID必须为正数") Long id) {
        boolean deleted = chainTraceConfigService.deleteChainTraceConfig(id);
        if (deleted) {
            return new DeletedVO(23);
        } else {
            throw new NotFoundException(10004, "链路配置不存在");
        }
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取链路配置详情", notes = "根据ID获取链路配置详细信息")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置查看", module = "链路追踪")
    public ChainTraceConfigVO getChainTraceConfig(@PathVariable @Positive(message = "ID必须为正数") Long id) {
        ChainTraceConfigDO config = chainTraceConfigService.getChainTraceConfigDetail(id);
        List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainIdOrderBySequence(id);

        return ChainTraceConfigVO.builder()
                .id(config.getId())
                .taskId(config.getTaskId())
                .chainName(config.getChainName())
                .chainDescription(config.getChainDescription())
                .version(config.getVersion())
                .status(config.getStatus())
                .createdBy(config.getCreatedBy())
                .createdTime(config.getCreatedTime())
                .updatedBy(config.getUpdatedBy())
                .updatedTime(config.getUpdatedTime())
                .nodeConfigs(nodes.stream()
                        .map(node -> ChainNodeConfigVO.builder()
                                .id(node.getId())
                                .chainId(node.getChainId())
                                .nodeName(node.getNodeName())
                                .nodeType(node.getNodeType())
                                .nodeDescription(node.getNodeDescription())
                                .sequenceOrder(node.getSequenceOrder())
                                .logPath(node.getLogPath())
                                .logPattern(node.getLogPattern())
                                .timestampPattern(node.getTimestampPattern())
                                .latencyPattern(node.getLatencyPattern())
                                .requestIdPattern(node.getRequestIdPattern())
                                .dataMapping(node.getDataMapping())
                                .status(node.getStatus())
                                .createdTime(node.getCreatedTime())
                                .updatedTime(node.getUpdatedTime())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/page")
    @ApiOperation(value = "分页查询链路配置", notes = "分页查询链路配置列表")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置查看", module = "链路追踪")
    public PageResponseVO<ChainTraceConfigVO> getChainTraceConfigPage(QueryChainTraceConfigDTO dto) {
        IPage<ChainTraceConfigDO> page = chainTraceConfigService.getChainTraceConfigPage(dto);

        List<ChainTraceConfigVO> voList = page.getRecords().stream()
                .map(config -> ChainTraceConfigVO.builder()
                        .id(config.getId())
                        .taskId(config.getTaskId())
                        .chainName(config.getChainName())
                        .chainDescription(config.getChainDescription())
                        .version(config.getVersion())
                        .status(config.getStatus())
                        .createdBy(config.getCreatedBy())
                        .createdTime(config.getCreatedTime())
                        .updatedBy(config.getUpdatedBy())
                        .updatedTime(config.getUpdatedTime())
                        .build())
                .collect(Collectors.toList());

        return PageUtil.build(page, voList);
    }

    @GetMapping("/task/{taskId}")
    @ApiOperation(value = "根据任务ID查询链路配置", notes = "获取指定测试任务的所有链路配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置查看", module = "链路追踪")
    public List<ChainTraceConfigVO> getChainTraceConfigsByTaskId(@PathVariable @Positive(message = "任务ID必须为正数") Long taskId) {
        List<ChainTraceConfigDO> configs = chainTraceConfigService.getChainTraceConfigsByTaskId(taskId);

        return configs.stream()
                .map(config -> ChainTraceConfigVO.builder()
                        .id(config.getId())
                        .taskId(config.getTaskId())
                        .chainName(config.getChainName())
                        .chainDescription(config.getChainDescription())
                        .version(config.getVersion())
                        .status(config.getStatus())
                        .createdBy(config.getCreatedBy())
                        .createdTime(config.getCreatedTime())
                        .updatedBy(config.getUpdatedBy())
                        .updatedTime(config.getUpdatedTime())
                        .build())
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/status")
    @ApiOperation(value = "更新链路配置状态", notes = "启用或禁用链路配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public UpdatedVO updateChainTraceConfigStatus(@PathVariable @Positive(message = "ID必须为正数") Long id,
                                                  @RequestParam @ApiParam(value = "状态：1-启用，0-禁用") Integer status) {
        boolean updated = chainTraceConfigService.updateChainTraceConfigStatus(id, status);
        if (updated) {
            return new UpdatedVO(22);
        } else {
            throw new NotFoundException(10004, "链路配置不存在");
        }
    }

    @PostMapping("/validate/name")
    @ApiOperation(value = "验证链路名称", notes = "检查链路名称是否可用")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public Map<String, Object> validateChainName(@RequestParam String chainName,
                                                 @RequestParam(required = false) Long excludeId) {
        boolean available = chainTraceConfigService.validateChainName(chainName, excludeId);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", available);
        result.put("message", available ? "链路名称可用" : "链路名称已存在");
        return result;
    }

    @PostMapping("/{id}/copy")
    @ApiOperation(value = "复制链路配置", notes = "基于现有配置创建新的链路配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public CreatedVO copyChainTraceConfig(@PathVariable @Positive(message = "ID必须为正数") Long id,
                                         @RequestParam String newChainName) {
        ChainTraceConfigDO newConfig = chainTraceConfigService.copyChainTraceConfig(id, newChainName);
        return new CreatedVO(21);
    }

    @GetMapping("/{id}/export")
    @ApiOperation(value = "导出链路配置", notes = "导出链路配置为JSON格式")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public ResponseEntity<String> exportChainTraceConfig(@PathVariable @Positive(message = "ID必须为正数") Long id) {
        String configData = chainTraceConfigService.exportChainTraceConfig(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=chain_config_" + id + ".json")
                .header("Content-Type", "application/json")
                .body(configData);
    }

    @PostMapping("/import")
    @ApiOperation(value = "导入链路配置", notes = "从JSON格式导入链路配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public CreatedVO importChainTraceConfig(@RequestBody String configData) {
        try {
            ChainTraceConfigDO newConfig = chainTraceConfigService.importChainTraceConfig(configData);
            return new CreatedVO(21);
        } catch (Exception e) {
            throw new ParameterException(20003, "配置数据格式错误");
        }
    }

    /**
     * 链路配置VO类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChainTraceConfigVO {
        private Long id;
        private Long taskId;
        private String chainName;
        private String chainDescription;
        private String version;
        private Integer status;
        private String createdBy;
        private java.time.LocalDateTime createdTime;
        private String updatedBy;
        private java.time.LocalDateTime updatedTime;
        private List<ChainNodeConfigVO> nodeConfigs;
    }

    /**
     * 链路节点配置VO类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChainNodeConfigVO {
        private Long id;
        private Long chainId;
        private String nodeName;
        private String nodeType;
        private String nodeDescription;
        private Integer sequenceOrder;
        private String logPath;
        private String logPattern;
        private String timestampPattern;
        private String latencyPattern;
        private String requestIdPattern;
        private String dataMapping;
        private Integer status;
        private java.time.LocalDateTime createdTime;
        private java.time.LocalDateTime updatedTime;
    }
}