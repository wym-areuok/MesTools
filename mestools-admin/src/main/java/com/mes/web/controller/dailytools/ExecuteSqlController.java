package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.enums.BusinessType;

import com.mes.system.domain.dto.ExecuteSqlDTO;
import com.mes.system.domain.ValidationResult;
import com.mes.system.service.IExecuteSqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
@RestController
@RequestMapping("/dailyTools/executeSql")
public class ExecuteSqlController extends BaseController {

    /**
     * SQL 验证
     */
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList("DROP", "TRUNCATE", "ALTER", "CREATE", "RENAME");
    private static final List<Pattern> ALWAYS_TRUE_PATTERNS = Arrays.asList(
            "\\s*1\\s*=\\s*1",
            "\\s*2\\s*>\\s*1",
            "'[^']*'\\s*=\\s*'[^']*'" // 更健壮的字符串比较
    ).stream().map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE)).collect(Collectors.toList());
    private static final int MAX_SELECT_ROWS = 1000;
    private static final Pattern TOP_N_PATTERN = Pattern.compile("SELECT\\s+(?:DISTINCT\\s+)?TOP\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    @Autowired
    private IExecuteSqlService sqlExecuteService;

    /**
     * 查询操作
     *
     * @param request
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:query')")
    @Log(title = "SQL执行工具", businessType = BusinessType.SELECT)
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
            if (future != null) {
                future.cancel(true);
            }
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
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:update')")
    @Log(title = "SQL执行工具", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
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
            // 在超时后,尝试中断后台线程以取消数据库查询
            if (future != null) {
                future.cancel(true);
            }
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
            // 在超时后,尝试中断后台线程以取消数据库查询
            if (future != null) {
                future.cancel(true);
            }
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
    @PreAuthorize("@ss.hasPermi('dailyTools:executeSql:delete')")
    @Log(title = "SQL执行工具", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
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
            // 在超时后,尝试中断后台线程以取消数据库查询
            if (future != null) {
                future.cancel(true);
            }
            return AjaxResult.error("SQL执行超时,请优化SQL语句或检查数据库状态");
        } catch (Exception e) {
            logger.error("删除SQL执行异常", e);
            return AjaxResult.error("删除执行失败：" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    private ValidationResult validate(String sql, String operationType) {
        if (!StringUtils.hasText(sql)) {
            return ValidationResult.invalid("SQL语句不能为空");
        }
        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();
        if (isBatchOperation(trimmedSql)) {
            return ValidationResult.invalid("不允许执行批量SQL操作");
        }
        if (!upperSql.startsWith(operationType)) {
            return ValidationResult.invalid(String.format("%s语句必须以%s开头", operationType, operationType));
        }
        if (!"INSERT".equals(operationType)) {
            for (String keyword : DANGEROUS_KEYWORDS) {
                if (upperSql.contains(" " + keyword + " ")) {
                    return ValidationResult.invalid("SQL语句包含危险关键字: " + keyword);
                }
            }
        }
        switch (operationType) {
            case "UPDATE":
            case "DELETE":
                if (!upperSql.contains(" WHERE ")) {
                    return ValidationResult.invalid(operationType + " 操作必须包含WHERE条件");
                }
                String whereClause = getWhereClause(upperSql);
                if (isAlwaysTrueCondition(whereClause)) {
                    return ValidationResult.invalid("WHERE条件疑似为恒真条件,操作被禁止");
                }
                if (!whereClause.contains("=") && !whereClause.contains(" IN ")) {
                    return ValidationResult.invalid("高危操作的WHERE条件必须包含等号(=)或IN子句进行精确匹配,以防止大范围误操作。");
                }
                break;
            case "SELECT":
                Matcher matcher = TOP_N_PATTERN.matcher(trimmedSql);
                if (!matcher.find()) {
                    return ValidationResult.invalid("查询语句必须使用 TOP N 语法 (例如: SELECT TOP 100 *)");
                }
                try {
                    int topValue = Integer.parseInt(matcher.group(1));
                    if (topValue <= 0) {
                        return ValidationResult.invalid("TOP N 中的 N 必须是一个正整数");
                    }
                    if (topValue > MAX_SELECT_ROWS) {
                        return ValidationResult.invalid("查询最多不能超过 " + MAX_SELECT_ROWS + " 条数据");
                    }
                } catch (NumberFormatException e) {
                    return ValidationResult.invalid("无法解析 TOP N 中的数值");
                }
                break;
            case "INSERT":
                break;
        }
        return ValidationResult.valid();
    }

    private boolean isBatchOperation(String sql) {
        // 检查是否包含多个SQL语句（以分号分隔且非空）
        return Arrays.stream(sql.split(";")).filter(s -> !s.trim().isEmpty()).count() > 1;
    }

    private String getWhereClause(String upperSql) {
        int whereIndex = upperSql.lastIndexOf(" WHERE ");
        if (whereIndex == -1) {
            return "";
        }
        String whereClause = upperSql.substring(whereIndex + 7);
        // 移除可能的ORDER BY, GROUP BY等后续子句
        String[] subsequentClauses = {"ORDER BY", "GROUP BY", "HAVING", "LIMIT", "OFFSET"};
        int firstSubsequentClauseIndex = -1;
        for (String clause : subsequentClauses) {
            int index = whereClause.indexOf(" " + clause + " ");
            if (index != -1 && (firstSubsequentClauseIndex == -1 || index < firstSubsequentClauseIndex)) {
                firstSubsequentClauseIndex = index;
            }
        }
        return (firstSubsequentClauseIndex != -1 ? whereClause.substring(0, firstSubsequentClauseIndex) : whereClause).trim();
    }

    private boolean isAlwaysTrueCondition(String whereClause) {
        for (Pattern pattern : ALWAYS_TRUE_PATTERNS) {
            if (pattern.matcher(whereClause).find()) {
                return true;
            }
        }
        return false;
    }
}