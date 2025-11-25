package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * JMeter Java Request Sampler configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxJavaSamplerDTO {
    
    /**
     * Java Request name
     */
    private String name;
    
    /**
     * Java class name (fully qualified)
     */
    private String classname;
    
    /**
     * Arguments passed to the Java sampler
     */
    private List<Argument> arguments = new ArrayList<>();
    
    /**
     * Comments
     */
    private String comments;
    
    /**
     * Java sampler argument
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Argument {
        private String name;
        private String value;
        private String description;
    }
}

