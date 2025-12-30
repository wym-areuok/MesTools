package com.mes.system.service.impl;

import com.mes.common.exception.ServiceException;
import com.mes.common.utils.SecurityUtils;
import com.mes.common.utils.StringUtils;
import com.mes.system.domain.QueryInfo;
import com.mes.system.mapper.QueryInfoMapper;
import com.mes.system.service.IQueryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 资料查询的Service实现类
 */
@Service
public class QueryInfoServiceImpl implements IQueryInfoService {

    @Autowired
    private QueryInfoMapper queryInfoMapper;

    /**
     * 根据infoId获取详情
     *
     * @param infoId
     * @return
     */
    @Override
    public QueryInfo selectQueryInfoByInfoId(Integer infoId) {
        QueryInfo info = queryInfoMapper.selectQueryInfoByInfoId(infoId);
        if (info != null) {
            // 查看详情时增加搜索/阅读次数
            QueryInfo updateInfo = new QueryInfo();
            updateInfo.setInfoId(info.getInfoId());
            int newCount = (info.getSearchCount() == null ? 0 : info.getSearchCount()) + 1;
            updateInfo.setSearchCount(newCount);
            queryInfoMapper.updateQueryInfo(updateInfo);
            // 更新返回对象的阅读数，使前端显示最新值
            info.setSearchCount(newCount);
        }
        return info;
    }

    /**
     * 列表查询
     *
     * @param queryInfo
     * @return
     */
    @Override
    public List<QueryInfo> selectQueryInfoList(QueryInfo queryInfo) {
        // 列表查询不应更新搜索次数，避免性能问题和逻辑错误
        return queryInfoMapper.selectQueryInfoList(queryInfo);
    }

    /**
     * 新增并且部分字段设定默认值
     *
     * @param queryInfo
     * @return
     */
    @Override
    @Transactional
    public int insertQueryInfo(QueryInfo queryInfo) {
        validateQueryInfo(queryInfo);
        if (queryInfo.getSearchCount() == null) {
            queryInfo.setSearchCount(0);
        }
        if (StringUtils.isEmpty(queryInfo.getStatus())) {
            queryInfo.setStatus("0");
        }
        queryInfo.setCreateBy(SecurityUtils.getUsername());
        queryInfo.setCreateTime(new Date());
        return queryInfoMapper.insertQueryInfo(queryInfo);
    }

    /**
     * 工具类-校验
     *
     * @param info
     */
    private void validateQueryInfo(QueryInfo info) {
        if (StringUtils.isEmpty(info.getInfoTitle())) {
            throw new ServiceException("资料标题不能为空");
        }
        if (StringUtils.isEmpty(info.getInfoType())) {
            throw new ServiceException("资料类型不能为空");
        }
        if (StringUtils.isEmpty(info.getInfoTags())) {
            throw new ServiceException("至少选择一个标签");
        }
    }

    /**
     * 更新
     *
     * @param queryInfo
     * @return
     */
    @Override
    @Transactional
    public int updateQueryInfo(QueryInfo queryInfo) {
        validateQueryInfo(queryInfo);
        queryInfo.setUpdateBy(SecurityUtils.getUsername());
        queryInfo.setUpdateTime(new Date());
        return queryInfoMapper.updateQueryInfo(queryInfo);
    }

    /**
     * 根据infoId删除
     *
     * @param infoId
     * @return
     */
    @Override
    @Transactional
    public int deleteQueryInfoByInfoId(Integer infoId) {
        return queryInfoMapper.deleteQueryInfoByInfoId(infoId);
    }

    /**
     * 根据infoIds批量删除
     *
     * @param infoIds
     * @return
     */
    @Override
    @Transactional
    public int deleteQueryInfoByInfoIds(Integer[] infoIds) {
        return queryInfoMapper.deleteQueryInfoByInfoIds(infoIds);
    }

    /**
     * 导入-并且根据title判重
     *
     * @param infoList
     * @param updateSupport
     * @param operName
     * @return
     */
    @Override
    @Transactional
    public String importQueryInfo(List<QueryInfo> infoList, boolean updateSupport, String operName) {
        if (infoList == null || infoList.isEmpty()) {
            throw new ServiceException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        int duplicateNum = 0;
        int updateNum = 0;
        StringBuilder failureMsg = new StringBuilder();
        for (QueryInfo info : infoList) {
            try {
                validateQueryInfo(info);
                QueryInfo existingInfo = queryInfoMapper.selectByInfoTitle(info.getInfoTitle());
                if (existingInfo == null) {
                    // 补全默认值，保持与insertQueryInfo逻辑一致
                    if (info.getSearchCount() == null) {
                        info.setSearchCount(0);
                    }
                    if (StringUtils.isEmpty(info.getStatus())) {
                        info.setStatus("0");
                    }
                    info.setCreateBy(operName);
                    info.setCreateTime(new Date());
                    queryInfoMapper.insertQueryInfo(info);
                    successNum++;
                } else if (updateSupport) {
                    info.setInfoId(existingInfo.getInfoId());
                    info.setUpdateBy(operName);
                    info.setUpdateTime(new Date());
                    queryInfoMapper.updateQueryInfo(info);
                    updateNum++;
                } else {
                    duplicateNum++;
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、资料 [" + info.getInfoTitle() + "] 导入失败：" + e.getMessage();
                failureMsg.append(msg);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        resultMsg.append("导入结果：成功新增 ").append(successNum).append(" 条,成功更新 ").append(updateNum).append(" 条,跳过重复 ").append(duplicateNum).append(" 条,失败 ").append(failureNum).append(" 条");
        if (failureNum > 0) {
            resultMsg.append("<br>失败明细：").append(failureMsg);
        }
        return resultMsg.toString();
    }
}