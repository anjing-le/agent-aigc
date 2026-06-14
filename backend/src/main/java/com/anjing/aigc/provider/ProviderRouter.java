package com.anjing.aigc.provider;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.service.AigcProviderRouteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 提供商路由器
 * 
 * <p>根据配置的 activeProvider 动态路由到对应的提供商实现。</p>
 * 
 * <h3>路由策略</h3>
 * <ul>
 *   <li>根据 aigc.image.active-provider 选择图片提供商</li>
 *   <li>根据 aigc.video.active-provider 选择视频提供商</li>
 *   <li>根据 aigc.audio.active-provider 选择音频提供商</li>
 * </ul>
 * 
 * <h3>架构说明</h3>
 * <p>我们只支持图片/视频/音频三种内容生成，文本不支持。</p>
 * <p>智能路由 Agent 的意图分析使用 OneRouter (gpt-4o-mini)，不通过 ProviderRouter。</p>
 * 
 * @author AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRouter {
    
    private final AigcProperties aigcProperties;
    private final AigcProviderRouteConfigService routeConfigService;
    
    // 注入所有提供商实现（只有图片/视频/音频）
    private final List<ImageGenerationProvider> imageProviders;
    private final List<VideoGenerationProvider> videoProviders;
    private final List<AudioGenerationProvider> audioProviders;
    
    @PostConstruct
    public void init() {
        log.info("========== Provider Router 初始化 ==========");
        log.info("图片提供商: {} 个已注册", imageProviders.size());
        for (ImageGenerationProvider p : imageProviders) {
            log.info("  - {} (type={}, available={})", p.getProviderName(), p.getProviderType(), p.isAvailable());
        }
        log.info("视频提供商: {} 个已注册", videoProviders.size());
        for (VideoGenerationProvider p : videoProviders) {
            log.info("  - {} (type={}, available={})", p.getProviderName(), p.getProviderType(), p.isAvailable());
        }
        log.info("音频提供商: {} 个已注册", audioProviders.size());
        for (AudioGenerationProvider p : audioProviders) {
            log.info("  - {} (type={}, available={})", p.getProviderName(), p.getProviderType(), p.isAvailable());
        }
        
        // 打印当前激活的提供商
        log.info("当前激活:");
        log.info("  图片: {}", routeConfigService.getActiveProvider(ContentType.IMAGE));
        log.info("  视频: {}", routeConfigService.getActiveProvider(ContentType.VIDEO));
        log.info("  音频: {}", routeConfigService.getActiveProvider(ContentType.AUDIO));
        
        // 打印 Google 配置状态
        log.info("Google 配置状态: isGoogleConfigured={}", aigcProperties.isGoogleConfigured());
        if (aigcProperties.getProviders().getGoogle().getApiKey() != null) {
            String key = aigcProperties.getProviders().getGoogle().getApiKey();
            log.info("  API Key: {}...{} (长度:{})", 
                    key.substring(0, Math.min(8, key.length())), 
                    key.length() > 4 ? key.substring(key.length() - 4) : "",
                    key.length());
        } else {
            log.warn("  API Key: null");
        }
        
        // 打印 Agent 使用的 LLM
        if (aigcProperties.isOneRouterConfigured()) {
            log.info("  Agent LLM: OneRouter ({})", aigcProperties.getProviders().getOnerouter().getModel());
        } else {
            log.warn("  Agent LLM: 未配置 OneRouter，将使用降级策略");
        }
    }
    
    /**
     * 获取激活的图片生成提供商
     */
    public ImageGenerationProvider getImageProvider() {
        return (ImageGenerationProvider) getProvider(ContentType.IMAGE);
    }
    
    /**
     * 获取激活的视频生成提供商
     */
    public VideoGenerationProvider getVideoProvider() {
        return (VideoGenerationProvider) getProvider(ContentType.VIDEO);
    }
    
    /**
     * 获取激活的音频生成提供商
     */
    public AudioGenerationProvider getAudioProvider() {
        return (AudioGenerationProvider) getProvider(ContentType.AUDIO);
    }

    /**
     * 按内容类型获取当前激活 Provider，用于任务执行与调用观测。
     */
    public ContentProvider getProvider(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> findProvider(imageProviders, routeConfigService.getActiveProvider(ContentType.IMAGE), "图片");
            case VIDEO -> findProvider(videoProviders, routeConfigService.getActiveProvider(ContentType.VIDEO), "视频");
            case AUDIO -> findProvider(audioProviders, routeConfigService.getActiveProvider(ContentType.AUDIO), "音频");
            case TEXT -> throw new IllegalStateException("文本生成暂未开放 Provider");
        };
    }

    private ContentProvider findProvider(List<? extends ContentProvider> providers, String activeProvider, String label) {
        return providers.stream()
                .filter(p -> matchesProvider(p, activeProvider))
                .filter(ContentProvider::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "没有可用的" + label + "生成提供商: " + activeProvider));
    }
    
    /**
     * 判断提供商是否匹配配置
     */
    private boolean matchesProvider(ContentProvider provider, String activeProvider) {
        if (activeProvider == null || activeProvider.isBlank()) {
            return true; // 未配置时使用第一个可用的
        }
        
        // 根据提供商类型匹配
        return switch (activeProvider.toLowerCase()) {
            case "google" -> provider.getProviderType() == ContentProvider.ProviderType.GOOGLE;
            case "openai" -> provider.getProviderType() == ContentProvider.ProviderType.OPENAI;
            case "stability" -> provider.getProviderType() == ContentProvider.ProviderType.STABILITY;
            default -> provider.getProviderName().toLowerCase().contains(activeProvider.toLowerCase());
        };
    }
    
    // ==================== 获取所有可用提供商（用于管理后台） ====================
    
    /**
     * 获取所有可用的图片提供商
     */
    public List<ImageGenerationProvider> getAvailableImageProviders() {
        return imageProviders.stream()
                .filter(ContentProvider::isAvailable)
                .toList();
    }

    public List<ImageGenerationProvider> getImageProviders() {
        return imageProviders;
    }
    
    /**
     * 获取所有可用的视频提供商
     */
    public List<VideoGenerationProvider> getAvailableVideoProviders() {
        return videoProviders.stream()
                .filter(ContentProvider::isAvailable)
                .toList();
    }

    public List<VideoGenerationProvider> getVideoProviders() {
        return videoProviders;
    }
    
    /**
     * 获取所有可用的音频提供商
     */
    public List<AudioGenerationProvider> getAvailableAudioProviders() {
        return audioProviders.stream()
                .filter(ContentProvider::isAvailable)
                .toList();
    }

    public List<AudioGenerationProvider> getAudioProviders() {
        return audioProviders;
    }
}
