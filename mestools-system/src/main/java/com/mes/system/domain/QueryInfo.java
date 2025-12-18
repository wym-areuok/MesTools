package com.mes.system.domain;

import com.mes.common.annotation.Excel;
import com.mes.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 资料查询的实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Integer infoId;

    @Excel(name = "资料标题")
    private String infoTitle;

    @Excel(name = "资料标签")
    private String infoTags;

    @Excel(name = "资料类型", dictType = "info_type")
    private String infoType;

    @Excel(name = "资料内容")
    private String infoContent;

    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    @Excel(name = "搜索次数")
    private Integer searchCount;
}