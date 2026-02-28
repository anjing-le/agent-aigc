package com.anjing.aigc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于 OneRouter 意图分析）
 * 
 * 注意：OneRouter 是国内服务，不需要代理！
 * Google API 的代理在各 Provider 中单独配置。
 * 
 * @author AI Team
 */
@Slf4j
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(AigcProperties aigcProperties) {
        var oneRouterConfig = aigcProperties.getProviders().getOnerouter();
        int timeout = oneRouterConfig.getTimeout();
        
        // OneRouter 不需要代理，直接访问
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        
        log.info("[RestTemplate] 初始化完成 (OneRouter 直连), timeout={}ms", timeout);
        
        return new RestTemplate(factory);
    }
}
