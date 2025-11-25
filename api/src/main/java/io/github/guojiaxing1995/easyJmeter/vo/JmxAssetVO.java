package io.github.guojiaxing1995.easyJmeter.vo;

import io.github.guojiaxing1995.easyJmeter.model.JmxAssetDO;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * View object for JMX asset responses
 */
@Data
@NoArgsConstructor
public class JmxAssetVO {

    private Integer id;
    private String name;
    private Integer projectId;
    private String description;
    private Integer jmxFileId;
    private String creationMode;
    private Integer latestStructureVersion;
    private JFileVO jmxFile;

    public JmxAssetVO(JmxAssetDO asset) {
        this.id = asset.getId();
        this.name = asset.getName();
        this.projectId = asset.getProjectId();
        this.description = asset.getDescription();
        this.jmxFileId = asset.getJmxFileId();
        this.creationMode = asset.getCreationMode();
        this.latestStructureVersion = asset.getLatestStructureVersion();
    }
}


