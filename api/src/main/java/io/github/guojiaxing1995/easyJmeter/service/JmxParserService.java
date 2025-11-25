package io.github.guojiaxing1995.easyJmeter.service;

import com.alibaba.fastjson2.JSONObject;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;

import java.io.File;

/**
 * Service for parsing and generating JMX files
 */
public interface JmxParserService {
    
    /**
     * Parse JMX file to JSON tree structure
     * 
     * @param jmxFile JMX file to parse
     * @return Root node of the parsed tree
     * @throws Exception if parsing fails
     */
    JmxTreeNodeDTO parseJmxToJson(File jmxFile) throws Exception;
    
    /**
     * Generate JMX file from JSON tree structure
     * 
     * @param structure Root node of the tree
     * @param outputFile Output JMX file
     * @throws Exception if generation fails
     */
    void jsonToJmx(JmxTreeNodeDTO structure, File outputFile) throws Exception;
    
    /**
     * Validate JMX structure
     * 
     * @param structure Root node to validate
     * @return true if valid, false otherwise
     */
    boolean validateStructure(JmxTreeNodeDTO structure);
    
    /**
     * Create empty test plan structure
     * 
     * @param name Test plan name
     * @return Empty test plan node
     */
    JmxTreeNodeDTO createEmptyTestPlan(String name);
}

