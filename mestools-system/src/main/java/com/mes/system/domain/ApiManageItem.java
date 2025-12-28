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
 * (采用单表多态设计，通过 item_type 区分不同类型的实体)
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
     * 【复用】名称 (接口名称 / 环境名称 / 分组名称)
     */
    private String itemName;

    /**
     * 【核心】类型: 'group'(分组), 'api'(接口), 'env'(环境)
     */
    private String itemType;

    /**
     * 【复用】唯一标识 (环境Key, 如 'dev')
     */
    private String itemKey;

    /**
     * 请求方式 (GET, POST, etc.)
     */
    private String reqMethod;

    /**
     * 【复用】请求地址 (API: 接口路径; Env: 基础URL)
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
     * 【复用】请求体内容 (API: Body内容; Env: 环境变量JSON)
     */
    private String reqBodyJson;

    /**
     * FormData (JSON 字符串)
     */
    private String reqFormData;

    /**
     * 鉴权类型 (none, bearer, etc.)
     */
    private String authType;

    /**
     * 鉴权Token
     */
    private String authToken;

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
