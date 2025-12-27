package com.mes.system.domain.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 用于接收前端代理请求的 DTO
 */
@Data
public class ProxyRequestDto {
    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 请求参数
     */
    private Map<String, String> params;

    /**
     * 请求体 (可以是 String (JSON) 或 Map (FormData))
     */
    private Object body;

    /**
     * 请求体类型
     */
    private String bodyType;

    /**
     * 快照JSON
     */
    private String snapshotJson;
}
