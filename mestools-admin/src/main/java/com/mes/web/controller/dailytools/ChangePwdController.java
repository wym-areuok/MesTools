package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.controller.BaseController;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.core.domain.model.LoginUser;
import com.mes.common.enums.BusinessType;
import com.mes.common.utils.SecurityUtils;
import com.mes.system.domain.dto.ChangePwdDTO;
import com.mes.system.service.IChangePwdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-07
 * @Description: 修改FIS账号密码的Controller
 */
@RestController
@RequestMapping("/dailytools/changePwd")
public class ChangePwdController extends BaseController {
    @Autowired
    private IChangePwdService changePwdService;

    /**
     * 修改当前用户密码
     *
     * @param pwdDTO
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:changePwd:loginFisNo')")
    @Log(title = "FisWeb改密-当前用户", businessType = BusinessType.UPDATE)
    @PutMapping("/current")
    public AjaxResult changeCurrentPwd(@Valid @RequestBody ChangePwdDTO pwdDTO) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = SecurityUtils.getLoginUser();
            String currentFisNumber = loginUser.getUser().getFisNumber();
            boolean result = changePwdService.changePwd(
                    currentFisNumber,
                    pwdDTO.getPassword(),
                    pwdDTO.getDbDataSource()
            );
            return result ? AjaxResult.success("密码修改成功") : AjaxResult.error("密码修改失败");
        } catch (Exception e) {
            return AjaxResult.error("密码修改异常：" + e.getMessage());
        }
    }

    /**
     * 修改其他用户密码
     *
     * @param pwdDTO
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:changePwd:otherFisNo')")
    @Log(title = "FisWeb改密-其他用户", businessType = BusinessType.UPDATE)
    @PutMapping("/other")
    public AjaxResult changeOtherPwd(@Valid @RequestBody ChangePwdDTO pwdDTO) {
        try {
            boolean result = changePwdService.changePwd(
                    pwdDTO.getFisNumber(),
                    pwdDTO.getPassword(),
                    pwdDTO.getDbDataSource()
            );
            return result ? AjaxResult.success("密码修改成功") : AjaxResult.error("密码修改失败");
        } catch (Exception e) {
            return AjaxResult.error("密码修改异常：" + e.getMessage());
        }
    }
}