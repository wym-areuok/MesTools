package com.mes.system.domain;

import com.mes.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口请求操作历史记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiManageHistory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long historyId;
    private Long itemId;
    private String reqMethod;
    private String reqUrl;
    private Integer resStatus;
    private Integer duration;
    private String snapshotJson;
}
