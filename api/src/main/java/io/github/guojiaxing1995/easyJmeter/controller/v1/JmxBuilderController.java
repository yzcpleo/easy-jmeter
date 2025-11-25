package io.github.guojiaxing1995.easyJmeter.controller.v1;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.*;
import io.github.guojiaxing1995.easyJmeter.service.JmxBuilderService;
import io.github.guojiaxing1995.easyJmeter.service.JmxParserService;
import io.github.guojiaxing1995.easyJmeter.vo.CreatedVO;
import io.github.talelin.core.annotation.LoginRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JMX Builder Controller
 * Handles JMX creation and manipulation
 */
@Slf4j
@RestController
@RequestMapping("/v1/jmx/builder")
@Api(tags = "JMX Builder Management")
@Validated
public class JmxBuilderController {
    
    @Autowired
    private JmxBuilderService jmxBuilderService;
    
    @Autowired
    private JmxParserService jmxParserService;
    
    @PostMapping("/create")
    @ApiOperation(value = "Create empty test plan", notes = "Create a new empty JMX test plan")
    @LoginRequired
    public JmxTreeNodeDTO createEmptyTestPlan(@RequestParam(value = "name", required = false, defaultValue = "Test Plan") String name) {
        log.info("Creating empty test plan: {}", name);
        return jmxBuilderService.createEmptyTestPlan(name);
    }
    
    @PostMapping("/template/{templateName}")
    @ApiOperation(value = "Get template", notes = "Get predefined JMX template (simple, load_test, stress_test)")
    @LoginRequired
    public JmxTreeNodeDTO getTemplate(@PathVariable String templateName) {
        log.info("Getting template: {}", templateName);
        return jmxBuilderService.getTemplate(templateName);
    }
    
    @PostMapping("/validate")
    @ApiOperation(value = "Validate JMX structure", notes = "Validate JMX structure for correctness")
    @LoginRequired
    public Map<String, Object> validateStructure(@RequestBody @Validated JmxStructureDTO dto) {
        log.info("Validating JMX structure for case: {}", dto.getCaseId());
        
        boolean isValid = jmxParserService.validateStructure(dto.getStructure());
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValid);
        result.put("message", isValid ? "Structure is valid" : "Structure validation failed");
        
        return result;
    }
    
    @PostMapping("/threadgroup")
    @ApiOperation(value = "Add thread group", notes = "Add a thread group to test plan")
    @LoginRequired
    public JmxTreeNodeDTO addThreadGroup(@RequestBody @Validated JmxThreadGroupDTO config) {
        log.info("Adding thread group: {}", config.getName());
        
        // This is a helper endpoint that returns the thread group structure
        // The actual addition should be done on the frontend by manipulating the tree
        JmxTreeNodeDTO testPlan = jmxBuilderService.createEmptyTestPlan("Temp");
        return jmxBuilderService.addThreadGroup(testPlan, config);
    }
    
    @PostMapping("/httpsampler")
    @ApiOperation(value = "Add HTTP sampler", notes = "Add an HTTP sampler")
    @LoginRequired
    public JmxTreeNodeDTO addHttpSampler(@RequestBody @Validated JmxHttpSamplerDTO config) {
        log.info("Adding HTTP sampler: {}", config.getName());
        
        // This is a helper endpoint that returns the HTTP sampler structure
        // The actual addition should be done on the frontend by manipulating the tree
        JmxTreeNodeDTO parent = new JmxTreeNodeDTO();
        parent.setType("ThreadGroup");
        parent.setName("Temp");
        return jmxBuilderService.addHttpSampler(parent, config);
    }
    
    @PostMapping("/javasampler")
    @ApiOperation(value = "Add Java sampler", notes = "Add a Java sampler")
    @LoginRequired
    public JmxTreeNodeDTO addJavaSampler(@RequestBody @Validated JmxJavaSamplerDTO config) {
        log.info("Adding Java sampler: {}", config.getName());
        
        // This is a helper endpoint that returns the Java sampler structure
        // The actual addition should be done on the frontend by manipulating the tree
        JmxTreeNodeDTO parent = new JmxTreeNodeDTO();
        parent.setType("ThreadGroup");
        parent.setName("Temp");
        return jmxBuilderService.addJavaSampler(parent, config);
    }
    
    @PostMapping("/csvdataset")
    @ApiOperation(value = "Add CSV data set", notes = "Add a CSV data set config")
    @LoginRequired
    public JmxTreeNodeDTO addCsvDataSet(@RequestBody @Validated JmxCsvDataSetDTO config) {
        log.info("Adding CSV data set: {}", config.getName());
        
        // This is a helper endpoint that returns the CSV data set structure
        // The actual addition should be done on the frontend by manipulating the tree
        JmxTreeNodeDTO parent = new JmxTreeNodeDTO();
        parent.setType("ThreadGroup");
        parent.setName("Temp");
        return jmxBuilderService.addCsvDataSet(parent, config);
    }
    
    @GetMapping("/templates")
    @ApiOperation(value = "List templates", notes = "Get list of available templates")
    @LoginRequired
    public Map<String, String> listTemplates() {
        Map<String, String> templates = new HashMap<>();
        templates.put("simple", "Simple HTTP Test - Single thread with one HTTP request");
        templates.put("load_test", "Load Test - 10 threads running for 5 minutes");
        templates.put("stress_test", "Stress Test - 100 threads running for 10 minutes");
        return templates;
    }
}

