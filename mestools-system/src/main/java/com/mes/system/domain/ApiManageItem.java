package com.mes.system.domain;

import com.mes.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiManageItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Long parentId;
    private String itemName;
    private String itemType; // 'group', 'api', 'env'
    private String itemKey;
    private String reqMethod;
    private String reqUrl;

    // JSON 存储字段
    private String reqParams;
    private String reqHeaders;
    private String reqPathParams;
    private String reqBodyType;
    private String reqBodyJson;
    private String reqFormData;

    private String authType;
    private String authToken;
    private String responseDef;
    private Integer sortOrder;

    // 子节点 (非数据库字段)
    private List<ApiManageItem> children = new ArrayList<>();
}
