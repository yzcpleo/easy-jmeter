package io.github.guojiaxing1995.easyJmeter.service.impl;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.*;
import io.github.guojiaxing1995.easyJmeter.service.JmxBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of JMX Builder Service
 */
@Slf4j
@Service
public class JmxBuilderServiceImpl implements JmxBuilderService {
    
    @Override
    public JmxTreeNodeDTO createEmptyTestPlan(String name) {
        JmxTreeNodeDTO testPlan = new JmxTreeNodeDTO();
        testPlan.setId(UUID.randomUUID().toString());
        testPlan.setType("TestPlan");
        testPlan.setName(name != null && !name.trim().isEmpty() ? name : "Test Plan");
        testPlan.setEnabled(true);
        testPlan.setGuiClass("TestPlanGui");
        testPlan.setTestClass("TestPlan");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("functionalMode", false);
        properties.put("serializeThreadGroups", false);
        properties.put("tearDownOnShutdown", true);
        testPlan.setProperties(properties);
        
        testPlan.setChildren(new ArrayList<>());
        
        log.info("Created empty test plan: {}", testPlan.getName());
        return testPlan;
    }
    
    @Override
    public JmxTreeNodeDTO addThreadGroup(JmxTreeNodeDTO testPlan, JmxThreadGroupDTO config) {
        JmxTreeNodeDTO threadGroup = new JmxTreeNodeDTO();
        threadGroup.setId(UUID.randomUUID().toString());
        threadGroup.setType("ThreadGroup");
        threadGroup.setName(config.getName() != null ? config.getName() : "Thread Group");
        threadGroup.setEnabled(true);
        threadGroup.setGuiClass("ThreadGroupGui");
        threadGroup.setTestClass("ThreadGroup");
        threadGroup.setComments(config.getComments());
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("numThreads", config.getNumThreads() != null ? config.getNumThreads() : 1);
        properties.put("rampTime", config.getRampTime() != null ? config.getRampTime() : 1);
        properties.put("loops", config.getLoops() != null ? config.getLoops() : 1);
        properties.put("duration", config.getDuration() != null ? config.getDuration() : 0L);
        properties.put("delay", config.getDelay() != null ? config.getDelay() : 0L);
        properties.put("scheduler", config.getScheduler() != null ? config.getScheduler() : false);
        properties.put("continueForever", config.getContinueForever() != null ? config.getContinueForever() : false);
        properties.put("sameUserOnNextIteration", config.getSameUserOnNextIteration() != null ? config.getSameUserOnNextIteration() : true);
        
        threadGroup.setProperties(properties);
        threadGroup.setChildren(new ArrayList<>());
        
        // Add to test plan
        if (testPlan.getChildren() == null) {
            testPlan.setChildren(new ArrayList<>());
        }
        testPlan.getChildren().add(threadGroup);
        
        log.info("Added thread group: {} to test plan", threadGroup.getName());
        return threadGroup;
    }
    
    @Override
    public JmxTreeNodeDTO addHttpSampler(JmxTreeNodeDTO parent, JmxHttpSamplerDTO config) {
        JmxTreeNodeDTO httpSampler = new JmxTreeNodeDTO();
        httpSampler.setId(UUID.randomUUID().toString());
        httpSampler.setType("HTTPSampler");
        httpSampler.setName(config.getName() != null ? config.getName() : "HTTP Request");
        httpSampler.setEnabled(true);
        httpSampler.setGuiClass("HttpTestSampleGui");
        httpSampler.setTestClass("HTTPSampler");
        httpSampler.setComments(config.getComments());
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("protocol", config.getProtocol() != null ? config.getProtocol() : "http");
        properties.put("domain", config.getDomain());
        properties.put("port", config.getPort() != null ? config.getPort() : 0);
        properties.put("path", config.getPath() != null ? config.getPath() : "");
        properties.put("method", config.getMethod() != null ? config.getMethod() : "GET");
        properties.put("contentEncoding", config.getContentEncoding() != null ? config.getContentEncoding() : "UTF-8");
        properties.put("followRedirects", config.getFollowRedirects() != null ? config.getFollowRedirects() : true);
        properties.put("autoRedirects", config.getAutoRedirects() != null ? config.getAutoRedirects() : false);
        properties.put("useKeepAlive", config.getUseKeepAlive() != null ? config.getUseKeepAlive() : true);
        
        if (config.getConnectTimeout() != null) {
            properties.put("connectTimeout", config.getConnectTimeout());
        }
        if (config.getResponseTimeout() != null) {
            properties.put("responseTimeout", config.getResponseTimeout());
        }
        
        // Parameters
        if (config.getParameters() != null && !config.getParameters().isEmpty()) {
            List<Map<String, String>> params = new ArrayList<>();
            for (JmxHttpSamplerDTO.KeyValuePair pair : config.getParameters()) {
                Map<String, String> param = new HashMap<>();
                param.put("key", pair.getKey());
                param.put("value", pair.getValue());
                param.put("description", pair.getDescription());
                params.add(param);
            }
            properties.put("parameters", params);
        }
        
        // Headers
        if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
            List<Map<String, String>> headers = new ArrayList<>();
            for (JmxHttpSamplerDTO.KeyValuePair pair : config.getHeaders()) {
                Map<String, String> header = new HashMap<>();
                header.put("key", pair.getKey());
                header.put("value", pair.getValue());
                headers.add(header);
            }
            properties.put("headers", headers);
        }
        
