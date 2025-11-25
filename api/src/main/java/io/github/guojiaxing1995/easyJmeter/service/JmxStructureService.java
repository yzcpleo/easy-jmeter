package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.model.JmxStructureDO;

import java.io.File;
import java.util.List;

/**
 * Service for managing JMX structures
 */
public interface JmxStructureService {
    
    /**
     * Parse JMX file and save structure to database
     * 
     * @param caseId Case ID
     * @param jmxFile JMX file to parse
     * @return Saved structure
     * @throws Exception if parsing or saving fails
     */
    JmxStructureDO parseAndSaveJmxStructure(Integer caseId, File jmxFile) throws Exception;
    
    /**
     * Save JMX structure to database
     * 
     * @param caseId Case ID
     * @param structure JMX tree structure
     * @return Saved structure
     */
    JmxStructureDO saveJmxStructure(Integer caseId, JmxTreeNodeDTO structure);
    
    /**
     * Get latest JMX structure for a case
     * 
     * @param caseId Case ID
     * @return JMX structure or null if not found
     */
    JmxTreeNodeDTO getLatestStructure(Integer caseId);
    
    /**
     * Get JMX structure by case ID and version
     * 
     * @param caseId Case ID
     * @param version Version number
     * @return JMX structure or null if not found
     */
    JmxTreeNodeDTO getStructureByVersion(Integer caseId, Integer version);
    
    /**
     * Generate JMX file from structure
     * 
     * @param caseId Case ID
     * @param outputFile Output file
     * @throws Exception if generation fails
     */
    void generateJmxFile(Integer caseId, File outputFile) throws Exception;
    
    /**
     * Generate JMX file from structure directly
     * 
     * @param structure JMX tree structure
     * @param outputFile Output file
     * @throws Exception if generation fails
     */
    void generateJmxFileFromStructure(JmxTreeNodeDTO structure, File outputFile) throws Exception;
    
    /**
     * Get all versions for a case
     * 
     * @param caseId Case ID
     * @return List of versions
     */
    List<Integer> getAllVersions(Integer caseId);
}

