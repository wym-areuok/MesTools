package com.mes.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-05
 * @Description: 跳站业务相关DTO
 */
@Data
@ApiModel(value = "JumpStationDTO", description = "板卡跳站参数")
public class JumpStationDTO {

    @ApiModelProperty(value = "SN列表", required = true)
    private List<String> snList;

    @ApiModelProperty(value = "数据源", required = true, example = "LOCALHOST")
    private String dbDataSource;

    @ApiModelProperty(value = "跳站类型", required = true, example = "PCA")
    private String jumpType;

    @ApiModelProperty(value = "目标站点", example = "00")
    private String station;

    @ApiModelProperty(value = "备注", example = "测试跳站")
    private String remark;
}
