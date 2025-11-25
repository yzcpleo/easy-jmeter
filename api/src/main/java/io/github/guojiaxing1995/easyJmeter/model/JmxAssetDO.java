package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Persistent model for reusable JMX assets
 */
@Data
@TableName("jmx_asset")
@EqualsAndHashCode(callSuper = true)
public class JmxAssetDO extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private Integer projectId;

    private String description;

    /**
     * JFile id that stores the generated JMX file
     */
    private Integer jmxFileId;

    private Integer creator;

    private String creationMode;

    private Integer latestStructureVersion;
}


