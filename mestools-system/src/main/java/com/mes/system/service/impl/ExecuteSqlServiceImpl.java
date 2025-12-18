package com.mes.system.service.impl;

import com.mes.system.service.IExecuteSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 执行SQL的Service实现类
 */
@Service
public class ExecuteSqlServiceImpl implements IExecuteSqlService {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteSqlServiceImpl.class);

    //TODO 为了测试暂时将本地作为主数据源
    // 注入主数据源 (LOCALHOST)
    @Autowired
    @Qualifier("dataSource")
    private DataSource localhostDataSource;

    // 注入其他数据源（使用required = false 即使不存在也不会报错）
    @Autowired(required = false)
    @Qualifier("iptfisDb71DataSource")
    private DataSource iptfisDb71DataSource;

    @Autowired(required = false)
    @Qualifier("iptfisDb70DataSource")
    private DataSource iptfisDb70DataSource;

    @Autowired(required = false)
    @Qualifier("itefisDbOnlineDataSource")
    private DataSource itefisDbOnlineDataSource;

    // 读取数据源开关配置
    @Value("${spring.datasource.druid.extra.iptfis_db_71.enabled}")
    private boolean iptfisDb71Enabled;

    @Value("${spring.datasource.druid.extra.iptfis_db_70.enabled}")
    private boolean iptfisDb70Enabled;

    @Value("${spring.datasource.druid.extra.itefis_db_online.enabled}")
    private boolean itefisDbOnlineEnabled;

    /**
     * 执行查询
     *
     * @param dbName
     * @param sql
     * @return
     */
    @Override
    public List<Map<String, Object>> executeQuery(String dbName, String sql) {
        long startTime = System.currentTimeMillis();
        logger.info("开始执行查询SQL: {}, 数据库: {}", sql, dbName);
        List<Map<String, Object>> result = new ArrayList<>();
        // 使用 try-with-resources 自动关闭资源
        try (Connection connection = getDataSourceByDbName(dbName).getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5); // 设置查询超时时间（秒）
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i); // 使用getColumnLabel支持别名
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    result.add(row);
                }
            }
            long endTime = System.currentTimeMillis();
            logger.info("查询执行完成,耗时: {} ms,返回 {} 行数据", (endTime - startTime), result.size());
            return result;
        } catch (SQLException e) {
            logger.error("执行查询SQL出错: {}", sql, e);
            throw new RuntimeException("执行查询SQL出错: " + e.getMessage(), e);
        }
    }

    /**
     * 执行更新、插入、删除的通用方法
     *
     * @param dbName
     * @param sql
     * @param operation
     * @return
     */
    private int executeDML(String dbName, String sql, String operation) {
        long startTime = System.currentTimeMillis();
        logger.info("开始执行{}SQL: {}, 数据库: {}", operation, sql, dbName);
        try (Connection connection = getDataSourceByDbName(dbName).getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            int rowsAffected = statement.executeUpdate();
            long endTime = System.currentTimeMillis();
            logger.info("{}执行完成,耗时: {} ms,影响 {} 行数据", operation, (endTime - startTime), rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("执行{}SQL出错: {}", operation, sql, e);
            throw new RuntimeException("执行" + operation + "SQL出错: " + e.getMessage(), e);
        }
    }

    @Override
    public int executeUpdate(String dbName, String sql) {
        return executeDML(dbName, sql, "更新");
    }

    @Override
    public int executeInsert(String dbName, String sql) {
        return executeDML(dbName, sql, "插入");
    }

    @Override
    public int executeDelete(String dbName, String sql) {
        return executeDML(dbName, sql, "删除");
    }

    /**
     * 根据数据库名称获取对应的数据源
     *
     * @param dbName
     * @return
     */
    private DataSource getDataSourceByDbName(String dbName) {
        switch (dbName) {
            case "LOCALHOST":
                return localhostDataSource;
            case "IPTFIS-DB-71":
                if (iptfisDb71Enabled && iptfisDb71DataSource != null) {
                    return iptfisDb71DataSource;
                }
                throw new RuntimeException("数据源 IPTFIS-DB-71 未启用或未配置");
            case "IPTFIS-DB-70":
                if (iptfisDb70Enabled && iptfisDb70DataSource != null) {
                    return iptfisDb70DataSource;
                }
                throw new RuntimeException("数据源 IPTFIS-DB-70 未启用或未配置");
            case "ITEFIS-DB-ONLINE":
                if (itefisDbOnlineEnabled && itefisDbOnlineDataSource != null) {
                    return itefisDbOnlineDataSource;
                }
                throw new RuntimeException("数据源 ITEFIS-DB-ONLINE 未启用或未配置");
            default:
                throw new RuntimeException("未知或未启用的数据源: " + dbName);
        }
    }
}
