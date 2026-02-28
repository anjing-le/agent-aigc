package com.anjing.aigc.model.dto;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型信息DTO
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /** 模型ID */
    private String id;

    /** 模型名称 */
    private String name;

    /** 模型描述 */
    private String description;

    /** 支持的内容类型 */
    private ContentType contentType;

    /** 模型提供商 */
    private String provider;

    /** 是否可用 */
    private Boolean available;

    /** 模型图标 */
    private String icon;
}

