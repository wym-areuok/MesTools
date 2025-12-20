package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.core.page.TableDataInfo;
import com.mes.common.enums.BusinessType;

import com.mes.system.domain.dto.JumpStationDTO;
import com.mes.system.service.IJumpStationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-27
 * @Description: 板卡跳站的Controller
 */
@Api(tags = "板卡跳站")
@RestController
@RequestMapping("/dailytools/jumpStation")
public class JumpStationController extends BaseController {
    @Autowired
    private IJumpStationService jumpStationService;

    /**
     * 获取站点List name-code
     *
     * @param jumpType
     * @return
     */
    @ApiOperation("获取站点列表")
    @GetMapping("/getStationList")
    public AjaxResult getStationList(@RequestParam String jumpType) {
        try {
            List<Map<String, Object>> stationList = jumpStationService.getStationList(jumpType);
            return AjaxResult.success(stationList);
        } catch (Exception e) {
            logger.error("获取站点列表失败: ", e);
            return AjaxResult.error("获取站点列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询SN的信息
     *
     * @param queryDTO
     * @return
     */
    @ApiOperation("查询SN信息")
    @PreAuthorize("@ss.hasPermi('dailyTools:jumpStation:query')")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody JumpStationDTO queryDTO) {
        try {
            startPage();
            List<Map<String, Object>> list = jumpStationService.list(queryDTO.getSnList(), queryDTO.getDbDataSource(), queryDTO.getJumpType());
            return getDataTable(list);
        } catch (Exception e) {
            logger.error("查询SN信息失败: ", e);
            throw new RuntimeException("查询SN信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行跳站
     *
     * @param jsDTO
     * @return
     */
    @ApiOperation("执行跳站")
    @Log(title = "板卡跳站", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('dailyTools:jumpStation:execute')")
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody JumpStationDTO jsDTO) {
        try {
            if (jsDTO.getSnList() == null || jsDTO.getSnList().isEmpty()) {
                return AjaxResult.error("SN列表不能为空");
            }
            if (jsDTO.getDbDataSource() == null || jsDTO.getDbDataSource().isEmpty()) {
                return AjaxResult.error("数据源不能为空");
            }
            if (jsDTO.getJumpType() == null || jsDTO.getJumpType().isEmpty()) {
                return AjaxResult.error("跳站类型不能为空");
            }
            if (jsDTO.getStation() == null || jsDTO.getStation().isEmpty()) {
                return AjaxResult.error("目标站点不能为空");
            }
            if (jsDTO.getRemark() == null || jsDTO.getRemark().isEmpty()) {
                return AjaxResult.error("备注不能为空");
            }
            List<Map<String, Object>> result = jumpStationService.execute(jsDTO.getSnList(), jsDTO.getDbDataSource(), jsDTO.getJumpType(), jsDTO.getStation(), jsDTO.getRemark());
            return AjaxResult.success("操作完成", result);
        } catch (Exception e) {
            logger.error("跳站操作失败: ", e);
            return AjaxResult.error("跳站失败: " + e.getMessage());
        }
    }
}