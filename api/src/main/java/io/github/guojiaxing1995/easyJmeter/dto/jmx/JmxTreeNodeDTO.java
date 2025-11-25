package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Generic JMX tree node structure
 * Represents any element in the JMX test plan hierarchy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxTreeNodeDTO {
    
    /**
     * Unique identifier for this node (UUID)
     */
    private String id;
    
    /**
     * Node type: TestPlan, ThreadGroup, HTTPSampler, JavaSampler, etc.
     */
    private String type;
    
    /**
     * Display name of the element
     */
    private String name;
    
    /**
     * Whether this element is enabled
     */
    private Boolean enabled = true;
    
    /**
     * Element-specific properties
     */
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * Child nodes
     */
    private List<JmxTreeNodeDTO> children = new ArrayList<>();
    
    /**
     * GUI class name (for JMeter compatibility)
     */
    private String guiClass;
    
    /**
     * Test class name (for JMeter compatibility)
     */
    private String testClass;
    
    /**
     * Comments/description for this element
     */
    private String comments;
}

