package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.dto.jcase.CreateOrUpdateCaseDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxStructureDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.mapper.CaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.CaseDO;
import io.github.guojiaxing1995.easyJmeter.model.JFileDO;
import io.github.guojiaxing1995.easyJmeter.service.CaseService;
import io.github.guojiaxing1995.easyJmeter.service.JFileService;
import io.github.guojiaxing1995.easyJmeter.service.JmxParserService;
import io.github.guojiaxing1995.easyJmeter.service.JmxStructureService;
import io.github.guojiaxing1995.easyJmeter.vo.*;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.core.annotation.GroupRequired;
import io.github.talelin.core.annotation.LoginRequired;
import io.github.talelin.core.annotation.PermissionMeta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/case")
@Api(tags = "用例管理")
@Validated
public class CaseController {

    @Autowired
    private CaseService caseService;
    
    @Autowired
    private JmxStructureService jmxStructureService;
    
    @Autowired
    private JFileService jFileService;
    
    @Autowired
    private CaseMapper caseMapper;
    
    @Autowired
    private JmxParserService jmxParserService;

    @PostMapping("")
    @ApiOperation(value = "用例新增", notes = "创建测试用例")
    @LoginRequired
    public CreatedVO creatCase(@RequestBody @Validated CreateOrUpdateCaseDTO validator) {
        caseService.createCase(validator);
        return new CreatedVO(1);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "用例更新", notes = "更新测试用例")
    @LoginRequired
    public UpdatedVO updateCase(@PathVariable("id") @Positive(message = "{id.positive}") Integer id, @RequestBody @Validated CreateOrUpdateCaseDTO validator) {
        CaseDO caseDO = caseService.getById(id);
        caseService.updateCase(caseDO, validator);
        return new UpdatedVO(2);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "用例删除", notes = "删除指定用例")
    @LoginRequired
    public DeletedVO deleteCase(@PathVariable("id") @Positive(message = "{id.positive}") Integer id){
        caseService.deleteCase(id);
        return new DeletedVO(3);
    }

    @GetMapping("")
    @ApiOperation(value = "用例列表", notes = "可根据id查询，id非必填")
    @LoginRequired
    @GroupRequired
    @PermissionMeta(value = "用例管理", module = "用例")
    public List<CaseInfoVO> getCases(@RequestParam(value = "id", required = false, defaultValue = "") Integer id) {
        return caseService.selectCase(id);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "用例详情", notes = "获取一个用例详情")
    @LoginRequired
    public CaseInfoPlusVO getCaseInfo(@PathVariable("id") @Positive(message = "{id.positive}") Integer id){
        return caseService.getCaseInfoById(id);
    }
    
    // ==================== JMX Structure Management ====================
    
