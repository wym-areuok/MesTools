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

    /**
     * 历史记录ID
     */
    private Long historyId;

    /**
     * 关联的接口项ID
     */
    private Long itemId;

    /**
     * 请求方式
     */
    private String reqMethod;

    /**
     * 完整的请求URL (包含Query参数)
     */
    private String reqUrl;

    /**
     * 响应状态码
     */
    private Integer resStatus;

    /**
     * 请求耗时 (毫秒)
     */
    private Integer duration;

    /**
     * 请求时的完整UI快照 (JSON字符串)
     */
    private String snapshotJson;
}
