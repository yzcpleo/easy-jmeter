package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JMeter CSV Data Set Config
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxCsvDataSetDTO {
    
    /**
     * CSV Data Set name
     */
    private String name;
    
    /**
     * CSV file path
     */
    private String filename;
    
    /**
     * File encoding
     */
    private String fileEncoding = "UTF-8";
    
    /**
     * Variable names (comma-separated)
     */
    private String variableNames;
    
    /**
     * Ignore first line (headers)
     */
    private Boolean ignoreFirstLine = false;
    
    /**
     * Delimiter (default: comma)
     */
    private String delimiter = ",";
    
    /**
     * Recycle on EOF
     */
    private Boolean recycle = true;
    
    /**
     * Stop thread on EOF
     */
    private Boolean stopThread = false;
    
    /**
     * Sharing mode: All threads, Current thread group, Current thread, Edit
     */
    private String shareMode = "shareMode.all";
    
    /**
     * Comments
     */
    private String comments;
}

