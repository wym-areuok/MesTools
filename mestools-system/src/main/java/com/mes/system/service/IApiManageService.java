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

    List<ApiManageItem> selectEnvList();

    ApiManageItem selectApiManageItemById(Long itemId);

    int insertApiManageItem(ApiManageItem apiManageItem);

    int updateApiManageItem(ApiManageItem apiManageItem);

    int deleteApiManageItemById(Long itemId);

    void saveEnvList(List<ApiManageItem> envList);

    List<ApiManageHistory> selectHistoryList(ApiManageHistory history);

    int insertHistory(ApiManageHistory history);

    // 代理请求
    Map<String, Object> proxyRequest(ProxyRequestDto proxyRequest);

    Map<String, Object> exportData();

    void importData(Map<String, Object> data);

    int toggleLock(Long itemId, Integer isLocked);
}
