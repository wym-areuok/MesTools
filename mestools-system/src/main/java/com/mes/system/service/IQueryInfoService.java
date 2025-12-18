package com.mes.system.service;

import com.mes.system.domain.QueryInfo;

import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 资料查询的Service
 */
public interface IQueryInfoService {

    List<QueryInfo> selectQueryInfoList(QueryInfo queryInfo);

    QueryInfo selectQueryInfoByInfoId(Integer infoId);

    int insertQueryInfo(QueryInfo queryInfo);

    int updateQueryInfo(QueryInfo queryInfo);

    int deleteQueryInfoByInfoIds(Integer[] infoIds);

    int deleteQueryInfoByInfoId(Integer infoId);

    String importQueryInfo(List<QueryInfo> queryInfoList, boolean updateSupport, String operName);
}
