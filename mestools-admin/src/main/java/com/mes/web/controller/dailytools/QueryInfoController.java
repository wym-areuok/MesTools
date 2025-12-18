package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.core.page.TableDataInfo;
import com.mes.common.enums.BusinessType;
import com.mes.common.utils.SecurityUtils;
import com.mes.common.utils.poi.ExcelUtil;
import com.mes.system.domain.QueryInfo;
import com.mes.system.service.IQueryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-10
 * @Description: 资料查询的Controller
 */
@RestController
@RequestMapping("/dailytools/queryInfo")
public class QueryInfoController extends BaseController {

    @Autowired
    private IQueryInfoService queryInfoService;

    /**
     * 分页查询列表
     *
     * @param queryInfo
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(QueryInfo queryInfo) {
        startPage();
        List<QueryInfo> list = queryInfoService.selectQueryInfoList(queryInfo);
        return getDataTable(list);
    }

    /**
     * 获取详细信息
     *
     * @param infoId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:query')")
    @GetMapping(value = "/{infoId}")
    public AjaxResult getInfo(@PathVariable("infoId") Integer infoId) {
        return success(queryInfoService.selectQueryInfoByInfoId(infoId));
    }

    /**
     * 新增
     *
     * @param queryInfo
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:add')")
    @Log(title = "信息查询", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody QueryInfo queryInfo) {
        queryInfo.setCreateBy(SecurityUtils.getUsername());
        return toAjax(queryInfoService.insertQueryInfo(queryInfo));
    }

    /**
     * 修改
     *
     * @param queryInfo
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:edit')")
    @Log(title = "信息查询", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody QueryInfo queryInfo) {
        queryInfo.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(queryInfoService.updateQueryInfo(queryInfo));
    }

    /**
     * 删除-批量删除
     *
     * @param infoIds
     * @return
     */
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('dailyTools:queryInfo:remove')")
    @Log(title = "信息查询", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@PathVariable Integer[] infoIds) {
        return toAjax(queryInfoService.deleteQueryInfoByInfoIds(infoIds));
    }

    /**
     * 导出
     *
     * @param response
     * @param queryInfo
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:export')")
    @Log(title = "信息查询", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QueryInfo queryInfo) {
        List<QueryInfo> list = queryInfoService.selectQueryInfoList(queryInfo);
        ExcelUtil<QueryInfo> util = new ExcelUtil<>(QueryInfo.class);
        util.exportExcel(response, list, "资料库信息");
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:import')")
    @Log(title = "信息查询", businessType = BusinessType.EXPORT)
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<QueryInfo> util = new ExcelUtil<>(QueryInfo.class);
        util.importTemplateExcel(response, "资料库信息导入模板");
    }

    /**
     * 导入资料库信息
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:queryInfo:import')")
    @Log(title = "信息查询", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QueryInfo> util = new ExcelUtil<>(QueryInfo.class);
        List<QueryInfo> queryInfoList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = queryInfoService.importQueryInfo(queryInfoList, updateSupport, operName);
        return success(message);
    }
}
