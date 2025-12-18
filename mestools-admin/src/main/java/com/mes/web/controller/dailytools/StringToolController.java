package com.mes.web.controller.dailytools;

import com.mes.common.annotation.Log;
import com.mes.common.core.domain.AjaxResult;
import com.mes.common.enums.BusinessType;
import com.mes.common.utils.SecurityUtils;
import com.mes.system.service.IStringToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-11-21
 * @Description: 字符串工具的Controller
 */
@RestController
@RequestMapping("/dailytools/stringtool")
public class StringToolController {
    @Autowired
    private IStringToolService stringToolService;

    /**
     * 执行字符串处理操作(中小数量级数据)
     *
     * @param data
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:stringTool:execute')")
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody Map<String, Object> data) {
        try {
            String input = (String) data.get("input");
            if (input == null || input.trim().isEmpty()) {
                return AjaxResult.error("输入内容不能为空");
            }
            String result = stringToolService.execute(input.trim());
            return AjaxResult.success("处理成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 下载模版文件
     *
     * @param response
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:stringTool:download')")
    @Log(title = "字符串工具", businessType = BusinessType.EXPORT)
    @PostMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        stringToolService.downloadTemplate(response);
    }

    /**
     * 解析Excel写入数据库
     *
     * @param file
     * @return
     */
    @PreAuthorize("@ss.hasPermi('dailyTools:stringTool:upload')")
    @Log(title = "字符串工具", businessType = BusinessType.IMPORT)
    @PostMapping("/upload")
    public AjaxResult uploadAndProcessExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return AjaxResult.error("上传文件不能为空");
            }
            String username;
            Long userId;
            try {
                username = SecurityUtils.getUsername();
                userId = SecurityUtils.getUserId();
            } catch (Exception e) {
                return AjaxResult.error("未能获取到当前用户信息,请重新登录后重试");
            }
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".xlsx";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            // 在文件名中加入用户名,确保多用户环境下的文件唯一性
            String fileName = "excel_" + username + "_" + sdf.format(new Date()) + extension;
            String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
            File destFile = new File(filePath);
            file.transferTo(destFile);
            new Thread(() -> {
                try {
                    stringToolService.processExcelFile(filePath, userId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 处理完成后删除临时文件
                    if (destFile.exists()) {
                        destFile.delete();
                    }
                }
            }).start();
            return AjaxResult.success("文件上传成功,正在后台处理,请稍后查看string_tool_temp表的内容");
        } catch (IOException e) {
            return AjaxResult.error("文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("处理过程中发生错误：" + e.getMessage());
        }
    }
}