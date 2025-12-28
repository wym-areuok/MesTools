package com.mes.web.controller.dailytools;

import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.core.page.TableDataInfo;
import com.mes.system.domain.ApiManageHistory;
import com.mes.system.domain.ApiManageItem;
import com.mes.system.domain.dto.ProxyRequestDto;
import com.mes.system.service.IApiManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-26
 * @Description: 接口管理
 */
@RestController
@RequestMapping("/dailytools/apiManage")
public class ApiManageController extends BaseController {

    @Autowired
    private IApiManageService apiManageService;

    // --- 接口树管理 ---

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult tree() {
        List<ApiManageItem> list = apiManageService.selectApiTree();
        return AjaxResult.success(list);
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId) {
        return AjaxResult.success(apiManageService.selectApiManageItemById(itemId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:insert')")
    public AjaxResult add(@RequestBody ApiManageItem apiManageItem) {
        apiManageItem.setCreateBy(getUsername());
        return toAjax(apiManageService.insertApiManageItem(apiManageItem));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult edit(@RequestBody ApiManageItem apiManageItem) {
        apiManageItem.setUpdateBy(getUsername());
        return toAjax(apiManageService.updateApiManageItem(apiManageItem));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:remove')")
    public AjaxResult remove(@PathVariable("itemId") Long itemId) {
        return toAjax(apiManageService.deleteApiManageItemById(itemId));
    }

    @PutMapping("/lock/{itemId}/{isLocked}")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult toggleLock(@PathVariable("itemId") Long itemId, @PathVariable("isLocked") Integer isLocked) {
        return toAjax(apiManageService.toggleLock(itemId, isLocked));
    }

    // --- 环境管理 ---

    @GetMapping("/env/list")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult listEnv() {
        return AjaxResult.success(apiManageService.selectEnvList());
    }

    @PostMapping("/env/batch")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult saveEnvList(@RequestBody List<ApiManageItem> envList) {
        apiManageService.saveEnvList(envList);
        return AjaxResult.success();
    }

    // --- 历史记录 ---

    @GetMapping("/history/list")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public TableDataInfo listHistory(ApiManageHistory history) {
        startPage();
        List<ApiManageHistory> list = apiManageService.selectHistoryList(history);
        return getDataTable(list);
    }

    @PostMapping("/history")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:insert')")
    public AjaxResult addHistory(@RequestBody ApiManageHistory history) {
        return toAjax(apiManageService.insertHistory(history));
    }

    // --- 代理请求 ---

    @PostMapping("/proxy")
    public AjaxResult proxyRequest(@RequestBody ProxyRequestDto proxyRequest) {
        Map<String, Object> result = apiManageService.proxyRequest(proxyRequest);
        return AjaxResult.success(result);
    }

    // --- 导入导出 ---

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:export')")
    public AjaxResult export() {
        return AjaxResult.success(apiManageService.exportData());
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:import')")
    public AjaxResult importData(@RequestBody Map<String, Object> data) {
        apiManageService.importData(data);
        return AjaxResult.success();
    }
}
