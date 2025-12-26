package com.mes.system.mapper;

import com.mes.system.domain.ApiManageItem;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理
 */
public interface ApiManageItemMapper {
    // 查询所有非环境节点 (用于构建树)
    List<ApiManageItem> selectApiTreeList();

    // 查询所有环境节点
    List<ApiManageItem> selectEnvList();

    // 根据ID查询
    ApiManageItem selectApiManageItemById(Long itemId);

    // 新增
    int insertApiManageItem(ApiManageItem apiManageItem);

    // 修改
    int updateApiManageItem(ApiManageItem apiManageItem);

    // 删除
    int deleteApiManageItemById(Long itemId);

    // 删除所有环境 (用于批量保存时的重置)
    int deleteEnvItems();
}

