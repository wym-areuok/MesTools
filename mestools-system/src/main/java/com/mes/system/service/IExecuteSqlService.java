package com.mes.system.service;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 执行SQL的Service
 */
public interface IExecuteSqlService {

    List<Map<String, Object>> executeQuery(String dbName, String sql);

    int executeUpdate(String dbName, String sql);

    int executeInsert(String dbName, String sql);

    int executeDelete(String dbName, String sql);
}