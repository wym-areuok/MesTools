package com.mes.system.mapper;

import com.mes.system.domain.ApiManageHistory;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口请求操作历史记录
 */
public interface ApiManageHistoryMapper {
    List<ApiManageHistory> selectApiManageHistoryList(ApiManageHistory history);

    int insertApiManageHistory(ApiManageHistory history);
}
