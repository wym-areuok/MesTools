package com.mes.system.service;

import com.mes.system.domain.ApiManageHistory;
import com.mes.system.domain.ApiManageItem;
import com.mes.system.domain.dto.ProxyRequestDto;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理
 */
public interface IApiManageService {
    List<ApiManageItem> selectApiTree();

    ApiManageItem selectApiManageItemById(Long itemId);

    int insertApiManageItem(ApiManageItem apiManageItem);

    int updateApiManageItem(ApiManageItem apiManageItem);

    int deleteApiManageItemById(Long itemId);

    List<ApiManageHistory> selectHistoryList(ApiManageHistory history);

    int insertHistory(ApiManageHistory history);

    // 代理请求
    Map<String, Object> proxyRequest(ProxyRequestDto proxyRequest);

    int toggleLock(Long itemId, Integer isLocked);
}
