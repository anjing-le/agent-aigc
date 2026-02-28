package com.anjing.aigc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储配置属性
 * 
 * 从 application-local.yml 加载OSS/S3等存储配置
 * 
 * @author AI Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    
    /**
     * OSS/S3 配置
     */
    private OssConfig oss = new OssConfig();
    
    /**
     * OSS配置类
     */
    @Data
    public static class OssConfig {
        /**
         * 提供商: aliyun / tencent / aws
         */
        private String provider = "aliyun";
        
        /**
         * 端点
         */
        private String endpoint;
        
        /**
         * Access Key ID
         */
        private String accessKeyId;
        
        /**
         * Access Key Secret
         */
        private String accessKeySecret;
        
        /**
         * Bucket名称
         */
        private String bucketName;
        
        /**
         * CDN域名（可选）
         */
        private String cdnDomain;
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
    
    /**
     * 检查OSS配置是否有效
     */
    public boolean isOssConfigured() {
        return oss != null 
            && oss.getAccessKeyId() != null 
            && !oss.getAccessKeyId().isBlank()
            && !oss.getAccessKeyId().startsWith("<")
            && oss.getAccessKeySecret() != null
            && !oss.getAccessKeySecret().isBlank()
            && !oss.getAccessKeySecret().startsWith("<");
    }
}

