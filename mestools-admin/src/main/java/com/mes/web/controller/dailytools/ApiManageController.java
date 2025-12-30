package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.core.page.TableDataInfo;
import com.mes.common.enums.BusinessType;
import com.mes.common.utils.SecurityUtils;
import com.mes.system.domain.ApiManageHistory;
import com.mes.system.domain.ApiManageItem;
import com.mes.system.domain.dto.ProxyRequestDto;
import com.mes.system.service.IApiManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "接口管理")
@RestController
@RequestMapping("/dailytools/apiManage")
public class ApiManageController extends BaseController {

    @Autowired
    private IApiManageService apiManageService;

    // --- 接口树管理 ---

    @GetMapping("/tree")
    @ApiOperation("获取接口树")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult tree() {
        List<ApiManageItem> list = apiManageService.selectApiTree();
        return AjaxResult.success(list);
    }

    @GetMapping("/{itemId}")
    @ApiOperation("获取接口详情")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId) {
        ApiManageItem item = apiManageService.selectApiManageItemById(itemId);
        if (item == null) {
            return AjaxResult.error("接口不存在或已被删除");
        }
        return AjaxResult.success(item);
    }

    @PostMapping
    @ApiOperation("新增接口/目录")
    @Log(title = "接口管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:insert')")
    public AjaxResult add(@RequestBody ApiManageItem apiManageItem) {
        apiManageItem.setCreateBy(getUsername());
        return toAjax(apiManageService.insertApiManageItem(apiManageItem));
    }

    @PutMapping
    @ApiOperation("修改接口/目录")
    @Log(title = "接口管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult edit(@RequestBody ApiManageItem apiManageItem) {
        apiManageItem.setUpdateBy(getUsername());
        return toAjax(apiManageService.updateApiManageItem(apiManageItem));
    }

    @DeleteMapping("/{itemId}")
    @ApiOperation("删除接口/目录")
    @Log(title = "接口管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:remove')")
    public AjaxResult remove(@PathVariable("itemId") Long itemId) {
        return toAjax(apiManageService.deleteApiManageItemById(itemId));
    }

    @PutMapping("/lock/{itemId}/{isLocked}")
    @ApiOperation("锁定/解锁接口")
    @Log(title = "接口管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult toggleLock(@PathVariable("itemId") Long itemId, @PathVariable("isLocked") Integer isLocked) {
        return toAjax(apiManageService.toggleLock(itemId, isLocked));
    }

    // --- 环境管理 ---

    @GetMapping("/env/list")
    @ApiOperation("获取环境列表")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult listEnv() {
        return AjaxResult.success(apiManageService.selectEnvList());
    }

    @PostMapping("/env/batch")
    @ApiOperation("批量保存环境")
    @Log(title = "接口管理-环境配置", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:edit')")
    public AjaxResult saveEnvList(@RequestBody List<ApiManageItem> envList) {
        String username = SecurityUtils.getUsername();
        for (ApiManageItem item : envList) {
            item.setCreateBy(username);
        }
        apiManageService.saveEnvList(envList);
        return AjaxResult.success();
    }

    // --- 历史记录 ---

    @GetMapping("/history/list")
    @ApiOperation("获取历史记录")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public TableDataInfo listHistory(ApiManageHistory history) {
        startPage();
        List<ApiManageHistory> list = apiManageService.selectHistoryList(history);
        return getDataTable(list);
    }

    @PostMapping("/history")
    @ApiOperation("新增历史记录")
    @Log(title = "接口管理-历史记录", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:insert')")
    public AjaxResult addHistory(@RequestBody ApiManageHistory history) {
        return toAjax(apiManageService.insertHistory(history));
    }

    // --- 代理请求 ---

    @PostMapping("/proxy")
    @ApiOperation("发送代理请求")
    @Log(title = "接口管理-代理请求", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:list')")
    public AjaxResult proxyRequest(@RequestBody ProxyRequestDto proxyRequest) {
        Map<String, Object> result = apiManageService.proxyRequest(proxyRequest);
        return AjaxResult.success(result);
    }

    // --- 导入导出 ---

    @GetMapping("/export")
    @ApiOperation("导出备份数据")
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:export')")
    public AjaxResult export() {
        return AjaxResult.success(apiManageService.exportData());
    }

    @PostMapping("/import")
    @ApiOperation("导入备份数据")
    @Log(title = "接口管理-导入", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('dailyTools:apiManage:import')")
    public AjaxResult importData(@RequestBody Map<String, Object> data) {
        apiManageService.importData(data);
        return AjaxResult.success();
    }
}
