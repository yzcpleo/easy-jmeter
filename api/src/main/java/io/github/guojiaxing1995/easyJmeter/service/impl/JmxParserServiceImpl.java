package io.github.guojiaxing1995.easyJmeter.service.impl;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.service.JmxParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.protocol.java.sampler.JavaSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Implementation of JMX Parser Service
 */
@Slf4j
@Service
public class JmxParserServiceImpl implements JmxParserService {
    
    @Value("${jmeter.home:}")
    private String jmeterHome;
    
    private static boolean jmeterInitialized = false;
    
    private static final Map<String, ClassMetadata> DEFAULT_CLASS_METADATA = new HashMap<>();
    static {
        DEFAULT_CLASS_METADATA.put("TestPlan", new ClassMetadata("TestPlanGui", "TestPlan"));
        DEFAULT_CLASS_METADATA.put("ThreadGroup", new ClassMetadata("ThreadGroupGui", "ThreadGroup"));
        DEFAULT_CLASS_METADATA.put("HTTPSampler", new ClassMetadata("HttpTestSampleGui", "HTTPSamplerProxy"));
        DEFAULT_CLASS_METADATA.put("HTTPSamplerProxy", new ClassMetadata("HttpTestSampleGui", "HTTPSamplerProxy"));
        DEFAULT_CLASS_METADATA.put("JavaSampler", new ClassMetadata("JavaTestSamplerGui", "JavaSampler"));
        DEFAULT_CLASS_METADATA.put("CSVDataSet", new ClassMetadata("TestBeanGUI", "CSVDataSet"));
        DEFAULT_CLASS_METADATA.put("HeaderManager", new ClassMetadata("HeaderPanel", "HeaderManager"));
        DEFAULT_CLASS_METADATA.put("ResultCollector", new ClassMetadata("ViewResultsFullVisualizer", "ResultCollector"));
    }
    
    @PostConstruct
    public void initJMeter() {
        if (jmeterInitialized) {
            return;
        }
        
        try {
            log.info("Initializing JMeter environment...");
            
            // Strategy 1: Try configured JMeter home first
            if (tryInitWithConfiguredHome()) {
                log.info("JMeter initialized from configured home: {}", jmeterHome);
                jmeterInitialized = true;
                return;
            }
            
            // Strategy 2: Use embedded configuration files from classpath
            if (tryInitWithEmbeddedConfig()) {
                log.info("JMeter initialized with embedded configuration");
                jmeterInitialized = true;
                return;
            }
            
            // Strategy 3: Try common installation locations
            if (tryInitWithCommonLocations()) {
                log.info("JMeter initialized from common installation location");
                jmeterInitialized = true;
                return;
            }
            
            log.error("Failed to initialize JMeter with any available strategy");
            throw new RuntimeException("JMeter initialization failed");
            
        } catch (Exception e) {
            log.error("Failed to initialize JMeter: {}", e.getMessage(), e);
            throw new RuntimeException("JMeter initialization failed", e);
        }
    }
    
    private boolean tryInitWithConfiguredHome() {
        if (jmeterHome == null || jmeterHome.isEmpty()) {
            return false;
        }
        
        File jmeterDir = new File(jmeterHome);
        File propsFile = new File(jmeterDir, "bin/jmeter.properties");
        
        if (jmeterDir.exists() && propsFile.exists()) {
            JMeterUtils.setJMeterHome(jmeterHome);
            JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
            JMeterUtils.initLocale();
            return true;
        }
        
        return false;
    }
    