        // Body data
        if (config.getBodyData() != null && !config.getBodyData().isEmpty()) {
            properties.put("bodyData", config.getBodyData());
        }
        
        httpSampler.setProperties(properties);
        httpSampler.setChildren(new ArrayList<>());
        
        // Add to parent
        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(httpSampler);
        
        log.info("Added HTTP sampler: {} to parent", httpSampler.getName());
        return httpSampler;
    }
    
    @Override
    public JmxTreeNodeDTO addJavaSampler(JmxTreeNodeDTO parent, JmxJavaSamplerDTO config) {
        JmxTreeNodeDTO javaSampler = new JmxTreeNodeDTO();
        javaSampler.setId(UUID.randomUUID().toString());
        javaSampler.setType("JavaSampler");
        javaSampler.setName(config.getName() != null ? config.getName() : "Java Request");
        javaSampler.setEnabled(true);
        javaSampler.setGuiClass("JavaTestSampleGui");
        javaSampler.setTestClass("JavaSampler");
        javaSampler.setComments(config.getComments());
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("classname", config.getClassname());
        
        // Arguments
        if (config.getArguments() != null && !config.getArguments().isEmpty()) {
            List<Map<String, String>> args = new ArrayList<>();
            for (JmxJavaSamplerDTO.Argument arg : config.getArguments()) {
                Map<String, String> argMap = new HashMap<>();
                argMap.put("name", arg.getName());
                argMap.put("value", arg.getValue());
                argMap.put("description", arg.getDescription());
                args.add(argMap);
            }
            properties.put("arguments", args);
        }
        
        javaSampler.setProperties(properties);
        javaSampler.setChildren(new ArrayList<>());
        
        // Add to parent
        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(javaSampler);
        
        log.info("Added Java sampler: {} to parent", javaSampler.getName());
        return javaSampler;
    }
    
    @Override
    public JmxTreeNodeDTO addCsvDataSet(JmxTreeNodeDTO parent, JmxCsvDataSetDTO config) {
        JmxTreeNodeDTO csvDataSet = new JmxTreeNodeDTO();
        csvDataSet.setId(UUID.randomUUID().toString());
        csvDataSet.setType("CSVDataSet");
        csvDataSet.setName(config.getName() != null ? config.getName() : "CSV Data Set Config");
        csvDataSet.setEnabled(true);
        csvDataSet.setGuiClass("TestBeanGUI");
        csvDataSet.setTestClass("CSVDataSet");
        csvDataSet.setComments(config.getComments());
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("filename", config.getFilename());
        properties.put("fileEncoding", config.getFileEncoding() != null ? config.getFileEncoding() : "UTF-8");
        properties.put("variableNames", config.getVariableNames());
        properties.put("ignoreFirstLine", config.getIgnoreFirstLine() != null ? config.getIgnoreFirstLine() : false);
        properties.put("delimiter", config.getDelimiter() != null ? config.getDelimiter() : ",");
        properties.put("recycle", config.getRecycle() != null ? config.getRecycle() : true);
        properties.put("stopThread", config.getStopThread() != null ? config.getStopThread() : false);
        properties.put("shareMode", config.getShareMode() != null ? config.getShareMode() : "shareMode.all");
        
        csvDataSet.setProperties(properties);
        csvDataSet.setChildren(new ArrayList<>());
        
        // Add to parent
        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(csvDataSet);
        
        log.info("Added CSV Data Set: {} to parent", csvDataSet.getName());
        return csvDataSet;
    }
    
    @Override
    public JmxTreeNodeDTO getTemplate(String templateName) {
        log.info("Getting template: {}", templateName);
        
        switch (templateName.toLowerCase()) {
            case "simple":
                return createSimpleTemplate();
                
            case "load_test":
                return createLoadTestTemplate();
                
            case "stress_test":
                return createStressTestTemplate();
                
            default:
                log.warn("Unknown template: {}, returning empty test plan", templateName);
                return createEmptyTestPlan("Test Plan");
        }
    }
    
    /**
     * Create simple template with one HTTP request
     */
    private JmxTreeNodeDTO createSimpleTemplate() {
        JmxTreeNodeDTO testPlan = createEmptyTestPlan("Simple HTTP Test");
        
        JmxThreadGroupDTO threadGroupConfig = new JmxThreadGroupDTO();
        threadGroupConfig.setName("Simple Thread Group");
        threadGroupConfig.setNumThreads(1);
        threadGroupConfig.setRampTime(1);
        threadGroupConfig.setLoops(1);
        JmxTreeNodeDTO threadGroup = addThreadGroup(testPlan, threadGroupConfig);
        
        JmxHttpSamplerDTO httpConfig = new JmxHttpSamplerDTO();
        httpConfig.setName("HTTP Request");
        httpConfig.setProtocol("https");
        httpConfig.setDomain("example.com");
        httpConfig.setPort(443);
        httpConfig.setPath("/api");
        httpConfig.setMethod("GET");
        addHttpSampler(threadGroup, httpConfig);
        
        return testPlan;
    }
    
    /**
     * Create load test template
     */
    private JmxTreeNodeDTO createLoadTestTemplate() {
        JmxTreeNodeDTO testPlan = createEmptyTestPlan("Load Test");
        
        JmxThreadGroupDTO threadGroupConfig = new JmxThreadGroupDTO();
        threadGroupConfig.setName("Load Test Thread Group");
        threadGroupConfig.setNumThreads(10);
        threadGroupConfig.setRampTime(60);
        threadGroupConfig.setDuration(300L);
        threadGroupConfig.setScheduler(true);
        threadGroupConfig.setLoops(-1); // Infinite loops
        JmxTreeNodeDTO threadGroup = addThreadGroup(testPlan, threadGroupConfig);
        
        JmxHttpSamplerDTO httpConfig = new JmxHttpSamplerDTO();
        httpConfig.setName("HTTP Request");
        httpConfig.setProtocol("https");
        httpConfig.setDomain("example.com");
        httpConfig.setPort(443);
        httpConfig.setPath("/api");
        httpConfig.setMethod("GET");
        addHttpSampler(threadGroup, httpConfig);
        
        return testPlan;
    }
    
    /**
     * Create stress test template
     */
    private JmxTreeNodeDTO createStressTestTemplate() {
        JmxTreeNodeDTO testPlan = createEmptyTestPlan("Stress Test");
        
        JmxThreadGroupDTO threadGroupConfig = new JmxThreadGroupDTO();
        threadGroupConfig.setName("Stress Test Thread Group");
        threadGroupConfig.setNumThreads(100);
        threadGroupConfig.setRampTime(10);
        threadGroupConfig.setDuration(600L);
        threadGroupConfig.setScheduler(true);
        threadGroupConfig.setLoops(-1); // Infinite loops
        JmxTreeNodeDTO threadGroup = addThreadGroup(testPlan, threadGroupConfig);
        
        JmxHttpSamplerDTO httpConfig = new JmxHttpSamplerDTO();
        httpConfig.setName("HTTP Request");
        httpConfig.setProtocol("https");
        httpConfig.setDomain("example.com");
        httpConfig.setPort(443);
        httpConfig.setPath("/api");
        httpConfig.setMethod("GET");
        addHttpSampler(threadGroup, httpConfig);
        
        return testPlan;
    }
}

