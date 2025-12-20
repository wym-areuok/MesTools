package com.mes.system.service;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-27
 * @Description: 板卡跳站的Service
 */
public interface IJumpStationService {

    List<Map<String, Object>> getStationList(String jumpType);

    List<Map<String, Object>> list(List<String> snList, String dbDataSource, String jumpType);

    List<Map<String, Object>> execute(List<String> snList, String dbDataSource, String jumpType, String station, String remark);
}