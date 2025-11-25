package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmxAssetStructureDTO {

    @JsonProperty("assetId")
    @NotNull(message = "Asset ID cannot be null")
    private Integer assetId;

    @JsonProperty("structure")
    @NotNull(message = "Structure cannot be null")
    private JmxTreeNodeDTO structure;

    @JsonProperty("version")
    private Integer version;
}


