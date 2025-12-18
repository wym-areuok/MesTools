package com.mes.system.domain.dto;

import com.mes.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 执行SQL的DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecuteSqlDTO extends BaseEntity {
    /**
     * 数据源
     */
    private String dbDataSource;

    /**
     * SQL内容
     */
    private String sqlContent;
}