    private boolean tryInitWithEmbeddedConfig() {
        try {
            // Create temp directory for JMeter home
            File tempJMeterHome = new File(System.getProperty("java.io.tmpdir"), "jmeter-embedded");
            File binDir = new File(tempJMeterHome, "bin");
            binDir.mkdirs();
            
            // Copy configuration files from classpath to temp directory
            String[] configFiles = {
                "jmeter.properties",
                "saveservice.properties",
                "upgrade.properties"
            };
            
            for (String configFile : configFiles) {
                InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("jmeter/bin/" + configFile);
                
                if (is == null) {
                    log.warn("Configuration file not found in classpath: jmeter/bin/{}", configFile);
                    return false;
                }
                
                File targetFile = new File(binDir, configFile);
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                is.close();
                
                log.debug("Copied {} to {}", configFile, targetFile.getAbsolutePath());
            }
            
            // Initialize JMeter with temp home
            JMeterUtils.setJMeterHome(tempJMeterHome.getAbsolutePath());
            JMeterUtils.loadJMeterProperties(new File(binDir, "jmeter.properties").getAbsolutePath());
            JMeterUtils.initLocale();
            
            log.info("JMeter initialized with embedded config at: {}", tempJMeterHome.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to initialize JMeter with embedded config: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean tryInitWithCommonLocations() {
        String[] possiblePaths = {
            "F:/apache-jmeter-5.5",
            "C:/apache-jmeter-5.5",
            "D:/apache-jmeter-5.5",
            System.getProperty("user.dir") + "/jmeter",
            System.getProperty("user.home") + "/apache-jmeter-5.5"
        };
        
        for (String path : possiblePaths) {
            File jmeterDir = new File(path);
            File propsFile = new File(jmeterDir, "bin/jmeter.properties");
            
            if (jmeterDir.exists() && propsFile.exists()) {
                JMeterUtils.setJMeterHome(path);
                JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
                JMeterUtils.initLocale();
                log.info("Found JMeter installation at: {}", path);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public JmxTreeNodeDTO parseJmxToJson(File jmxFile) throws Exception {
        log.info("Parsing JMX file: {}", jmxFile.getAbsolutePath());
        
        try {
            // Try using JMeter SaveService first
            HashTree testPlanTree = SaveService.loadTree(jmxFile);
            
            // Get the root element (TestPlan)
            Object[] rootArray = testPlanTree.getArray();
            if (rootArray.length == 0) {
                throw new Exception("JMX file has no test plan");
            }
            
            TestElement rootElement = (TestElement) rootArray[0];
            JmxTreeNodeDTO rootNode = parseTestElement(rootElement, testPlanTree);
            
            log.info("JMX file parsed successfully using SaveService");
            return rootNode;
            
        } catch (Exception e) {
            log.warn("Failed to parse JMX using SaveService: {}. Trying XML parser fallback.", e.getMessage());
            // Fallback to simple XML parsing
            return parseJmxUsingXml(jmxFile);
        }
    }
    
    /**
     * Fallback method to parse JMX file as plain XML
     */
    private JmxTreeNodeDTO parseJmxUsingXml(File jmxFile) throws Exception {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(jmxFile);
            doc.getDocumentElement().normalize();
            
            // Find the jmeterTestPlan element
            org.w3c.dom.Element root = doc.getDocumentElement();
            if (!"jmeterTestPlan".equals(root.getNodeName())) {
                throw new Exception("Root element is not jmeterTestPlan");
            }
            
            // Find the TestPlan element (usually hashTree > TestPlan)
            org.w3c.dom.NodeList hashTrees = root.getElementsByTagName("hashTree");
            if (hashTrees.getLength() == 0) {
                throw new Exception("No hashTree found in JMX");
            }
            
            org.w3c.dom.Element firstHashTree = (org.w3c.dom.Element) hashTrees.item(0);
            org.w3c.dom.NodeList children = firstHashTree.getChildNodes();
            
            JmxTreeNodeDTO rootNode = null;
            org.w3c.dom.Element testPlanElement = null;
            org.w3c.dom.Element testPlanHashTree = null;
            
            // Find TestPlan and its corresponding hashTree
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node node = children.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                    if ("TestPlan".equals(element.getNodeName())) {
                        testPlanElement = element;
                        // Next hashTree is the TestPlan's children
                        for (int j = i + 1; j < children.getLength(); j++) {
                            org.w3c.dom.Node nextNode = children.item(j);
                            if (nextNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && 
                                "hashTree".equals(nextNode.getNodeName())) {
                                testPlanHashTree = (org.w3c.dom.Element) nextNode;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            
            if (testPlanElement == null) {
                throw new Exception("No TestPlan element found in JMX");
            }
            
            rootNode = parseXmlElement(testPlanElement, testPlanHashTree);
            log.info("JMX file parsed successfully using XML parser");
            return rootNode;
            
        } catch (Exception e) {
            log.error("Failed to parse JMX file as XML: {}", e.getMessage(), e);
            throw new Exception("Failed to parse JMX file: " + e.getMessage());
        }
    }
    
    /**
     * Parse an XML element to JmxTreeNodeDTO
     */
    private JmxTreeNodeDTO parseXmlElement(org.w3c.dom.Element element, org.w3c.dom.Element hashTree) {
        JmxTreeNodeDTO node = new JmxTreeNodeDTO();
        node.setId(UUID.randomUUID().toString());
        node.setType(element.getNodeName());
        
        // Extract basic attributes
        if (element.hasAttribute("testname")) {
            node.setName(element.getAttribute("testname"));
        }
        if (element.hasAttribute("enabled")) {
            node.setEnabled(Boolean.parseBoolean(element.getAttribute("enabled")));
        } else {
            node.setEnabled(true); // Default to enabled
        }
        if (element.hasAttribute("guiclass")) {
            node.setGuiClass(element.getAttribute("guiclass"));
        }
        if (element.hasAttribute("testclass")) {
            node.setTestClass(element.getAttribute("testclass"));
        }
        
        // Extract properties
        Map<String, Object> properties = new HashMap<>();
        org.w3c.dom.NodeList propElements = element.getChildNodes();
        for (int i = 0; i < propElements.getLength(); i++) {
            org.w3c.dom.Node propNode = propElements.item(i);
            if (propNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.Element propElement = (org.w3c.dom.Element) propNode;
                String propName = propElement.getAttribute("name");
                if (propName != null && !propName.isEmpty()) {
                    String propValue = propElement.getTextContent();
                    properties.put(propName, propValue);
                }
            }
        }
        node.setProperties(properties);
        
        // Parse children from hashTree
        List<JmxTreeNodeDTO> children = new ArrayList<>();
        if (hashTree != null) {
            org.w3c.dom.NodeList childNodes = hashTree.getChildNodes();
            org.w3c.dom.Element currentElement = null;
            
            for (int i = 0; i < childNodes.getLength(); i++) {
                org.w3c.dom.Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element childElement = (org.w3c.dom.Element) childNode;
                    
                    if ("hashTree".equals(childElement.getNodeName())) {
                        // This is the hashTree for the previous element
                        if (currentElement != null) {
                            JmxTreeNodeDTO childDto = parseXmlElement(currentElement, childElement);
                            children.add(childDto);
                            currentElement = null;
                        }
                    } else {
                        // This is a test element
                        currentElement = childElement;
                    }
                }
            }
            
            // Handle last element if it has no children (empty hashTree)
            if (currentElement != null) {
                JmxTreeNodeDTO childDto = parseXmlElement(currentElement, null);
                children.add(childDto);
            }
        }
        
        node.setChildren(children);
        
        // Special handling for JavaSampler arguments
        // If arguments is a string (from XML parsing), convert it to array format
        if ("JavaSampler".equals(node.getType()) && properties.containsKey("arguments")) {
            Object argumentsObj = properties.get("arguments");
            if (argumentsObj instanceof String) {
                String argumentsStr = (String) argumentsObj;
                List<Map<String, String>> argsList = parseJavaSamplerArguments(argumentsStr);
                if (!argsList.isEmpty()) {
                    properties.put("arguments", argsList);
                }
            }
        }
        
        // Extract comments
        if (properties.containsKey("TestPlan.comments")) {
            node.setComments(properties.get("TestPlan.comments").toString());
        } else if (properties.containsKey("ThreadGroup.comments")) {
            node.setComments(properties.get("ThreadGroup.comments").toString());
        } else if (properties.containsKey("HTTPSampler.comments")) {
            node.setComments(properties.get("HTTPSampler.comments").toString());
        }
        
        return node;
    }
    
    /**
     * Parse JavaSampler arguments string to list of argument maps
     * Format: name\nvalue\n=\n (repeated for each argument)
     * The format appears to be: whitespace, name, value, "=", whitespace (repeated)
     */
    private List<Map<String, String>> parseJavaSamplerArguments(String argumentsStr) {
        List<Map<String, String>> argsList = new ArrayList<>();
        if (argumentsStr == null || argumentsStr.trim().isEmpty()) {
            return argsList;
        }
        
        // Split by lines
        String[] lines = argumentsStr.split("\n");
        String currentName = null;
        StringBuilder currentValue = new StringBuilder();
        boolean expectingValue = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // If we see "=", it's a separator - save current argument and reset
            if (trimmed.equals("=")) {
                if (currentName != null && !currentName.isEmpty()) {
                    Map<String, String> arg = new HashMap<>();
                    arg.put("name", currentName);
                    arg.put("value", currentValue.toString().trim());
                    arg.put("description", "");
                    argsList.add(arg);
                }
                currentName = null;
                currentValue.setLength(0);
                expectingValue = false;
                continue;
            }
            
            // If we don't have a name yet, this is the name
            if (currentName == null) {
                currentName = trimmed;
                expectingValue = true;
            } else if (expectingValue) {
                // This is the value (could be multi-line, especially for JSON)
                if (currentValue.length() > 0) {
                    currentValue.append("\n");
                }
                currentValue.append(trimmed);
            }
        }
        
        // Handle last argument if there's no trailing "="
        if (currentName != null && !currentName.isEmpty()) {
            Map<String, String> arg = new HashMap<>();
            arg.put("name", currentName);
            arg.put("value", currentValue.toString().trim());
            arg.put("description", "");
            argsList.add(arg);
        }
        
        return argsList;
    }
    
    /**
     * Recursively parse a test element and its children
     */
    private JmxTreeNodeDTO parseTestElement(TestElement element, HashTree tree) {
        JmxTreeNodeDTO node = new JmxTreeNodeDTO();
        node.setId(UUID.randomUUID().toString());
        node.setName(element.getName());
        node.setEnabled(element.isEnabled());
        node.setComments(element.getComment());
        
        // Set GUI and test class
        String guiClass = element.getPropertyAsString(TestElement.GUI_CLASS);
        String testClass = element.getPropertyAsString(TestElement.TEST_CLASS);
        node.setGuiClass(guiClass);
        node.setTestClass(testClass);
        
        // Determine element type and extract properties
        Map<String, Object> properties = new HashMap<>();
        
        if (element instanceof TestPlan) {
            node.setType("TestPlan");
            TestPlan testPlan = (TestPlan) element;
            properties.put("functionalMode", testPlan.isFunctionalMode());
            properties.put("serializeThreadGroups", testPlan.isSerialized());
            properties.put("tearDownOnShutdown", testPlan.isTearDownOnShutdown());
            
        } else if (element instanceof ThreadGroup) {
            node.setType("ThreadGroup");
            ThreadGroup threadGroup = (ThreadGroup) element;
            properties.put("numThreads", threadGroup.getNumThreads());
            properties.put("rampTime", threadGroup.getRampUp());
            properties.put("scheduler", threadGroup.getScheduler());
            properties.put("duration", threadGroup.getDuration());
            properties.put("delay", threadGroup.getDelay());
            properties.put("continueForever", element.getPropertyAsBoolean("ThreadGroup.continue_forever"));
            properties.put("sameUserOnNextIteration", element.getPropertyAsBoolean("ThreadGroup.same_user_on_next_iteration"));
            
            // Loop controller settings
            if (threadGroup.getSamplerController() instanceof LoopController) {
                LoopController loopController = (LoopController) threadGroup.getSamplerController();
                properties.put("loops", loopController.getLoops());
            }
            
        } else if (element instanceof HTTPSampler) {
            node.setType("HTTPSampler");
            HTTPSampler httpSampler = (HTTPSampler) element;
            properties.put("protocol", httpSampler.getProtocol());
            properties.put("domain", httpSampler.getDomain());
            properties.put("port", httpSampler.getPort());
            properties.put("path", httpSampler.getPath());
            properties.put("method", httpSampler.getMethod());
            properties.put("contentEncoding", httpSampler.getContentEncoding());
            properties.put("followRedirects", httpSampler.getFollowRedirects());
            properties.put("autoRedirects", httpSampler.getAutoRedirects());
            properties.put("useKeepAlive", httpSampler.getUseKeepAlive());
            properties.put("connectTimeout", httpSampler.getConnectTimeout());
            properties.put("responseTimeout", httpSampler.getResponseTimeout());
            
            // Body data or parameters
            Arguments arguments = httpSampler.getArguments();
            if (arguments != null) {
                List<Map<String, String>> params = new ArrayList<>();
                for (int i = 0; i < arguments.getArgumentCount(); i++) {
                    Map<String, String> param = new HashMap<>();
                    param.put("key", arguments.getArgument(i).getName());
                    param.put("value", arguments.getArgument(i).getValue());
                    param.put("description", arguments.getArgument(i).getDescription());
                    params.add(param);
                }
                properties.put("parameters", params);
            }
            
            // Check for POST body
            if (arguments != null && httpSampler.getPostBodyRaw() && arguments.getArgumentCount() > 0) {
                properties.put("bodyData", arguments.getArgument(0).getValue());
            }
            
        } else if (element instanceof JavaSampler) {
            node.setType("JavaSampler");
            JavaSampler javaSampler = (JavaSampler) element;
            properties.put("classname", javaSampler.getClassname());
            
            Arguments arguments = javaSampler.getArguments();
            if (arguments != null) {
                List<Map<String, String>> args = new ArrayList<>();
                for (int i = 0; i < arguments.getArgumentCount(); i++) {
                    Map<String, String> arg = new HashMap<>();
                    arg.put("name", arguments.getArgument(i).getName());
                    arg.put("value", arguments.getArgument(i).getValue());
                    arg.put("description", arguments.getArgument(i).getDescription());
                    args.add(arg);
                }
                properties.put("arguments", args);
            }
            
        } else if (element instanceof CSVDataSet) {
            node.setType("CSVDataSet");
            CSVDataSet csvDataSet = (CSVDataSet) element;
            properties.put("filename", csvDataSet.getFilename());
            properties.put("fileEncoding", csvDataSet.getFileEncoding());
            properties.put("variableNames", csvDataSet.getVariableNames());
            properties.put("ignoreFirstLine", csvDataSet.getPropertyAsBoolean("ignoreFirstLine"));
            properties.put("delimiter", csvDataSet.getDelimiter());
            properties.put("recycle", csvDataSet.getRecycle());
            properties.put("stopThread", csvDataSet.getStopThread());
            properties.put("shareMode", csvDataSet.getShareMode());
            
        } else if (element instanceof HeaderManager) {
            node.setType("HeaderManager");
            HeaderManager headerManager = (HeaderManager) element;
            List<Map<String, String>> headers = new ArrayList<>();
            for (int i = 0; i < headerManager.size(); i++) {
                Header header = headerManager.get(i);
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("key", header.getName());
                headerMap.put("value", header.getValue());
                headers.add(headerMap);
            }
            properties.put("headers", headers);
            
        } else if (element instanceof ResultCollector) {
            node.setType("ResultCollector");
            ResultCollector resultCollector = (ResultCollector) element;
            properties.put("filename", resultCollector.getFilename());
            
        } else {
            // Generic element
            node.setType(element.getClass().getSimpleName());
        }
        
        node.setProperties(properties);
        
        // Parse children recursively
        HashTree subTree = tree.getTree(element);
        if (subTree != null) {
            List<JmxTreeNodeDTO> children = new ArrayList<>();
            for (Object child : subTree.list()) {
                if (child instanceof TestElement) {
                    children.add(parseTestElement((TestElement) child, subTree));
                }
            }
            node.setChildren(children);
        }
        
        return node;
    }
    
    @Override
    public void jsonToJmx(JmxTreeNodeDTO structure, File outputFile) throws Exception {
        log.info("Generating JMX file from structure: {}", outputFile.getAbsolutePath());
        
        // Ensure JMeter is initialized
        if (!jmeterInitialized) {
            log.info("JMeter not initialized, initializing now...");
            initJMeter();
        }
        
        // Create root HashTree
        HashTree testPlanTree = new HashTree();
        
        // Build the tree from structure
        TestElement rootElement = buildTestElement(structure);
        testPlanTree.add(rootElement);
        buildHashTree(structure, rootElement, testPlanTree);
        
        // Save to file
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            SaveService.saveTree(testPlanTree, outputStream);
        }
        
        log.info("JMX file generated successfully");
    }
    
    /**
     * Build TestElement from JmxTreeNodeDTO
     */
    @SuppressWarnings("unchecked")
    private TestElement buildTestElement(JmxTreeNodeDTO node) {
        applyDefaultClassMetadata(node);
        
        TestElement element = null;
        Map<String, Object> props = node.getProperties();
        
        switch (node.getType()) {
            case "TestPlan":
                TestPlan testPlan = new TestPlan();
                // Use getBooleanValue to safely handle String/Boolean conversion
                testPlan.setFunctionalMode(getBooleanValue(props, "functionalMode", false));
                testPlan.setSerialized(getBooleanValue(props, "serializeThreadGroups", false));
                testPlan.setTearDownOnShutdown(getBooleanValue(props, "tearDownOnShutdown", true));
                element = testPlan;
                break;
                
            case "ThreadGroup":
                ThreadGroup threadGroup = new ThreadGroup();
                
                int numThreads = getIntValue(props, "numThreads", 1);
                threadGroup.setNumThreads(numThreads);
                
                int rampTime = getIntValue(props, "rampTime", 1);
                threadGroup.setRampUp(rampTime);
                
                boolean scheduler = getBooleanValue(props, "scheduler", false);
                threadGroup.setScheduler(scheduler);
                
                long duration = getLongValue(props, "duration", 0L);
                threadGroup.setDuration(duration);
                
                long delay = getLongValue(props, "delay", 0L);
                threadGroup.setDelay(delay);
                
                // Loop controller
                LoopController loopController = new LoopController();
                int loops = getIntValue(props, "loops", 1);
                loopController.setLoops(loops);
                loopController.setFirst(true);
                loopController.initialize();
                threadGroup.setSamplerController(loopController);
                
                element = threadGroup;
                break;
                
            case "HTTPSampler":
            case "HTTPSamplerProxy":
                HTTPSampler httpSampler = new HTTPSampler();
                
                // Protocol
                String protocol = getStringValueWithFallback(props, "http", "protocol", "HTTPSampler.protocol");
                httpSampler.setProtocol(protocol);
                
                // Domain
                String domain = getStringValueWithFallback(props, "", "domain", "HTTPSampler.domain");
                httpSampler.setDomain(domain);
                
                // Port - handle both Integer and String
                int port = getIntValueWithFallback(props, 0, "port", "HTTPSampler.port");
                httpSampler.setPort(port);
                
                // Path
                String path = getStringValueWithFallback(props, "", "path", "HTTPSampler.path");
                httpSampler.setPath(path);
                
                // Method
                String method = getStringValueWithFallback(props, "GET", "method", "HTTPSampler.method");
                httpSampler.setMethod(method);
                
                // Content Encoding
                String encoding = getStringValueWithFallback(props, "UTF-8", "contentEncoding", "HTTPSampler.contentEncoding");
                httpSampler.setContentEncoding(encoding);
                
                // Boolean flags - handle both Boolean and String
                boolean followRedirects = getBooleanValueWithFallback(props, true, "followRedirects", "HTTPSampler.follow_redirects");
                httpSampler.setFollowRedirects(followRedirects);
                
                boolean autoRedirects = getBooleanValueWithFallback(props, false, "autoRedirects", "HTTPSampler.auto_redirects");
                httpSampler.setAutoRedirects(autoRedirects);
                
                boolean useKeepAlive = getBooleanValueWithFallback(props, true, "useKeepAlive", "HTTPSampler.use_keepalive");
                httpSampler.setUseKeepAlive(useKeepAlive);
                
                // Timeouts
                int connectTimeout = getIntValueWithFallback(props, -1, "connectTimeout", "HTTPSampler.connect_timeout");
                if (connectTimeout >= 0) {
                    httpSampler.setConnectTimeout(String.valueOf(connectTimeout));
                }
                int responseTimeout = getIntValueWithFallback(props, -1, "responseTimeout", "HTTPSampler.response_timeout");
                if (responseTimeout >= 0) {
                    httpSampler.setResponseTimeout(String.valueOf(responseTimeout));
                }
                
                // Parameters
                Arguments arguments = new Arguments();
                Object parameters = getFirstNonNull(props, "parameters", "HTTPSampler.Arguments");
                if (parameters instanceof List) {
                    List<Map<String, Object>> params = (List<Map<String, Object>>) parameters;
                    for (Map<String, Object> param : params) {
                        String key = getStringValue(param, "key", "");
                        String value = getStringValue(param, "value", "");
                        if (!key.isEmpty()) {
                            HTTPArgument httpArgument = new HTTPArgument(key, value);
                            httpArgument.setAlwaysEncoded(false);
                            httpArgument.setMetaData("=");
                            arguments.addArgument(httpArgument);
                        }
                    }
                }
                
                // Body data
                Object bodyDataObj = getFirstNonNull(props, "bodyData", "HTTPSampler.postBodyRaw");
                if (bodyDataObj != null) {
                    String bodyData = bodyDataObj.toString();
                    if (!bodyData.isEmpty()) {
                        arguments = new Arguments();
                        HTTPArgument bodyArgument = new HTTPArgument();
                        bodyArgument.setAlwaysEncoded(false);
                        bodyArgument.setMetaData("=");
                        bodyArgument.setName("");
                        bodyArgument.setValue(bodyData);
                        arguments.addArgument(bodyArgument);
                        httpSampler.setPostBodyRaw(true);
                    }
                }
                
                httpSampler.setArguments(arguments);
                element = httpSampler;
                break;
                
            case "JavaSampler":
                JavaSampler javaSampler = new JavaSampler();
                javaSampler.setClassname((String) props.get("classname"));
                
                Arguments javaArgs = new Arguments();
                if (props.containsKey("arguments")) {
                    Object argumentsObj = props.get("arguments");
                    List<Map<String, String>> args = null;
                    
                    // Handle both string and list formats
                    if (argumentsObj instanceof String) {
                        // Convert string format to list
                        args = parseJavaSamplerArguments((String) argumentsObj);
                    } else if (argumentsObj instanceof List) {
                        // Already in list format
                        args = (List<Map<String, String>>) argumentsObj;
                    }
                    
                    if (args != null) {
                        for (Map<String, String> arg : args) {
                            String name = arg.get("name");
                            String value = arg.get("value");
                            if (name != null) {
                                javaArgs.addArgument(name, value != null ? value : "");
                            }
                        }
                    }
                }
                javaSampler.setArguments(javaArgs);
                
                element = javaSampler;
                break;
                
            case "CSVDataSet":
                CSVDataSet csvDataSet = new CSVDataSet();
                csvDataSet.setFilename((String) props.get("filename"));
                csvDataSet.setFileEncoding((String) props.getOrDefault("fileEncoding", "UTF-8"));
                csvDataSet.setVariableNames((String) props.get("variableNames"));
                // Use getBooleanValue to safely handle String/Boolean conversion
                csvDataSet.setIgnoreFirstLine(getBooleanValue(props, "ignoreFirstLine", false));
                csvDataSet.setDelimiter((String) props.getOrDefault("delimiter", ","));
                csvDataSet.setRecycle(getBooleanValue(props, "recycle", true));
                csvDataSet.setStopThread(getBooleanValue(props, "stopThread", false));
                csvDataSet.setShareMode((String) props.getOrDefault("shareMode", "shareMode.all"));
                
                element = csvDataSet;
                break;
                
            case "HeaderManager":
                HeaderManager headerManager = new HeaderManager();
                if (props.containsKey("headers")) {
                    List<Map<String, String>> headers = (List<Map<String, String>>) props.get("headers");
                    for (Map<String, String> header : headers) {
                        headerManager.add(new Header(header.get("key"), header.get("value")));
                    }
                }
                element = headerManager;
                break;
                
            case "ResultCollector":
                try {
                    // Ensure JMeter is initialized before creating ResultCollector
                    if (!jmeterInitialized) {
                        initJMeter();
                    }
                    ResultCollector resultCollector = new ResultCollector();
                    if (props.containsKey("filename")) {
                        resultCollector.setFilename((String) props.get("filename"));
                    }
                    element = resultCollector;
                } catch (Exception e) {
                    log.warn("Failed to create ResultCollector (JMeter not fully initialized), skipping: {}", e.getMessage());
                    // Skip ResultCollector if JMeter initialization fails
                    // Return a placeholder that won't cause errors
                    element = new TestPlan(); // Use TestPlan as safe fallback
                    element.setName(node.getName());
                    element.setEnabled(false); // Disable it so it won't affect tests
                }
                break;
                
            default:
                log.warn("Unknown element type: {}, creating generic element", node.getType());
                element = new TestPlan(); // Fallback
                break;
        }
        
        // Set common properties
        element.setName(node.getName());
        element.setEnabled(node.getEnabled());
        if (node.getComments() != null) {
            element.setComment(node.getComments());
        }
        
        // Set GUI and test class if provided
        if (node.getGuiClass() != null) {
            element.setProperty(TestElement.GUI_CLASS, node.getGuiClass());
        }
        if (node.getTestClass() != null) {
            element.setProperty(TestElement.TEST_CLASS, node.getTestClass());
        }
        
        return element;
    }
    
    /**
     * Recursively build HashTree from structure
     */
    private void buildHashTree(JmxTreeNodeDTO node, TestElement parentElement, HashTree parentTree) {
        HashTree subTree = parentTree.getTree(parentElement);
        
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (JmxTreeNodeDTO child : node.getChildren()) {
                TestElement childElement = buildTestElement(child);
                subTree.add(childElement);
                buildHashTree(child, childElement, subTree);
            }
        }
    }
    
    @Override
    public boolean validateStructure(JmxTreeNodeDTO structure) {
        if (structure == null) {
            log.warn("Structure is null");
            return false;
        }
        
        if (!"TestPlan".equals(structure.getType())) {
            log.warn("Root element must be TestPlan, got: {}", structure.getType());
            return false;
        }
        
        if (structure.getName() == null || structure.getName().trim().isEmpty()) {
            log.warn("Test plan name cannot be empty");
            return false;
        }
        
        // Validate children recursively
        return validateNode(structure);
    }
    
    private boolean validateNode(JmxTreeNodeDTO node) {
        if (node.getName() == null || node.getName().trim().isEmpty()) {
            log.warn("Node name cannot be empty for type: {}", node.getType());
            return false;
        }
        
        // Validate specific element types
        switch (node.getType()) {
            case "HTTPSampler":
                if (!node.getProperties().containsKey("domain") || 
                    node.getProperties().get("domain") == null) {
                    log.warn("HTTP Sampler must have a domain");
                    return false;
                }
                break;
                
            case "JavaSampler":
                if (!node.getProperties().containsKey("classname") || 
                    node.getProperties().get("classname") == null) {
                    log.warn("Java Sampler must have a classname");
                    return false;
                }
                break;
                
            case "CSVDataSet":
                if (!node.getProperties().containsKey("filename") || 
                    node.getProperties().get("filename") == null) {
                    log.warn("CSV DataSet must have a filename");
                    return false;
                }
                break;
        }
        
        // Validate children
        if (node.getChildren() != null) {
            for (JmxTreeNodeDTO child : node.getChildren()) {
                if (!validateNode(child)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public JmxTreeNodeDTO createEmptyTestPlan(String name) {
        JmxTreeNodeDTO testPlan = new JmxTreeNodeDTO();
        testPlan.setId(UUID.randomUUID().toString());
        testPlan.setType("TestPlan");
        testPlan.setName(name);
        testPlan.setEnabled(true);
        testPlan.setGuiClass("TestPlanGui");
        testPlan.setTestClass("TestPlan");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("functionalMode", false);
        properties.put("serializeThreadGroups", false);
        properties.put("tearDownOnShutdown", true);
        testPlan.setProperties(properties);
        
        testPlan.setChildren(new ArrayList<>());
        
        return testPlan;
    }
    
    /**
     * Helper method to safely get String value from map
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value.toString();
    }
    
    private String getStringValueWithFallback(Map<String, Object> map, String defaultValue, String... keys) {
        if (map == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key).toString();
            }
        }
        return defaultValue;
    }
    
    /**
     * Helper method to safely get int value from map
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse int value for key {}: {}", key, value);
            return defaultValue;
        }
    }
    
    private int getIntValueWithFallback(Map<String, Object> map, int defaultValue, String... keys) {
        if (map == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (!map.containsKey(key) || map.get(key) == null) {
                continue;
            }
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse int value for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Helper method to safely get boolean value from map
     */
    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value);
        }
        return defaultValue;
    }
    
    private boolean getBooleanValueWithFallback(Map<String, Object> map, boolean defaultValue, String... keys) {
        if (map == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (!map.containsKey(key) || map.get(key) == null) {
                continue;
            }
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof String) {
                return "true".equalsIgnoreCase((String) value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Helper method to safely get long value from map
     */
    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value for key {}: {}", key, value);
            return defaultValue;
        }
    }
    
    private Object getFirstNonNull(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }
    
    private void applyDefaultClassMetadata(JmxTreeNodeDTO node) {
        if (node == null || node.getType() == null) {
            return;
        }
        ClassMetadata metadata = DEFAULT_CLASS_METADATA.get(node.getType());
        if (metadata == null) {
            return;
        }
        if (isBlank(node.getGuiClass())) {
            node.setGuiClass(metadata.getGuiClass());
        }
        if (isBlank(node.getTestClass())) {
            node.setTestClass(metadata.getTestClass());
        }
    }
    
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    private static class ClassMetadata {
        private final String guiClass;
        private final String testClass;
        
        ClassMetadata(String guiClass, String testClass) {
            this.guiClass = guiClass;
            this.testClass = testClass;
        }
        
        String getGuiClass() {
            return guiClass;
        }
        
        String getTestClass() {
            return testClass;
        }
    }
}

