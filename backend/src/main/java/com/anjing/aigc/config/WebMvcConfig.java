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
 * <p>配置静态资源映射，用于访问生成的图片、视频、音频等文件</p>
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
        
        if (localConfig.isEnabled()) {
            // 将 /files/** 映射到本地存储目录
            String basePath = localConfig.getBasePath();
            Path absolutePath = Paths.get(basePath).toAbsolutePath();
            String resourceLocation = "file:" + absolutePath.toString() + "/";
            
            registry.addResourceHandler("/files/**")
                    .addResourceLocations(resourceLocation);
            
            log.info("✅ 静态资源映射配置完成: /files/** -> {}", resourceLocation);
        }
    }
}

