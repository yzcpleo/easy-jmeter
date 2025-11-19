package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainNodeDTO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.service.ChainNodeConfigService;
import io.github.guojiaxing1995.easyJmeter.vo.CreatedVO;
import io.github.guojiaxing1995.easyJmeter.vo.DeletedVO;
import io.github.guojiaxing1995.easyJmeter.vo.UpdatedVO;
import io.github.talelin.core.annotation.GroupRequired;
import io.github.talelin.core.annotation.LoginRequired;
import io.github.talelin.core.annotation.PermissionMeta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.util.List;

/**
 * 链路节点配置控制器
 *
 * @author Assistant
 * @version 1.0.0
 */
@RestController
@RequestMapping("/v1/chain/node")
@Api(tags = "链路节点配置")
@Validated
public class ChainNodeConfigController {

    @Autowired
    private ChainNodeConfigService chainNodeConfigService;

    @PostMapping("")
    @ApiOperation(value = "创建节点配置", notes = "创建新的链路节点配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public CreatedVO createNode(@RequestBody @Validated CreateOrUpdateChainNodeDTO dto) {
        ChainNodeConfigDO node = chainNodeConfigService.createNode(dto);
        return new CreatedVO(21);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新节点配置", notes = "更新指定ID的节点配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public UpdatedVO updateNode(
            @ApiParam(value = "节点ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id,
            @RequestBody @Validated CreateOrUpdateChainNodeDTO dto) {
        chainNodeConfigService.updateNode(id, dto);
        return new UpdatedVO(22);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除节点配置", notes = "删除指定ID的节点配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public DeletedVO deleteNode(
            @ApiParam(value = "节点ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id) {
        chainNodeConfigService.deleteNode(id);
        return new DeletedVO(23);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取节点配置", notes = "根据ID获取节点配置详情")
    @LoginRequired
    @PermissionMeta(value = "链路配置查看", module = "链路追踪")
    public ChainNodeConfigDO getNode(
            @ApiParam(value = "节点ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long id) {
        return chainNodeConfigService.getNodeById(id);
    }

    @GetMapping("/chain/{chainId}")
    @ApiOperation(value = "获取链路的所有节点", notes = "根据链路ID获取所有节点配置")
    @LoginRequired
    @PermissionMeta(value = "链路配置查看", module = "链路追踪")
    public List<ChainNodeConfigDO> getNodesByChainId(
            @ApiParam(value = "链路ID", required = true) @PathVariable @Positive(message = "{id.positive}") Long chainId) {
        return chainNodeConfigService.getNodesByChainId(chainId);
    }

    @PostMapping("/batch")
    @ApiOperation(value = "批量创建节点", notes = "批量创建链路节点配置")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "链路配置管理", module = "链路追踪")
    public CreatedVO batchCreateNodes(
            @ApiParam(value = "链路ID", required = true) @RequestParam @Positive(message = "{id.positive}") Long chainId,
            @RequestBody @Validated List<CreateOrUpdateChainNodeDTO> dtos) {
        chainNodeConfigService.batchCreateNodes(chainId, dtos);
        return new CreatedVO(21);
    }
}
