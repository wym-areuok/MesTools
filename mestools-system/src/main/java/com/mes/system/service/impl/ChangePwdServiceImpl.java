package com.mes.system.service.impl;

import com.mes.common.exception.ServiceException;
import com.mes.system.service.IChangePwdService;
import com.mes.system.service.ISysDictDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-07
 * @Description: 修改FIS用户密码的Service实现类
 */
@Service
public class ChangePwdServiceImpl implements IChangePwdService {

    private static final Logger logger = LoggerFactory.getLogger(ChangePwdServiceImpl.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;
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
    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 根据数据库名称获取对应的数据源
     *
     * @param dbName 数据库名称
     * @return 对应的数据源
     */
    private DataSource getDataSourceByDbName(String dbName) {
        switch (dbName) {
            case "LOCALHOST":
                return jdbcTemplate.getDataSource();
            case "IPTFIS-DB-71":
                if (iptfisDb71DataSource != null && iptfisDb71Enabled) {
                    return iptfisDb71DataSource;
                }
                if (!iptfisDb71Enabled) {
                    throw new RuntimeException("数据源 IPTFIS-DB-71 未启用");
                }
                throw new RuntimeException("数据源 IPTFIS-DB-71 未配置");
            case "IPTFIS-DB-70":
                if (iptfisDb70DataSource != null && iptfisDb70Enabled) {
                    return iptfisDb70DataSource;
                }
                if (!iptfisDb70Enabled) {
                    throw new RuntimeException("数据源 IPTFIS-DB-70 未启用");
                }
                throw new RuntimeException("数据源 IPTFIS-DB-70 未配置");
            case "ITEFIS-DB-ONLINE":
                if (itefisDbOnlineDataSource != null && itefisDbOnlineEnabled) {
                    return itefisDbOnlineDataSource;
                }
                if (!itefisDbOnlineEnabled) {
                    throw new RuntimeException("数据源 ITEFIS-DB-ONLINE 未启用");
                }
                throw new RuntimeException("数据源 ITEFIS-DB-ONLINE 未配置");
            default:
                throw new RuntimeException("未知数据库名称: " + dbName + ",请检查是否已在字典中维护");
        }
    }

    /**
     * 修改当前用户和其他用户密码 使用一个方法
     *
     * @param fisNumber
     * @param password
     * @param dbDataSource
     * @return
     */
    @Override
    public boolean changePwd(String fisNumber, String password, String dbDataSource) {
        // 根据dbDataSource获取对应的数据源
        DataSource dataSource;
        try {
            dataSource = getDataSourceByDbName(dbDataSource != null ? dbDataSource : "LOCALHOST");
        } catch (RuntimeException e) {
            throw new ServiceException(e.getMessage());
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);

        // 从fis_pwd_table字典获取label为FISPWDTABLE的value作为tableName
        // 获取表名
        String tableName = dictDataService.selectDictByTypeAndLabel("fis_pwd_table", "FISPWDTABLE");
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("未找到FIS密码表配置信息");
        }
        try {
            String checkSql = "SELECT COUNT(1) FROM " + tableName + " WHERE fis_number = ?";
            Integer count = template.queryForObject(checkSql, Integer.class, fisNumber);
            if (count == null || count == 0) {
                throw new ServiceException("FIS账号 [" + fisNumber + "] 不存在,无法修改密码");
            }
            if (count > 1) {
                throw new ServiceException("FIS账号 [" + fisNumber + "] 存在多条记录,无法修改密码");
            }
            //懒得配置字典了直接PCA..dbo.ComplexHash(?,?)写固定调用
            String updateSql = "UPDATE " + tableName + " SET password = PCA..dbo.(fis_number, ?), Udt = GETDATE() WHERE fis_number = ?";
            int result = template.update(updateSql, password, fisNumber);
            return result > 0;
        } catch (DataAccessException e) {
            logger.error("数据库操作异常: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Invalid object name")) {
                throw new ServiceException("数据库表 [" + tableName + "] 不存在");
            }
            throw new ServiceException("数据库操作失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("密码修改异常: {}", e.getMessage());
            // ServiceException直接抛出,其他异常包装为ServiceException
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            }
            throw new ServiceException("密码修改失败: " + e.getMessage());
        }
    }
}