package com.anjing.aigc.model.dto;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    /** 当前内容类型配置的激活 Provider */
    private String activeProvider;

    /** 路由配置来源：configuration 或 database */
    private String routeConfigSource;

    /** Provider 凭证来源：configuration、database 或 missing */
    private String credentialSource;

    /** Provider 凭证更新时间，仅数据库来源时返回 */
    private String credentialUpdatedAt;

    /** 是否为当前激活 Provider */
    private Boolean active;

    /** 是否可用 */
    private Boolean available;

    /** 配置中的模型名称 */
    private String configuredModel;

    /** 默认生成参数 */
    private Map<String, Object> defaultParams;

    /** 默认参数模板来源：configuration、database、not-required 或 missing */
    private String paramConfigSource;

    /** 默认参数模板更新时间，仅数据库来源时返回 */
    private String paramConfigUpdatedAt;

    /** 缺失配置说明 */
    private String missingConfig;

    /** 状态说明 */
    private String statusReason;

    /** 模型图标 */
    private String icon;
}
