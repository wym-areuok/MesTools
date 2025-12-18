package com.mes.system.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-24
 * @Description: 字符串工具的Mapper
 */
public interface StringToolMapper {
    /**
     * 删除指定用户的数据
     *
     * @param userId
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 查询指定用户的记录数
     *
     * @param userId
     * @return
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 为表创建索引
     */
    void createIndex();
}