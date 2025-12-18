package com.mes.system.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-05
 * @Description: 跳站业务相关DTO
 */
@Data
public class JumpStationDTO {
    /*SN列表*/
    private List<String> snList;
    /*数据源*/
    private String dbDataSource;
    /*站点*/
    private String station;
    /*跳站类型*/
    private String jumpType;
    /*跳站原因*/
    private String remark;
}