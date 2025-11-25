package io.github.guojiaxing1995.easyJmeter.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.mapper.JmxStructureMapper;
import io.github.guojiaxing1995.easyJmeter.model.JmxStructureDO;
import io.github.guojiaxing1995.easyJmeter.service.JmxParserService;
import io.github.guojiaxing1995.easyJmeter.service.JmxStructureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of JMX Structure Service
 */
@Slf4j
@Service
public class JmxStructureServiceImpl implements JmxStructureService {
    
    @Autowired
    private JmxStructureMapper jmxStructureMapper;
    
    @Autowired
    private JmxParserService jmxParserService;
    
    @Override
    @Transactional
    public JmxStructureDO parseAndSaveJmxStructure(Integer caseId, File jmxFile) throws Exception {
        log.info("Parsing and saving JMX structure for case: {}", caseId);
        
        // Parse JMX file
        JmxTreeNodeDTO structure = jmxParserService.parseJmxToJson(jmxFile);
        
        // Save to database
        return saveJmxStructure(caseId, structure);
    }
    
    @Override
    @Transactional
    public JmxStructureDO saveJmxStructure(Integer caseId, JmxTreeNodeDTO structure) {
        log.info("Saving JMX structure for case: {}", caseId);
        
        // Get latest version
        JmxStructureDO latest = jmxStructureMapper.findLatestByCaseId(caseId);
        int newVersion = latest != null ? latest.getVersion() + 1 : 1;
        
        // Create new structure record
        JmxStructureDO structureDO = new JmxStructureDO();
        structureDO.setCaseId(caseId);
        structureDO.setStructureJson(JSON.toJSONString(structure));
        structureDO.setVersion(newVersion);
        
        jmxStructureMapper.insert(structureDO);
        
        log.info("JMX structure saved with version: {}", newVersion);
        return structureDO;
    }
    
    @Override
    public JmxTreeNodeDTO getLatestStructure(Integer caseId) {
        log.info("Getting latest JMX structure for case: {}", caseId);
        
        JmxStructureDO structureDO = jmxStructureMapper.findLatestByCaseId(caseId);
        if (structureDO == null) {
            log.warn("No JMX structure found for case: {}", caseId);
            return null;
        }
        
        return JSON.parseObject(structureDO.getStructureJson(), JmxTreeNodeDTO.class);
    }
    
    @Override
    public JmxTreeNodeDTO getStructureByVersion(Integer caseId, Integer version) {
        log.info("Getting JMX structure for case: {}, version: {}", caseId, version);
        
        JmxStructureDO structureDO = jmxStructureMapper.findByCaseIdAndVersion(caseId, version);
        if (structureDO == null) {
            log.warn("No JMX structure found for case: {}, version: {}", caseId, version);
            return null;
        }
        
        return JSON.parseObject(structureDO.getStructureJson(), JmxTreeNodeDTO.class);
    }
    
    @Override
    public void generateJmxFile(Integer caseId, File outputFile) throws Exception {
        log.info("Generating JMX file for case: {}", caseId);
        
        JmxTreeNodeDTO structure = getLatestStructure(caseId);
        if (structure == null) {
            throw new Exception("No JMX structure found for case: " + caseId);
        }
        
        jmxParserService.jsonToJmx(structure, outputFile);
        
        log.info("JMX file generated: {}", outputFile.getAbsolutePath());
    }
    
    @Override
    public void generateJmxFileFromStructure(JmxTreeNodeDTO structure, File outputFile) throws Exception {
        log.info("Generating JMX file from structure");
        
        if (structure == null) {
            throw new Exception("Structure cannot be null");
        }
        
        jmxParserService.jsonToJmx(structure, outputFile);
        
        log.info("JMX file generated: {}", outputFile.getAbsolutePath());
    }
    
    @Override
    public List<Integer> getAllVersions(Integer caseId) {
        log.info("Getting all versions for case: {}", caseId);
        
        List<JmxStructureDO> structures = jmxStructureMapper.findAllVersionsByCaseId(caseId);
        
        return structures.stream()
                .map(JmxStructureDO::getVersion)
                .collect(Collectors.toList());
    }
}

