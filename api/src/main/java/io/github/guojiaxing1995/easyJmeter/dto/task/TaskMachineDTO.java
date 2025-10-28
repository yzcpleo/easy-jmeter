package io.github.guojiaxing1995.easyJmeter.dto.task;

import io.github.guojiaxing1995.easyJmeter.model.TaskDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMachineDTO {

    private TaskDO taskDO;

    private String machineIp;

    private Boolean result;

    private Integer status;

}
