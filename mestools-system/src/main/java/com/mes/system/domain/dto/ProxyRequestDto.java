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
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> params;
    // 可以是 String (JSON) 或 Map (FormData)
    private Object body;
    private String bodyType;
}
