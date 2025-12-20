package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.enums.BusinessType;
import com.mes.system.domain.ValidationResult;
import com.mes.system.domain.dto.ExecuteSqlDTO;
import com.mes.system.service.IExecuteSqlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 执行SQL的Controller
 */
@Api(tags = "SQL执行工具")
@RestController
@RequestMapping("/dailytools/executeSql")
public class ExecuteSqlController extends BaseController {

    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList("DROP", "TRUNCATE", "ALTER", "CREATE", "RENAME");

    private static final List<Pattern> ALWAYS_TRUE_PATTERNS = Arrays.asList(
            "\\b1\\s*=\\s*1\\b",
            "\\b2\\s*>\\s*1\\b",
            "N?'(?:''|[^'])*'\\s*=\\s*N?'(?:''|[^'])*'"
    ).stream().map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE)).collect(Collectors.toList());

    private static final int MAX_SELECT_ROWS = 1000;

    //通用分词正则：用于提取关键字并忽略注释/字符串(支持 N'Unicode'格式)
    private static final Pattern GENERAL_TOKEN_PATTERN = Pattern.compile(
            "(--[^\\r\\n]*)|(/\\*[\\s\\S]*?\\*/)|(N?'(?:''|[^'])*')|(\\[[^\\]]*\\])|(;)|\\b(SELECT|UPDATE|INSERT|DELETE|DROP|TRUNCATE|ALTER|CREATE|RENAME)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * SELECT TOP N 校验正则
     * Group 1: 单行注释 (--...)
     * Group 2: 多行注释 (/*...*\/)
     * Group 3: 字符串 (N'...' 或 '...')
     * Group 4: 方括号标识符 ([...])
     * Group 5: SELECT 关键字 (仅匹配单词边界)
     * Group 6: TOP N 中的 N (捕获数字 \d+)
     */
    private static final Pattern SELECT_TOP_PATTERN = Pattern.compile(
            "(--[^\\r\\n]*)|(/\\*[\\s\\S]*?\\*/)|(N?'(?:''|[^'])*')|(\\[[^\\]]*\\])|(\\bSELECT\\b)(?:\\s+(?:DISTINCT|ALL))?(?:\\s+TOP(?:\\s+|\\s*\\(\\s*)(\\d+))?",
            Pattern.CASE_INSENSITIVE);

    //WHERE 关键字查找正则 (用于精准定位WHERE子句)
    private static final Pattern WHERE_PATTERN = Pattern.compile(
            "(--[^\\r\\n]*)|(/\\*[\\s\\S]*?\\*/)|(N?'(?:''|[^'])*')|(\\[[^\\]]*\\])|(\\bWHERE\\b)",
            Pattern.CASE_INSENSITIVE);

    @Autowired
    private IExecuteSqlService sqlExecuteService;

    /**
     * 查询操作
     *
     * @param request
     * @return
     */
    @ApiOperation("执行查询")
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:query')")
    @Log(title = "SQL执行工具", businessType = BusinessType.SELECT, isSaveResponseData = false)
    @PostMapping("/query")
    public AjaxResult executeQuery(@RequestBody ExecuteSqlDTO request) {
        Future<List<Map<String, Object>>> future = null;
        try {
            ValidationResult validation = validate(request.getSqlContent(), "SELECT");
            if (!validation.isValid()) {
                return AjaxResult.error("SQL语句验证失败：" + validation.getMessage());
            }
            future = CompletableFuture.supplyAsync(() -> sqlExecuteService.executeQuery(request.getDbDataSource(), request.getSqlContent()));
            List<Map<String, Object>> queryResult = future.get(5, TimeUnit.SECONDS);
            return AjaxResult.success(queryResult);
        } catch (TimeoutException e) {
            // 在超时后,尝试中断后台线程以取消数据库查询
            if (future != null) future.cancel(true);
            return AjaxResult.error("SQL执行超时,请优化SQL语句或检查数据库状态");
        } catch (Exception e) {
            logger.error("查询SQL执行异常", e);
            return AjaxResult.error("查询执行失败：" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * 更新操作
     *
     * @param request
     * @return
     */
    @ApiOperation("执行更新")
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:update')")
    @Log(title = "SQL执行工具", businessType = BusinessType.UPDATE)
    @PutMapping("/update")
    public AjaxResult executeUpdate(@RequestBody ExecuteSqlDTO request) {
        Future<Integer> future = null;
        try {
            ValidationResult validation = validate(request.getSqlContent(), "UPDATE");
            if (!validation.isValid()) {
                return AjaxResult.error("SQL语句验证失败：" + validation.getMessage());
            }
            future = CompletableFuture.supplyAsync(() -> sqlExecuteService.executeUpdate(request.getDbDataSource(), request.getSqlContent()));
            int affectedRows = future.get(5, TimeUnit.SECONDS);
            return AjaxResult.success("更新成功", affectedRows);
        } catch (TimeoutException e) {
            if (future != null) future.cancel(true);
            return AjaxResult.error("SQL执行超时,请优化SQL语句或检查数据库状态");
        } catch (Exception e) {
            logger.error("更新SQL执行异常", e);
            return AjaxResult.error("更新执行失败：" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * 插入操作
     *
     * @param request
     * @return
     */
    @ApiOperation("执行插入")
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:insert')")
    @Log(title = "SQL执行工具", businessType = BusinessType.INSERT)
    @PostMapping("/insert")
    public AjaxResult executeInsert(@RequestBody ExecuteSqlDTO request) {
        Future<Integer> future = null;
        try {
            ValidationResult validation = validate(request.getSqlContent(), "INSERT");
            if (!validation.isValid()) {
                return AjaxResult.error("SQL语句验证失败：" + validation.getMessage());
            }
            future = CompletableFuture.supplyAsync(() -> sqlExecuteService.executeInsert(request.getDbDataSource(), request.getSqlContent()));
            int affectedRows = future.get(5, TimeUnit.SECONDS);
            return AjaxResult.success("插入成功", affectedRows);
        } catch (TimeoutException e) {
            if (future != null) future.cancel(true);
            return AjaxResult.error("SQL执行超时,请优化SQL语句或检查数据库状态");
        } catch (Exception e) {
            logger.error("插入SQL执行异常", e);
            return AjaxResult.error("插入执行失败：" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * 删除操作
     *
     * @param request
     * @return
     */
    @ApiOperation("执行删除")
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:delete')")
    @Log(title = "SQL执行工具", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete")
    public AjaxResult executeDelete(@RequestBody ExecuteSqlDTO request) {
        Future<Integer> future = null;
        try {
            ValidationResult validation = validate(request.getSqlContent(), "DELETE");
            if (!validation.isValid()) {
                return AjaxResult.error("SQL语句验证失败：" + validation.getMessage());
            }
            future = CompletableFuture.supplyAsync(() -> sqlExecuteService.executeDelete(request.getDbDataSource(), request.getSqlContent()));
            int affectedRows = future.get(5, TimeUnit.SECONDS);
            return AjaxResult.success("删除成功", affectedRows);
        } catch (TimeoutException e) {
            if (future != null) future.cancel(true);
            return AjaxResult.error("SQL执行超时,请优化SQL语句或检查数据库状态");
        } catch (Exception e) {
            logger.error("删除SQL执行异常", e);
            return AjaxResult.error("删除执行失败：" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * SQL 安全校验核心逻辑：
     * 1. 预处理：去除 BOM 头及首尾空格,防止字符编码绕过。
     * 2. 智能分词：使用正则解析 SQL,自动忽略注释,字符串常量(含 N'Unicode')及方括号标识符,精准提取有效关键字。
     * 3. 防批处理：检测分号 (;),强制禁止一次性执行多条 SQL 语句。
     * 4. 关键字校验：
     * 验证首个有效关键字是否匹配当前操作类型。
     * 全局拦截高危关键字 (DROP, TRUNCATE, ALTER, CREATE, RENAME),INSERT 操作除外。
     * 互斥检查：SELECT 语句禁止包含 DML 关键字；DML 语句禁止包含其他类型的 DML 操作且主关键字只能出现一次。
     * 5. 深度语法校验：
     * SELECT: 强制所有查询（含子查询,联合查询）必须包含 TOP N 语法,且 N <= 1000。
     * UPDATE/DELETE: 强制包含 WHERE 子句；禁止恒真条件 (如 1=1)；强制包含 = 或 IN 精确匹配条件。
     */
    private ValidationResult validate(String sql, String operationType) {
        if (!StringUtils.hasText(sql)) {
            return ValidationResult.invalid("SQL语句不能为空");
        }
        String trimmedSql = sql.replace("\uFEFF", "").trim();
        // 核心安全逻辑：关键字计数与互斥检查
        Matcher matcher = GENERAL_TOKEN_PATTERN.matcher(trimmedSql);
        Map<String, Integer> kwCounts = new HashMap<>();
        kwCounts.put("SELECT", 0);
        kwCounts.put("UPDATE", 0);
        kwCounts.put("INSERT", 0);
        kwCounts.put("DELETE", 0);
        kwCounts.put("DANGEROUS", 0);
        String firstKeyword = null;
        boolean hasSemicolon = false;
        while (matcher.find()) {
            // 跳过 Group 1(单行注释), Group 2(多行注释), Group 3(字符串), Group 4(方括号标识符)
            if (matcher.group(1) != null || matcher.group(2) != null || matcher.group(3) != null || matcher.group(4) != null) {
                continue;
            }
            // Group 5 是分号
            if (matcher.group(5) != null) {
                hasSemicolon = true;
                continue;
            }
            // Group 6 是关键字
            String keyword = matcher.group(6).toUpperCase();
            if (firstKeyword == null) {
                firstKeyword = keyword;
            }
            // 如果之前已经出现了分号,且现在又出现了新的关键字,说明是多条语句 (Batch)
            if (hasSemicolon) {
                return ValidationResult.invalid("检测到多条SQL语句（通过分号分隔）,请一次只执行一条语句");
            }
            if (DANGEROUS_KEYWORDS.contains(keyword)) {
                kwCounts.put("DANGEROUS", kwCounts.get("DANGEROUS") + 1);
            } else {
                kwCounts.put(keyword, kwCounts.getOrDefault(keyword, 0) + 1);
            }
        }
        if (firstKeyword == null || !firstKeyword.equals(operationType)) {
            return ValidationResult.invalid(String.format("%s语句必须以%s开头", operationType, operationType));
        }
        // 检查危险关键字
        if (!"INSERT".equals(operationType) && kwCounts.get("DANGEROUS") > 0) {
            return ValidationResult.invalid("SQL语句包含危险关键字 (DROP, TRUNCATE, ALTER, CREATE, RENAME)");
        }
        // 检查混合操作与多语句
        if ("SELECT".equals(operationType)) {
            if (kwCounts.get("UPDATE") > 0 || kwCounts.get("INSERT") > 0 || kwCounts.get("DELETE") > 0) {
                return ValidationResult.invalid("查询语句不允许包含 UPDATE/INSERT/DELETE 操作");
            }
            return validateSelectTopN(trimmedSql);
        } else {
            // UPDATE, INSERT, DELETE
            if (kwCounts.get(operationType) > 1) {
                return ValidationResult.invalid(String.format("检测到多个 %s 关键字,禁止执行多条语句", operationType));
            }
            List<String> forbidden = new ArrayList<>(Arrays.asList("UPDATE", "INSERT", "DELETE"));
            forbidden.remove(operationType);
            for (String k : forbidden) {
                if (kwCounts.get(k) > 0) {
                    return ValidationResult.invalid(String.format("%s 语句不允许包含 %s 操作", operationType, k));
                }
            }
        }

        switch (operationType) {
            case "UPDATE":
            case "DELETE":
                // 使用正则遍历查找真正的 WHERE 关键字 (忽略注释,字符串,方括号中的内容)
                Matcher whereMatcher = WHERE_PATTERN.matcher(trimmedSql);
                String rawWhereClause = null;
                while (whereMatcher.find()) {
                    // Group 1-4 是干扰项,跳过
                    if (whereMatcher.group(1) != null || whereMatcher.group(2) != null || whereMatcher.group(3) != null || whereMatcher.group(4) != null) {
                        continue;
                    }
                    // Group 5 是 WHERE 关键字
                    if (whereMatcher.group(5) != null) {
                        rawWhereClause = trimmedSql.substring(whereMatcher.end());
                        break;
                    }
                }
                if (rawWhereClause == null) {
                    return ValidationResult.invalid(operationType + " 操作必须包含WHERE条件");
                }
                // 清理 WHERE 子句：只去除注释,保留字符串内容以便检测 'a'='a'
                String cleanWhereClause = rawWhereClause.replaceAll("(--[^\\r\\n]*)|(/\\*[\\s\\S]*?\\*/)", " ");
                String finalWhereClause = stripSubsequentClauses(cleanWhereClause);
                if (isAlwaysTrueCondition(finalWhereClause)) {
                    return ValidationResult.invalid("WHERE条件疑似为恒真条件,操作被禁止");
                }
                if (!finalWhereClause.contains("=") && !finalWhereClause.toUpperCase().contains(" IN ")) {
                    return ValidationResult.invalid("高危操作的WHERE条件必须包含等号(=)或IN子句进行精确匹配,以防止大范围误操作。");
                }
                break;
            case "INSERT":
                break;
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateSelectTopN(String sql) {
        Matcher matcher = SELECT_TOP_PATTERN.matcher(sql);
        boolean hasSelect = false;
        while (matcher.find()) {
            if (matcher.group(1) != null || matcher.group(2) != null || matcher.group(3) != null || matcher.group(4) != null)
                continue;
            if (matcher.group(5) != null) {
                hasSelect = true;
                String topN = matcher.group(6);
                if (topN == null) {
                    return ValidationResult.invalid("所有查询(包括子查询、联合查询)必须包含 TOP N 语法");
                }
                try {
                    int n = Integer.parseInt(topN);
                    if (n <= 0) {
                        return ValidationResult.invalid("TOP N 中的 N 必须是一个正整数");
                    }
                    if (n > MAX_SELECT_ROWS) {
                        return ValidationResult.invalid("查询限制数量不能超过 " + MAX_SELECT_ROWS + " (检测到: " + n + ")");
                    }
                } catch (NumberFormatException e) {
                    return ValidationResult.invalid("无法解析 TOP N 中的数值");
                }
            }
        }
        if (!hasSelect) {
            return ValidationResult.invalid("未检测到有效的 SELECT 语句");
        }
        return ValidationResult.valid();
    }

    private String stripSubsequentClauses(String whereClause) {
        // 使用正则查找关键字,确保不匹配字符串或注释中的内容
        // Group 5 是查找的截断关键字
        Pattern p = Pattern.compile("(--[^\\r\\n]*)|(/\\*[\\s\\S]*?\\*/)|(N?'(?:''|[^'])*')|(\\[[^\\]]*\\])|\\b(ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT|OFFSET)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(whereClause);
        while (m.find()) {
            if (m.group(5) != null) {
                return whereClause.substring(0, m.start()).trim();
            }
        }
        return whereClause.trim();
    }

    private boolean isAlwaysTrueCondition(String whereClause) {
        for (Pattern pattern : ALWAYS_TRUE_PATTERNS) {
            if (pattern.matcher(whereClause).find()) {
                return true;
            }
        }
        return false;
    }
    /**测试SQL事例
     * -- 1. 缺少 TOP：全表查询风险
     * SELECT * FROM [dailytools].[dbo].[sys_oper_log];
     * -- 2. TOP 数量超标：超过 1000 条
     * SELECT TOP 1001 * FROM [dailytools].[dbo].[sys_oper_log];
     * -- 3. 子查询漏洞：外层有 TOP，但内层子查询没有 TOP (会导致内层全表扫描)
     * SELECT TOP 10 *
     * FROM (
     *     SELECT * FROM [dailytools].[dbo].[sys_oper_log]
     * ) AS T;
     * -- 4. 批量操作：通过分号分隔的多条语句
     * SELECT TOP 10 * FROM [dailytools].[dbo].[sys_oper_log];
     * SELECT TOP 10 * FROM [dailytools].[dbo].[sys_oper_log];
     * -- 5. 混合操作：查询语句中夹带删除操作 (有分号)
     * SELECT TOP 10 * FROM [dailytools].[dbo].[sys_oper_log]; DELETE FROM [dailytools].[dbo].[sys_oper_log];
     * -- 6. 混合操作：更新语句中夹带删除操作 (无分号)
     * UPDATE [dailytools].[dbo].[sys_oper_log] SET status = 1 WHERE oper_id = 1 DELETE FROM [dailytools].[dbo].[sys_oper_log];
     * -- 7. 危险操作：DROP 表
     * DROP TABLE [dailytools].[dbo].[sys_oper_log];
     * -- 8. 危险操作：RENAME (即使不常用，也应拦截)
     * -- SQL Server 使用 sp_rename, 但我们的关键字检测会拦截 RENAME
     * SELECT TOP 10 * FROM [dailytools].[dbo].[sys_oper_log] RENAME TO new_log;
     * -- 9. 不安全的 UPDATE：缺少 WHERE 子句 (全表更新)
     * UPDATE [dailytools].[dbo].[sys_oper_log] SET status = 1;
     * -- 10. 不安全的 UPDATE：WHERE 条件范围过大 (未使用 = 或 IN)
     * UPDATE [dailytools].[dbo].[sys_oper_log] SET status = 1 WHERE oper_id > 100;
     * -- 11. 不安全的 DELETE：恒真条件 (1=1)
     * DELETE FROM [dailytools].[dbo].[sys_oper_log] WHERE 1=1;
     * -- 12. 不安全的 DELETE：恒真条件变体 ('a'='a')
     * DELETE FROM [dailytools].[dbo].[sys_oper_log] WHERE 'a'='a';
     * -- 13. 高级绕过尝试：利用字符串内容干扰 WHERE 子句截断
     * DELETE FROM [dailytools].[dbo].[sys_oper_log] WHERE title = 'some title ORDER BY oper_id' OR 1=1;
     */
}