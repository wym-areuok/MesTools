package com.mes.system.mapper;

import com.mes.system.domain.QueryInfo;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 资料查询的Mapper
 */
public interface QueryInfoMapper {
    /**
     * 查询资料库信息
     *
     * @param infoId
     * @return
     */
    QueryInfo selectQueryInfoByInfoId(Integer infoId);

    /**
     * 通过标题查询资料
     *
     * @param infoTitle
     * @return
     */
    QueryInfo selectByInfoTitle(String infoTitle);

    /**
     * 查询资料库信息列表
     *
     * @param queryInfo
     * @return
     */
    List<QueryInfo> selectQueryInfoList(QueryInfo queryInfo);

    /**
     * 新增资料库信息
     *
     * @param queryInfo
     * @return
     */
    int insertQueryInfo(QueryInfo queryInfo);

    /**
     * 修改资料库信息
     *
     * @param queryInfo
     * @return
     */
    int updateQueryInfo(QueryInfo queryInfo);

    /**
     * 删除资料库信息
     *
     * @param infoId
     * @return
     */
    int deleteQueryInfoByInfoId(Integer infoId);

    /**
     * 批量删除资料库信息
     *
     * @param infoIds
     * @return
     */
    int deleteQueryInfoByInfoIds(Integer[] infoIds);
}
