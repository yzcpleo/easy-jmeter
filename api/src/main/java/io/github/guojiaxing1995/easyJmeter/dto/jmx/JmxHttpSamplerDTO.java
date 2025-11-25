package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * JMeter HTTP Request Sampler configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxHttpSamplerDTO {
    
    /**
     * HTTP Request name
     */
    private String name;
    
    /**
     * Protocol (http, https)
     */
    private String protocol = "http";
    
    /**
     * Server domain or IP
     */
    private String domain;
    
    /**
     * Port number
     */
    private Integer port;
    
    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    private String method = "GET";
    
    /**
     * Path (without domain)
     */
    private String path;
    
    /**
     * Content encoding
     */
    private String contentEncoding = "UTF-8";
    
    /**
     * Request body data
     */
    private String bodyData;
    
    /**
     * HTTP Headers
     */
    private List<KeyValuePair> headers = new ArrayList<>();
    
    /**
     * Query parameters or form data
     */
    private List<KeyValuePair> parameters = new ArrayList<>();
    
    /**
     * Follow redirects
     */
    private Boolean followRedirects = true;
    
    /**
     * Auto redirects
     */
    private Boolean autoRedirects = false;
    
    /**
     * Use KeepAlive
     */
    private Boolean useKeepAlive = true;
    
    /**
     * Use multipart/form-data for POST
     */
    private Boolean doMultipartPost = false;
    
    /**
     * Connection timeout (milliseconds)
     */
    private Integer connectTimeout;
    
    /**
     * Response timeout (milliseconds)
     */
    private Integer responseTimeout;
    
    /**
     * Comments
     */
    private String comments;
    
    /**
     * Key-Value pair for headers and parameters
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyValuePair {
        private String key;
        private String value;
        private String description;
    }
}

