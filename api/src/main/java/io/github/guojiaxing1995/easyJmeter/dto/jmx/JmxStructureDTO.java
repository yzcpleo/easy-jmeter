package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * DTO for saving/retrieving JMX structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxStructureDTO {
    
    /**
     * Case ID
     */
    @JsonProperty("caseId")  // Explicitly map JSON field name
    @NotNull(message = "Case ID cannot be null")
    private Integer caseId;
    
    /**
     * JMX tree structure (root node)
     */
    @JsonProperty("structure")  // Explicitly map JSON field name
    @NotNull(message = "Structure cannot be null")
    private JmxTreeNodeDTO structure;
    
    /**
     * Version number
     */
    @JsonProperty("version")  // Explicitly map JSON field name
    private Integer version;
}

