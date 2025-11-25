package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * JMX structure model
 * Stores parsed JMX structure in JSON format
 */
@Data
@TableName("jmx_structure")
@EqualsAndHashCode(callSuper = true)
public class JmxStructureDO extends BaseModel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Associated case ID
     */
    private Integer caseId;
    
    /**
     * JMX structure in JSON format (stored as TEXT/LONGTEXT)
     */
    private String structureJson;
    
    /**
     * Version number for tracking changes
     */
    private Integer version = 1;
}

