package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JMeter Thread Group configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxThreadGroupDTO {
    
    /**
     * Thread group name
     */
    private String name;
    
    /**
     * Number of threads (users)
     */
    private Integer numThreads = 1;
    
    /**
     * Ramp-up period (seconds)
     */
    private Integer rampTime = 1;
    
    /**
     * Loop count (-1 for infinite)
     */
    private Integer loops = 1;
    
    /**
     * Duration in seconds (0 = disabled)
     */
    private Long duration = 0L;
    
    /**
     * Startup delay in seconds
     */
    private Long delay = 0L;
    
    /**
     * Use scheduler
     */
    private Boolean scheduler = false;
    
    /**
     * Continue on sampler error
     */
    private Boolean continueForever = false;
    
    /**
     * Same user on each iteration
     */
    private Boolean sameUserOnNextIteration = true;
    
    /**
     * Comments
     */
    private String comments;
}

