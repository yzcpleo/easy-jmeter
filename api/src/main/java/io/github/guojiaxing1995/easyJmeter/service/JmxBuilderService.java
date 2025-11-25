package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.*;

/**
 * Service for building JMX structures
 */
public interface JmxBuilderService {
    
    /**
     * Create empty test plan
     * 
     * @param name Test plan name
     * @return Empty test plan structure
     */
    JmxTreeNodeDTO createEmptyTestPlan(String name);
    
    /**
     * Add thread group to test plan
     * 
     * @param testPlan Test plan node
     * @param config Thread group configuration
     * @return Thread group node
     */
    JmxTreeNodeDTO addThreadGroup(JmxTreeNodeDTO testPlan, JmxThreadGroupDTO config);
    
    /**
     * Add HTTP sampler to parent node
     * 
     * @param parent Parent node (usually thread group)
     * @param config HTTP sampler configuration
     * @return HTTP sampler node
     */
    JmxTreeNodeDTO addHttpSampler(JmxTreeNodeDTO parent, JmxHttpSamplerDTO config);
    
    /**
     * Add Java sampler to parent node
     * 
     * @param parent Parent node (usually thread group)
     * @param config Java sampler configuration
     * @return Java sampler node
     */
    JmxTreeNodeDTO addJavaSampler(JmxTreeNodeDTO parent, JmxJavaSamplerDTO config);
    
    /**
     * Add CSV data set to parent node
     * 
     * @param parent Parent node
     * @param config CSV data set configuration
     * @return CSV data set node
     */
    JmxTreeNodeDTO addCsvDataSet(JmxTreeNodeDTO parent, JmxCsvDataSetDTO config);
    
    /**
     * Get predefined templates
     * 
     * @param templateName Template name (e.g., "simple", "load_test", "stress_test")
     * @return Template structure
     */
    JmxTreeNodeDTO getTemplate(String templateName);
}

