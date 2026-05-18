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
 * (采用单表设计，通过 item_type 区分目录与接口)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiManageItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long itemId;

    /**
     * 父节点ID (用于构建树形结构)
     */
    private Long parentId;

    /**
     * 名称 (接口名称 / 分组名称)
     */
    private String itemName;

    /**
     * 协议类型: 'http', 'webservice'
     */
    private String protocol;

    /**
     * 类型: 'group'(分组), 'api'(接口)
     */
    private String itemType;

    /**
     * 请求方式 (GET, POST, etc.)
     */
    private String reqMethod;

    /**
     * 请求地址 (API 接口路径)
     */
    private String reqUrl;

    /**
     * Query Params (JSON 字符串)
     */
    private String reqParams;

    /**
     * Headers (JSON 字符串)
     */
    private String reqHeaders;

    /**
     * Path Variables (JSON 字符串)
     */
    private String reqPathParams;

    /**
     * 请求体类型 (json, form, etc.)
     */
    private String reqBodyType;

    /**
     * 请求体内容 (JSON 字符串)
     */
    private String reqBodyJson;

    /**
     * FormData (JSON 字符串)
     */
    private String reqFormData;

    /**
     * 响应定义 (JSON 字符串)
     */
    private String responseDef;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否锁定 (1:是, 0:否)
     */
    private Integer isLocked;

    /**
     * 子节点 (非数据库字段, 用于构建树)
     */
    private List<ApiManageItem> children = new ArrayList<>();
}
