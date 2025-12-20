package com.mes.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-07
 * @Description: 修改FIS账号密码DTO
 */
@Data
@ApiModel(value = "ChangePwdDTO", description = "修改密码参数")
public class ChangePwdDTO {

    @ApiModelProperty(value = "FIS账号", example = "2550091")
    private String fisNumber;

    @ApiModelProperty(value = "新密码", required = true, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "数据源", required = true, example = "LOCALHOST")
    @NotBlank(message = "数据源不能为空")
    private String dbDataSource;
}

