package com.mes.system.service.impl;

import com.mes.common.exception.ServiceException;
import com.mes.system.domain.vo.SnInfoVO;
import com.mes.system.service.IJumpStationService;
import com.mes.system.service.ISysDictDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-27
 * @Description: 板卡跳站的Service实现类
 */
@Service
public class JumpStationServiceImpl implements IJumpStationService {

    private static final Logger logger = LoggerFactory.getLogger(JumpStationServiceImpl.class);
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
     * @param dbName
     * @return
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
     * 获取站点List name-code
     *
     * @param jumpType
     * @return
     */
    @Override
    public List<Map<String, Object>> getStationList(String jumpType) {
        String tableName = dictDataService.selectDictByTypeAndLabel(jumpType, "WC");
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型WC配置不完整: " + jumpType);
        }
        String sql = "SELECT WC AS stationCode, Description AS stationName FROM " + tableName.toUpperCase(Locale.ROOT) + " ORDER BY WC";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (DataAccessException e) {
            throw new ServiceException("查询站点信息失败: " + e.getMessage());
        }
    }

    /**
     * 查询SN的信息
     *
     * @param snList
     * @param jumpType
     * @param dbDataSource
     * @return
     */
    @Override
    public List<SnInfoVO> list(List<String> snList, String jumpType, String dbDataSource) {
        if (snList == null || snList.isEmpty()) {
            throw new ServiceException("SN列表不能为空!");
        }
        if (jumpType == null || jumpType.trim().isEmpty()) {
            throw new ServiceException("跳站类型不能为空!");
        }
        if (dbDataSource == null || dbDataSource.trim().isEmpty()) {
            throw new ServiceException("数据源不能为空!");
        }
        // 根据dbSource获取对应的数据源
        DataSource dataSource;
        try {
            dataSource = getDataSourceByDbName(dbDataSource);
        } catch (RuntimeException e) {
            throw new ServiceException(e.getMessage());
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String tableName = dictDataService.selectDictByTypeAndLabel(jumpType, "SN");
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型SN配置不完整: " + jumpType);
        }
        // 先把snList处理成用于McbSno IN查询的形式
        String mcbSnoList = String.join(",", Collections.nCopies(snList.size(), "?"));
        StringBuilder snSql = new StringBuilder();
        snSql.append("SELECT TOP 10000 * FROM ").append(tableName).append(" WHERE McbSno IN (").append(mcbSnoList).append(")");
        try {
            List<SnInfoVO> result = template.query(snSql.toString(), snList.toArray(), new BeanPropertyRowMapper<>(SnInfoVO.class));
            // 如果查询到数据,则处理并返回
            if (!result.isEmpty()) {
                // 检查model字段是否一致
                String modelName = validateModelConsistency(result);
                // 根据model获取SFC
                String sfc = getSfcByModel(jumpType, modelName);
                result.forEach(snInfo -> snInfo.setSfc(sfc));
                return result;
            }
            return result; // 返回空列表
        } catch (DataAccessException e) {
            logger.warn("查询数据库 {} 时发生错误: {}", dbDataSource, e.getMessage());
            // 发生异常时抛出ServiceException
            throw new ServiceException("查询数据库 " + dbDataSource + " 时发生错误: " + e.getMessage());
        }
    }

    /**
     * 执行跳站
     *
     * @param snList
     * @param station
     * @param jumpType
     * @param remark
     * @param dbDataSource
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String execute(List<String> snList, String station, String jumpType, String remark, String dbDataSource) {
        if (dbDataSource == null || dbDataSource.trim().isEmpty()) {
            throw new ServiceException("数据源不能为空!");
        }
        // 根据dbSource获取对应的数据源
        DataSource dataSource;
        try {
            dataSource = getDataSourceByDbName(dbDataSource);
        } catch (RuntimeException e) {
            throw new ServiceException(e.getMessage());
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String tableName = dictDataService.selectDictByTypeAndLabel(jumpType, "SN");
        String logTableName = dictDataService.selectDictByTypeAndLabel(jumpType, "LOG");
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型SN配置不完整: " + jumpType);
        }
        if (logTableName == null || logTableName.isEmpty()) {
            throw new ServiceException("跳站类型LOG配置不完整: " + jumpType);
        }
        String mcbSnoList = String.join(",", Collections.nCopies(snList.size(), "?"));
        StringBuilder snSql = new StringBuilder();
        snSql.append("SELECT TOP 10000 * FROM ").append(tableName).append(" WHERE McbSno IN (").append(mcbSnoList).append(")");
        try {
            List<SnInfoVO> result = template.query(snSql.toString(), snList.toArray(), new BeanPropertyRowMapper<>(SnInfoVO.class));
            if (!result.isEmpty()) {
                return executeJumpInDatabase(result, station, remark, dbDataSource, tableName, logTableName, template);
            }
        } catch (DataAccessException e) {
            logger.warn("查询数据库 {} 时发生错误: {}", dbDataSource, e.getMessage());
            throw new ServiceException("查询数据库 " + dbDataSource + " 时发生错误: " + e.getMessage());
        }
        throw new ServiceException("未找到有效的SN信息: " + String.join(", ", snList));
    }

    /**
     * 在指定数据库中执行跳站操作
     *
     * @param snInfoList
     * @param station
     * @param remark
     * @param dbDataSource
     * @param tableName
     * @param logTableName
     * @param jdbcTemplate
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    private String executeJumpInDatabase(List<SnInfoVO> snInfoList, String station, String remark, String dbDataSource, String tableName, String logTableName, JdbcTemplate jdbcTemplate) {
        int successCount = 0;
        List<String> failedSnList = new ArrayList<>();
        List<String> processedSnList = new ArrayList<>();
        for (SnInfoVO snInfo : snInfoList) {
            String sn = snInfo.getMcbSno();
            processedSnList.add(sn);
            // 原始站点 跳站前的站点
            String originalWc = snInfo.getNwc();
            // 从SnInfoVO中获取SnoId（SNO表的主键）用来log表插入值
            int snoId = snInfo.getSnoId();
            try {
                String updateSql = "UPDATE " + tableName + " SET NWC = ?, Udt = GETDATE() " + "WHERE McbSno = ?";
                int updatedRows = jdbcTemplate.update(updateSql, station, sn);
                if (updatedRows > 0) {
                    successCount++;
                    // 记录日志 - 使用SN表的主键ID作为LOG表的SnoId字段
                    String logSql = "INSERT INTO " + logTableName + " (SnoId, McbSno, Original_WC, Dest_WC, Reason, Creator, Cdt) " + "VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
                    jdbcTemplate.update(logSql, snoId, sn, originalWc, station, remark, "MESTools");
                } else {
                    failedSnList.add(sn);
                }
            } catch (DataAccessException e) {
                logger.error("跳站操作失败,SN: " + sn + " 数据库: " + dbDataSource, e);
                failedSnList.add(sn);
                // 发生异常时抛出,触发事务回滚
                throw new ServiceException("跳站操作失败,SN: " + sn + " 错误: " + e.getMessage());
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        resultMsg.append("成功处理 ").append(successCount).append(" 个SN");
        if (!failedSnList.isEmpty()) {
            resultMsg.append(",失败SN: ").append(String.join(", ", failedSnList));
        }
        // 记录操作日志
        logger.info("跳站操作完成。处理总数: {}, 成功数: {}, 失败SN: {}", processedSnList.size(), successCount, String.join(", ", failedSnList));
        return resultMsg.toString();
    }

    /**
     * 验证SN列表中所有项目的model字段是否一致,并返回基准model值 防止不同model的SN进行跳站
     *
     * @param result
     * @return
     */
    private String validateModelConsistency(List<SnInfoVO> result) {
        // 检查model为空的情况并收集对应的SN
        List<String> nullModelSnList = result.stream().filter(snInfo -> snInfo.getModel() == null || snInfo.getModel().isEmpty()).map(SnInfoVO::getMcbSno).collect(Collectors.toList());
        if (!nullModelSnList.isEmpty()) {
            throw new ServiceException("以下SN的机型为空: " + String.join(", ", nullModelSnList));
        }
        String baseModel = result.get(0).getModel();
        // 查找与基准model不一致的SN
        List<String> inconsistentSnList = result.stream().filter(snInfo -> !Objects.equals(baseModel, snInfo.getModel())).map(SnInfoVO::getMcbSno).collect(Collectors.toList());
        if (!inconsistentSnList.isEmpty()) {
            throw new ServiceException("以下SN的机型与其他不一致: " + String.join(", ", inconsistentSnList) + "。基准机型为: " + baseModel);
        }
        return baseModel;
    }

    /**
     * 根据model获取对应的SFC
     *
     * @param jumpType
     * @param modelName
     * @return
     */
    private String getSfcByModel(String jumpType, String modelName) {
        String sfcTableName = dictDataService.selectDictByTypeAndLabel(jumpType, "SFC");
        if (sfcTableName == null || sfcTableName.isEmpty()) {
            throw new ServiceException("跳站类型SFC配置不完整: " + jumpType);
        }
        String sfcSql = "SELECT Flow FROM " + sfcTableName + " WHERE Model = ?";
        try {
            List<String> sfcList = jdbcTemplate.queryForList(sfcSql, String.class, modelName);
            if (sfcList.isEmpty()) {
                throw new ServiceException("未找到机型 '" + modelName + "' 对应的SFC配置");
            } else if (sfcList.size() > 1) {
                throw new ServiceException("机型 '" + modelName + "' 对应多个SFC配置,请检查数据");
            }
            return sfcList.get(0);
        } catch (DataAccessException e) {
            throw new ServiceException("查询SFC信息失败: " + e.getMessage());
        }
    }
}