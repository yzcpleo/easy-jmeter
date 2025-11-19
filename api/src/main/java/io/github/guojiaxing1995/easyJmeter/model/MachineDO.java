package io.github.guojiaxing1995.easyJmeter.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "machine")
@EqualsAndHashCode(callSuper = true)
public class MachineDO extends BaseModel implements Serializable {

    private static final long serialVersionUID = -6928506417680888594L;

    private String name;

    private String address;

    private String path;

    private String version;

    private Boolean isOnline;

    @TableField(value = "`jmeter_status`", jdbcType = JdbcType.INTEGER)
    private Integer jmeterStatus;

    @JsonIgnore
    @TableField(value = "`client_id`")
    private String clientId;

}
