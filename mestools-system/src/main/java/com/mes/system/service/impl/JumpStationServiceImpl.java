package com.mes.system.service.impl;

import com.mes.common.exception.ServiceException;
import com.mes.system.service.IJumpStationService;
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
        if (tableName != null) tableName = tableName.trim();
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型WC配置不完整: " + jumpType);
        }
        String sql;
        //LR WC字段是PROCESS_CODE 其他的是WC
        if ("LR".equals(jumpType)) {
            sql = "SELECT PROCESS_CODE AS stationCode, PROCESS_NAME AS stationName FROM " + tableName.toUpperCase(Locale.ROOT) + " ORDER BY PROCESS_CODE";
        } else {
            sql = "SELECT WC AS stationCode, Description AS stationName FROM " + tableName.toUpperCase(Locale.ROOT) + " ORDER BY WC";
        }
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
     * @param dbDataSource
     * @param jumpType
     * @return
     */
    @Override
    public List<Map<String, Object>> list(List<String> snList, String dbDataSource, String jumpType) {
        if (snList == null || snList.isEmpty()) {
            throw new ServiceException("SN列表不能为空!");
        }
        if (dbDataSource == null || dbDataSource.trim().isEmpty()) {
            throw new ServiceException("数据源不能为空!");
        }
        if (jumpType == null || jumpType.trim().isEmpty()) {
            throw new ServiceException("跳站类型不能为空!");
        }
        DataSource dataSource;
        try {
            dataSource = getDataSourceByDbName(dbDataSource);
        } catch (RuntimeException e) {
            throw new ServiceException(e.getMessage());
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String tableName = dictDataService.selectDictByTypeAndLabel(jumpType, "SN");
        if (tableName != null) tableName = tableName.trim();
        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型SN配置不完整: " + jumpType);
        }
        try {
            List<Map<String, Object>> result = querySnListFromDb(snList, jumpType, tableName, template);
            if (!result.isEmpty()) {
                // 检查model字段是否一致
                String modelName = validateModelConsistency(result);
                // 根据model获取SFC
                String sfc = getSfcByModel(jumpType, modelName);
                result.forEach(row -> row.put("sfc", sfc));
                return result;
            }
            return result;
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
     * @param dbDataSource
     * @param jumpType
     * @param station
     * @param remark
     * @return
     */
    @Override
    public List<Map<String, Object>> execute(List<String> snList, String dbDataSource, String jumpType, String station, String remark) {
        if (dbDataSource == null || dbDataSource.trim().isEmpty()) {
            throw new ServiceException("数据源不能为空!");
        }
        DataSource dataSource;
        try {
            dataSource = getDataSourceByDbName(dbDataSource);
        } catch (RuntimeException e) {
            throw new ServiceException(e.getMessage());
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String tableName = dictDataService.selectDictByTypeAndLabel(jumpType, "SN");
        String logTableName = dictDataService.selectDictByTypeAndLabel(jumpType, "LOG");
        if (tableName != null) tableName = tableName.trim();
        if (logTableName != null) logTableName = logTableName.trim();

        if (tableName == null || tableName.isEmpty()) {
            throw new ServiceException("跳站类型SN配置不完整: " + jumpType);
        }
        // 日志表逻辑: 如果配置了LOG表,则必须校验数据库中是否存在; 如果未配置,则不强制记录
        if (logTableName != null && !logTableName.isEmpty()) {
            if (!checkTableExists(template, logTableName)) {
                throw new ServiceException("跳站类型 " + jumpType + " 配置的日志表 [" + logTableName + "] 在数据库中不存在，请检查配置或数据库。");
            }
        }
        try {
            List<Map<String, Object>> result = querySnListFromDb(snList, jumpType, tableName, template);
            if (!result.isEmpty()) {
                // 安全校验：执行跳站前也确保机型一致，防止绕过前端直接调用接口导致的数据错误
                validateModelConsistency(result);
                return executeJumpInDatabase(result, jumpType, station, remark, dbDataSource, tableName, logTableName, template);
            }
        } catch (DataAccessException e) {
            logger.warn("查询数据库 {} 时发生错误: {}", dbDataSource, e.getMessage());
            throw new ServiceException("查询数据库 " + dbDataSource + " 时发生错误: " + e.getMessage());
        }
        throw new ServiceException("未找到有效的SN信息: " + String.join(", ", snList));
    }

    /**
     * 检查数据库中是否存在指定的表
     * 使用 SQL Server 的 OBJECT_ID 函数，它可以处理 'TableName' 和 'Schema.TableName' 两种格式
     */
    private boolean checkTableExists(JdbcTemplate template, String tableName) {
        try {
            String sql = "SELECT CASE WHEN OBJECT_ID(?) IS NOT NULL THEN 1 ELSE 0 END";
            Integer result = template.queryForObject(sql, Integer.class, tableName);
            return result != null && result == 1;
        } catch (DataAccessException e) {
            logger.warn("检查表 {} 是否存在时发生错误: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 通用方法：从数据库查询SN列表信息
     */
    private List<Map<String, Object>> querySnListFromDb(List<String> snList, String jumpType, String tableName, JdbcTemplate template) {
        // 先把snList处理成用于McbSno IN查询的形式
        String mcbSnoList = String.join(",", Collections.nCopies(snList.size(), "?"));
        StringBuilder snSql = new StringBuilder();
        // MDS SN是Sno 其他的是McbSno
        if ("MDS".equalsIgnoreCase(jumpType)) {
            snSql.append("SELECT TOP 10000 * FROM ").append(tableName)
                    .append(" WHERE Sno IN (").append(mcbSnoList).append(")");
        } else {
            snSql.append("SELECT TOP 10000 * FROM ").append(tableName)
                    .append(" WHERE McbSno IN (").append(mcbSnoList).append(")");
        }
        return template.queryForList(snSql.toString(), snList.toArray());
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
    private List<Map<String, Object>> executeJumpInDatabase(List<Map<String, Object>> snInfoList, String jumpType, String station, String remark, String dbDataSource, String tableName, String logTableName, JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 预先判断类型和构建SQL，避免在循环中重复判断
        boolean isMds = "MDS".equalsIgnoreCase(jumpType);
        String updateSql;
        if (isMds) {
            updateSql = "UPDATE " + tableName + " SET NextWc = ?, Udt = GETDATE() WHERE Sno = ?";
        } else {
            updateSql = "UPDATE " + tableName + " SET NWC = ?, Udt = GETDATE() WHERE McbSno = ?";
        }

        // 判断是否需要记录日志 (表名存在 且 类型支持)
        boolean shouldLog = logTableName != null && !logTableName.isEmpty();

        // 预构建日志SQL，避免在循环中重复拼接字符串
        String pcaLogSql = "INSERT INTO " + logTableName + " (SnoId, McbSno, Original_WC, Dest_WC, Reason, Creator, Cdt) VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
        String rmaLogSql = "INSERT INTO " + logTableName + " (SnoId, OriginalWC, TestCount, OriginalNWC, NWC, Type, Reason, Remark,Editor, Cdt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";

        for (Map<String, Object> snInfo : snInfoList) {
            // 根据跳站类型动态获取列名 MDS需要特殊处理 -NextWc - Wc
            String sn = Objects.toString(snInfo.get(isMds ? "Sno" : "McbSno"), "");
            String currentWc = Objects.toString(snInfo.get(isMds ? "Wc" : "WC"), "");
            String currentNwc = Objects.toString(snInfo.get(isMds ? "NextWc" : "NWC"), "");
            Object snoIdObj = snInfo.get(isMds ? "Id" : "SnoId");
            int snoId = (snoIdObj instanceof Number) ? ((Number) snoIdObj).intValue() : 0;

            Map<String, Object> resultRow = new LinkedHashMap<>();
            resultRow.put("SN", sn);
            resultRow.put("原始站点", currentWc);
            resultRow.put("目标站点", station);
            try {
                int updatedRows = jdbcTemplate.update(updateSql, station, sn);
                if (updatedRows > 0) {
                    // 记录日志 - 仅当配置了日志表且类型匹配时
                    if (shouldLog && "PCA".equalsIgnoreCase(jumpType)) {
                        jdbcTemplate.update(pcaLogSql, snoId, sn, currentWc, station, remark, "MESTools");
                    } else if (shouldLog && "RMA".equalsIgnoreCase(jumpType)) {
                        jdbcTemplate.update(rmaLogSql, snoId, currentWc, 1, currentNwc, station, "A", remark, "", "MESTools");
                    }
                    // 对于LR,MDS等类型,由于字典中未配置LOG表,logTableName为空,不会执行日志插入(数据库没找到相关跳站Log表)
                    resultRow.put("结果", "成功");
                } else {
                    resultRow.put("结果", "失败");
                    resultRow.put("信息", "更新0行，SN可能不存在");
                }
            } catch (DataAccessException e) {
                logger.error("跳站操作失败,SN: " + sn + " 数据库: " + dbDataSource, e);
                resultRow.put("结果", "失败");
                resultRow.put("信息", "数据库异常: " + e.getMessage());
            }
            resultList.add(resultRow);
        }
        return resultList;
    }

    /**
     * 验证SN列表中所有项目的model字段是否一致,并返回基准model值 防止不同model的SN进行跳站
     *
     * @param result
     * @return
     */
    private String validateModelConsistency(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty()) {
            return null;
        }
        // 动态判断SN列名是 McbSno 还是 Sno
        String snKey = result.get(0).containsKey("McbSno") ? "McbSno" : "Sno";
        // 检查model为空的情况并收集对应的SN
        List<String> nullModelSnList = result.stream().filter(row -> row.get("Model") == null || String.valueOf(row.get("Model")).isEmpty()).map(row -> String.valueOf(row.get(snKey))).collect(Collectors.toList());
        if (!nullModelSnList.isEmpty()) {
            throw new ServiceException("以下SN的机型为空: " + String.join(", ", nullModelSnList));
        }
        String baseModel = String.valueOf(result.get(0).get("Model"));
        // 查找与基准model不一致的SN
        List<String> inconsistentSnList = result.stream().filter(row -> !Objects.equals(baseModel, String.valueOf(row.get("Model")))).map(row -> String.valueOf(row.get(snKey))).collect(Collectors.toList());
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
        if (sfcTableName != null) sfcTableName = sfcTableName.trim();
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