    @GetMapping("/{id}/jmx/parse")
    @ApiOperation(value = "解析JMX文件", notes = "Parse uploaded JMX file to JSON structure")
    @LoginRequired
    public JmxTreeNodeDTO parseJmx(@PathVariable("id") @Positive(message = "{id.positive}") Integer id) {
        log.info("Parsing JMX for case: {}", id);
        try {
            return parseJmxInternal(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Parsing JMX failed for case {}, fallback to empty structure: {}", id, e.getMessage());
            return null;
        }
    }

    private JmxTreeNodeDTO parseJmxInternal(Integer id) throws Exception {
        // Check if structure already exists
        JmxTreeNodeDTO existingStructure = jmxStructureService.getLatestStructure(id);
        if (existingStructure != null) {
            log.info("JMX structure already exists for case {}, returning cached version", id);
            return existingStructure;
        }

        // Structure doesn't exist, try to parse from file
        CaseDO caseDO = caseService.getById(id);
        if (caseDO == null) {
            throw new NotFoundException(12201);
        }

        if (caseDO.getJmx() == null || caseDO.getJmx().isEmpty()) {
            log.warn("Case {} has no JMX file uploaded, returning null to allow empty structure creation", id);
            return null;
        }

        // Get JMX file
        String jmxFileId = caseDO.getJmx().split(",")[0];
        JFileDO jFileDO = jFileService.searchById(Integer.parseInt(jmxFileId));
        if (jFileDO == null) {
            log.warn("JMX file record not found in database for case {}, file ID: {}, returning null", id, jmxFileId);
            return null;
        }

        File jmxFile = null;
        try {
            jmxFile = jFileService.getFileFromUrl(jFileDO);
            if (jmxFile == null || !jmxFile.exists()) {
                log.warn("JMX file does not exist in storage for case {}, path: {}, returning null", id, jFileDO.getPath());
                return null;
            }

            jmxStructureService.parseAndSaveJmxStructure(id, jmxFile);
            return jmxStructureService.getLatestStructure(id);
        } catch (Exception e) {
            log.error("Failed to parse JMX file for case {}: {}", id, e.getMessage(), e);
            if (e.getMessage() != null && (e.getMessage().contains("Object does not exist") ||
                                           e.getMessage().contains("NoSuchKey") ||
                                           e.getMessage().contains("does not exist"))) {
                log.warn("JMX file not found in storage for case {}, returning null to allow empty structure creation", id);
                return null;
            }
            log.warn("Unable to parse JMX file for case {}, returning null: {}", id, e.getMessage());
            return null;
        } finally {
            if (jmxFile != null && jmxFile.getAbsolutePath().contains("temp")) {
                try {
                    jmxFile.delete();
                } catch (Exception e) {
                    log.warn("Failed to delete temporary file: {}", jmxFile.getAbsolutePath());
                }
            }
        }
    }
    
    @GetMapping("/{id}/jmx/tree")
    @ApiOperation(value = "获取JMX结构树", notes = "Get JMX tree structure for display")
    @LoginRequired
    public JmxTreeNodeDTO getJmxTree(@PathVariable("id") @Positive(message = "{id.positive}") Integer id) {
        log.info("Getting JMX tree for case: {}", id);
        return jmxStructureService.getLatestStructure(id);
    }
    
    @PostMapping("/{id}/jmx/structure")
    @ApiOperation(value = "保存JMX结构", notes = "Save edited JMX structure")
    @LoginRequired
    public Map<String, Object> saveJmxStructure(
            @PathVariable("id") @Positive(message = "{id.positive}") Integer id,
            @RequestBody @Validated JmxStructureDTO dto) {
        log.info("Saving JMX structure for case: {}", id);
        
        // Save structure
        jmxStructureService.saveJmxStructure(id, dto.getStructure());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "JMX structure saved successfully");
        
        return result;
    }
    
    @PostMapping("/{id}/jmx/generate")
    @ApiOperation(value = "生成JMX文件", notes = "Generate JMX file from JSON structure")
    @LoginRequired
    public Map<String, Object> generateJmx(@PathVariable("id") @Positive(message = "{id.positive}") Integer id) throws Exception {
        log.info("Generating JMX file for case: {}", id);
        
        CaseDO caseDO = caseService.getById(id);
        if (caseDO == null) {
            throw new NotFoundException(12201);
        }
        
        // Generate JMX file to temp directory
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "jmx");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        String fileName = caseDO.getName() + ".jmx";
        File outputFile = new File(tempDir, fileName);
        jmxStructureService.generateJmxFile(id, outputFile);
        
        // Upload to MinIO/file storage
        JFileDO uploadedFile = jFileService.createFile(outputFile.getAbsolutePath());
        
        // Update case with new JMX file (replace existing jmx field)
        caseDO.setJmx(String.valueOf(uploadedFile.getId()));
        caseMapper.updateById(caseDO);
        
        // Clean up temp file
        if (outputFile.exists()) {
            outputFile.delete();
        }
        
        log.info("JMX file generated and uploaded: fileId={}, path={}", uploadedFile.getId(), uploadedFile.getPath());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "JMX file generated and uploaded successfully");
        result.put("fileId", uploadedFile.getId());
        result.put("filePath", uploadedFile.getPath());
        
        return result;
    }
    
    @GetMapping("/{id}/jmx/versions")
    @ApiOperation(value = "获取JMX版本列表", notes = "Get all JMX structure versions")
    @LoginRequired
    public List<Integer> getJmxVersions(@PathVariable("id") @Positive(message = "{id.positive}") Integer id) {
        log.info("Getting JMX versions for case: {}", id);
        return jmxStructureService.getAllVersions(id);
    }
    
    @GetMapping("/{id}/jmx/version/{version}")
    @ApiOperation(value = "获取指定版本的JMX结构", notes = "Get JMX structure by version")
    @LoginRequired
    public JmxTreeNodeDTO getJmxByVersion(
            @PathVariable("id") @Positive(message = "{id.positive}") Integer id,
            @PathVariable("version") @Positive(message = "{version.positive}") Integer version) {
        log.info("Getting JMX structure for case: {}, version: {}", id, version);
        return jmxStructureService.getStructureByVersion(id, version);
    }

}
