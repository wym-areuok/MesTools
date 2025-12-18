package com.mes.system.service;

import com.mes.system.domain.vo.SnInfoVO;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-27
 * @Description: 板卡跳站的Service
 */
public interface IJumpStationService {

    List<Map<String, Object>> getStationList(String jumpType);

    List<SnInfoVO> list(List<String> snList, String jumpType, String dbDataSource);

    String execute(List<String> snList, String station, String jumpType, String remark, String dbDataSource);
}