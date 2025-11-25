package io.github.guojiaxing1995.easyJmeter.dto.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * DTO for copying an existing JMX asset with a new name
 */
@Data
@NoArgsConstructor
public class CopyJmxAssetDTO {

    @NotBlank(message = "{jmx.asset.copy.name.not-blank}")
    @Length(max = 100, message = "{jmx.asset.copy.name.length}")
    private String name;
}


