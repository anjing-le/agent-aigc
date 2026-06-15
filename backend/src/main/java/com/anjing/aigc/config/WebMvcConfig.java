package com.anjing.aigc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WebMvc 配置
 * 
 * <p>配置静态资源兼容映射。AIGC 私有文件访问优先通过 AIGC preview/download API。</p>
 *
 * @author AIGC Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AigcProperties aigcProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取本地存储配置
        AigcProperties.LocalStorageConfig localConfig = aigcProperties.getStorage().getLocal();
        
        if (localConfig.isEnabled() && localConfig.isStaticServingEnabled()) {
            String basePath = localConfig.getBasePath();
            Path absolutePath = Paths.get(basePath).toAbsolutePath();
            String resourceLocation = "file:" + absolutePath.toString() + "/";
            
            registry.addResourceHandler("/files/**")
                    .addResourceLocations(resourceLocation);
            
            log.info("AIGC 本地静态资源兼容映射已启用: /files/** -> {}", resourceLocation);
        } else if (localConfig.isEnabled()) {
            log.info("AIGC 本地静态资源兼容映射已关闭，请通过 AIGC preview/download API 访问私有文件");
        }
    }
}
