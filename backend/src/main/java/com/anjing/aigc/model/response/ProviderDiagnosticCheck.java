package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider 探测检查项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDiagnosticCheck {

    /** 检查项标识 */
    private String id;

    /** 展示名称 */
    private String label;

    /** 检查状态：PASS、WARN、FAIL */
    private String status;

    /** 检查结果说明 */
    private String message;
}
