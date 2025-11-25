package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.CopyJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.CreateOrUpdateJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxAssetStructureDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.model.JFileDO;
import io.github.guojiaxing1995.easyJmeter.model.JmxAssetDO;
import io.github.guojiaxing1995.easyJmeter.model.JmxStructureDO;
import io.github.guojiaxing1995.easyJmeter.service.JFileService;
import io.github.guojiaxing1995.easyJmeter.service.JmxAssetService;
import io.github.guojiaxing1995.easyJmeter.service.JmxStructureService;
import io.github.guojiaxing1995.easyJmeter.vo.CreatedVO;
import io.github.guojiaxing1995.easyJmeter.vo.DeletedVO;
import io.github.guojiaxing1995.easyJmeter.vo.JFileVO;
import io.github.guojiaxing1995.easyJmeter.vo.JmxAssetVO;
import io.github.guojiaxing1995.easyJmeter.vo.UpdatedVO;
import io.github.talelin.core.annotation.LoginRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/jmx")
@Api(tags = "JMX 资产管理")
@Validated
public class JmxAssetController {

    @Autowired
    private JmxAssetService jmxAssetService;

    @Autowired
    private JmxStructureService jmxStructureService;

    @Autowired
    private JFileService jFileService;

    @GetMapping("")
    @LoginRequired
    @ApiOperation(value = "JMX资产列表", notes = "按项目过滤可复用的JMX资产")
    public List<JmxAssetVO> list(@RequestParam(value = "projectId", required = false) Integer projectId) {
        List<JmxAssetDO> assets = jmxAssetService.listAssets(projectId);
        return assets.stream().map(this::toVO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @LoginRequired
    @ApiOperation(value = "JMX资产详情", notes = "获取单个JMX资产")
    public JmxAssetVO detail(@PathVariable("id") @Positive Integer id) {
        JmxAssetDO asset = jmxAssetService.getAsset(id);
        if (asset == null) {
            return null;
        }
        return toVO(asset);
    }

    @PostMapping("")
    @LoginRequired
    @ApiOperation(value = "新增JMX资产")
    public CreatedVO create(@RequestBody @Validated CreateOrUpdateJmxAssetDTO dto) {
        jmxAssetService.createAsset(dto);
        return new CreatedVO(1);
    }

    @PutMapping("/{id}")
    @LoginRequired
    @ApiOperation(value = "更新JMX资产")
    public UpdatedVO update(@PathVariable("id") @Positive Integer id,
                            @RequestBody @Validated CreateOrUpdateJmxAssetDTO dto) {
        jmxAssetService.updateAsset(id, dto);
        return new UpdatedVO(2);
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    @ApiOperation(value = "删除JMX资产")
    public DeletedVO delete(@PathVariable("id") @Positive Integer id) {
        jmxAssetService.deleteAsset(id);
        return new DeletedVO(3);
    }

    @PostMapping("/{id}/copy")
    @LoginRequired
    @ApiOperation(value = "复制JMX资产")
    public CreatedVO copy(@PathVariable("id") @Positive Integer id,
                          @RequestBody @Validated CopyJmxAssetDTO dto) {
        jmxAssetService.copyAsset(id, dto);
        return new CreatedVO(1);
    }

    @GetMapping("/{id}/structure")
    @LoginRequired
    @ApiOperation(value = "获取资产结构")
    public JmxTreeNodeDTO getStructure(@PathVariable("id") @Positive Integer id) {
        return jmxStructureService.getLatestStructureForAsset(id);
    }

    @PostMapping("/{id}/structure")
    @LoginRequired
    @ApiOperation(value = "保存资产结构")
    public UpdatedVO saveStructure(@PathVariable("id") @Positive Integer id,
                                   @RequestBody @Validated JmxAssetStructureDTO dto) {
        if (!id.equals(dto.getAssetId())) {
            throw new IllegalArgumentException("Asset id mismatch");
        }
        JmxStructureDO saved = jmxStructureService.saveAssetStructure(id, dto.getStructure());
        if (saved != null) {
            jmxAssetService.refreshLatestStructureVersion(id, saved.getVersion());
        }
        return new UpdatedVO(2);
    }

    @GetMapping("/{id}/versions")
    @LoginRequired
    @ApiOperation(value = "资产结构版本列表")
    public List<Integer> versions(@PathVariable("id") @Positive Integer id) {
        return jmxStructureService.getAllVersionsForAsset(id);
    }

    @GetMapping("/{id}/version/{version}")
    @LoginRequired
    @ApiOperation(value = "获取指定版本结构")
    public JmxTreeNodeDTO structureByVersion(@PathVariable("id") @Positive Integer id,
                                             @PathVariable("version") @Positive Integer version) {
        return jmxStructureService.getAssetStructureByVersion(id, version);
    }

    @PostMapping("/{id}/generate")
    @LoginRequired
    @ApiOperation(value = "生成JMX文件", notes = "根据结构生成JMX并上传")
    public UpdatedVO generate(@PathVariable("id") @Positive Integer id) throws Exception {
        JmxAssetDO asset = jmxAssetService.getAsset(id);
        if (asset == null) {
            throw new IllegalArgumentException("Asset not found");
        }
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "jmx-asset");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File outputFile = new File(tempDir, asset.getName() + ".jmx");
        try {
            jmxStructureService.generateJmxFileForAsset(id, outputFile);
            JFileDO uploaded = jFileService.createFile(outputFile.getAbsolutePath());
            asset.setJmxFileId(uploaded.getId());
            jmxAssetService.updateAsset(asset.getId(), toUpdateDTO(asset));
        } finally {
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        return new UpdatedVO(2);
    }

    private CreateOrUpdateJmxAssetDTO toUpdateDTO(JmxAssetDO asset) {
        CreateOrUpdateJmxAssetDTO dto = new CreateOrUpdateJmxAssetDTO();
        dto.setName(asset.getName());
        dto.setProjectId(asset.getProjectId());
        dto.setDescription(asset.getDescription());
        dto.setJmxFileId(asset.getJmxFileId());
        dto.setCreationMode(asset.getCreationMode());
        return dto;
    }

    private JmxAssetVO toVO(JmxAssetDO asset) {
        JmxAssetVO vo = new JmxAssetVO(asset);
        if (asset.getJmxFileId() != null) {
            JFileDO fileDO = jFileService.searchById(asset.getJmxFileId());
            if (fileDO != null) {
                vo.setJmxFile(new JFileVO(fileDO, formatSize(fileDO.getSize())));
            }
        }
        return vo;
    }

    private String formatSize(Long size) {
        if (size == null) {
            return "";
        }
        if (size >= 1024 * 1024) {
            return String.format("%.2fMB", size / (1024f * 1024f));
        }
        if (size >= 1024) {
            return String.format("%.2fKB", size / 1024f);
        }
        return size + "B";
    }
}


