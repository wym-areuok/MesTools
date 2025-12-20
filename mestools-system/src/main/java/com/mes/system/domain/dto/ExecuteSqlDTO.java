package com.mes.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 执行SQL的DTO
 */
@Data
@ApiModel(value = "ExecuteSqlDTO", description = "SQL执行参数")
public class ExecuteSqlDTO {

    @ApiModelProperty(value = "数据源", required = true, example = "LOCALHOST")
    private String dbDataSource;

    @ApiModelProperty(value = "SQL语句内容", required = true, example = "没有例子自己写")
    private String sqlContent;
}
