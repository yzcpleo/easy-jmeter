package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * DTO for creating or updating a reusable JMX asset
 */
@Data
@NoArgsConstructor
public class CreateOrUpdateJmxAssetDTO {

    @NotBlank(message = "{jmx.asset.name.not-blank}")
    @Length(max = 100, message = "{jmx.asset.name.length}")
    private String name;

    @NotNull(message = "{jmx.asset.project.not-null}")
    private Integer projectId;

    @Length(max = 500, message = "{jmx.asset.description.length}")
    private String description;

    /**
     * JFile id referencing the generated/uploaded JMX (optional in builder mode until generated)
     */
    private Integer jmxFileId;

    /**
     * UPLOAD or BUILDER, defaults handled in service
     */
    private String creationMode;
}


