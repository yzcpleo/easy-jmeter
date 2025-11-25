package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JMeter Test Plan configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxTestPlanDTO {
    
    /**
     * Test plan name
     */
    private String name;
    
    /**
     * User defined variables
     */
    private String userDefinedVariables;
    
    /**
     * Functional test mode
     */
    private Boolean functionalMode = false;
    
    /**
     * Serialize thread groups
     */
    private Boolean serializeThreadGroups = false;
    
    /**
     * Teardown on shutdown
     */
    private Boolean tearDownOnShutdown = true;
    
    /**
     * Comments
     */
    private String comments;
}

