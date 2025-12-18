package com.mes.system.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: weiyiming
 * @CreateTime: 2025-12-14
 * @Description: sql校验结果
 */
@Getter
@AllArgsConstructor
public class ValidationResult {
    /**
     * 验证结果
     */
    private boolean valid;
    /**
     * 提示信息
     */
    private String message;

    public static ValidationResult valid() {
        return new ValidationResult(true, "验证通过");
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
}
