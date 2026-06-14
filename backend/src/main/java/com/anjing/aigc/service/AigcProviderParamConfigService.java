package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcProviderParamConfig;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.repository.AigcProviderParamConfigRepository;
import com.anjing.model.errorcode.AigcErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider 默认参数模板配置来源。
 *
 * <p>优先读取数据库中的页面配置；没有页面配置时回落到环境变量和 yml。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderParamConfigService {

    public static final String GOOGLE_PROVIDER_KEY = "google";

    private static final String UPDATED_BY_RUNTIME_PAGE = "runtime-page";
    private static final List<String> IMAGE_KEYS = List.of("aspectRatio", "imageSize", "timeoutMs");
    private static final List<String> VIDEO_KEYS = List.of("aspectRatio", "resolution", "duration", "timeoutMs");
    private static final List<String> AUDIO_KEYS = List.of("voice", "bpm", "temperature", "timeoutMs");

    private final AigcProperties aigcProperties;
    private final AigcProviderParamConfigRepository paramConfigRepository;

    public Map<String, Object> getDefaultParams(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER
                && provider.getProviderName().toLowerCase().contains("mock")) {
            return Map.of("mode", "local-demo", "externalKey", false);
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return Map.of();
        }
        return getGoogleDefaultParams(contentType);
    }

    public Map<String, Object> getGoogleDefaultParams(ContentType contentType) {
        return paramConfigRepository.findByContentTypeAndProviderKey(contentType, GOOGLE_PROVIDER_KEY)
                .map(AigcProviderParamConfig::getDefaultParams)
                .filter(params -> params != null && !params.isEmpty())
                .orElseGet(() -> getConfiguredGoogleDefaultParams(contentType));
    }

    public String getParamConfigSource(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER
                && provider.getProviderName().toLowerCase().contains("mock")) {
            return "not-required";
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return "missing";
        }
        return paramConfigRepository.findByContentTypeAndProviderKey(contentType, GOOGLE_PROVIDER_KEY)
                .map(config -> "database")
                .orElse("configuration");
    }

    public LocalDateTime getParamConfigUpdatedAt(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return null;
        }
        return paramConfigRepository.findByContentTypeAndProviderKey(contentType, GOOGLE_PROVIDER_KEY)
                .map(AigcProviderParamConfig::getUpdatedAt)
                .orElse(null);
    }

    @Transactional
    public AigcProviderParamConfig saveGoogleDefaultParams(ContentType contentType, Map<String, Object> defaultParams,
            ContentProvider provider) {
        Map<String, Object> normalizedParams = normalizeGoogleParams(contentType, defaultParams);
        AigcProviderParamConfig config = paramConfigRepository.findByContentTypeAndProviderKey(
                        contentType, GOOGLE_PROVIDER_KEY)
                .orElseGet(AigcProviderParamConfig::new);
        config.setContentType(contentType);
        config.setProviderKey(GOOGLE_PROVIDER_KEY);
        config.setProviderName(provider.getProviderName());
        config.setProviderType(provider.getProviderType().name());
        config.setDefaultParams(normalizedParams);
        config.setUpdatedBy(UPDATED_BY_RUNTIME_PAGE);
        return paramConfigRepository.save(config);
    }

    public String getGoogleImageAspectRatio() {
        return stringParam(ContentType.IMAGE, "aspectRatio",
                aigcProperties.getImage().getGoogle().getDefaultAspectRatio());
    }

    public String getGoogleImageSize() {
        return stringParam(ContentType.IMAGE, "imageSize",
                aigcProperties.getImage().getGoogle().getDefaultImageSize());
    }

    public String getGoogleVideoAspectRatio() {
        return stringParam(ContentType.VIDEO, "aspectRatio",
                aigcProperties.getVideo().getGoogle().getDefaultAspectRatio());
    }

    public String getGoogleVideoResolution() {
        return stringParam(ContentType.VIDEO, "resolution",
                aigcProperties.getVideo().getGoogle().getDefaultResolution());
    }

    public int getGoogleVideoDuration() {
        return intParam(ContentType.VIDEO, "duration",
                aigcProperties.getVideo().getGoogle().getDefaultDuration());
    }

    public String getGoogleAudioVoice() {
        return stringParam(ContentType.AUDIO, "voice",
                aigcProperties.getAudio().getGoogle().getDefaultVoice());
    }

    public int getGoogleAudioBpm() {
        return intParam(ContentType.AUDIO, "bpm",
                aigcProperties.getAudio().getGoogle().getDefaultBpm());
    }

    public double getGoogleAudioTemperature() {
        return doubleParam(ContentType.AUDIO, "temperature",
                aigcProperties.getAudio().getGoogle().getDefaultTemperature());
    }

    private Map<String, Object> getConfiguredGoogleDefaultParams(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> orderedMap(
                    "aspectRatio", aigcProperties.getImage().getGoogle().getDefaultAspectRatio(),
                    "imageSize", aigcProperties.getImage().getGoogle().getDefaultImageSize(),
                    "timeoutMs", aigcProperties.getImage().getGoogle().getTimeout()
            );
            case VIDEO -> orderedMap(
                    "aspectRatio", aigcProperties.getVideo().getGoogle().getDefaultAspectRatio(),
                    "resolution", aigcProperties.getVideo().getGoogle().getDefaultResolution(),
                    "duration", aigcProperties.getVideo().getGoogle().getDefaultDuration(),
                    "timeoutMs", aigcProperties.getVideo().getGoogle().getTimeout()
            );
            case AUDIO -> orderedMap(
                    "voice", aigcProperties.getAudio().getGoogle().getDefaultVoice(),
                    "bpm", aigcProperties.getAudio().getGoogle().getDefaultBpm(),
                    "temperature", aigcProperties.getAudio().getGoogle().getDefaultTemperature(),
                    "timeoutMs", aigcProperties.getAudio().getGoogle().getTimeout()
            );
            case TEXT -> Map.of();
        };
    }

    private Map<String, Object> normalizeGoogleParams(ContentType contentType, Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, "默认参数不能为空");
        }
        Map<String, Object> baseParams = getConfiguredGoogleDefaultParams(contentType);
        List<String> allowedKeys = allowedKeys(contentType);
        Map<String, Object> normalized = new LinkedHashMap<>();

        for (String key : allowedKeys) {
            Object rawValue = input.containsKey(key) ? input.get(key) : baseParams.get(key);
            normalized.put(key, normalizeValue(contentType, key, rawValue));
        }
        return normalized;
    }

    private Object normalizeValue(ContentType contentType, String key, Object rawValue) {
        if (rawValue == null || String.valueOf(rawValue).isBlank()) {
            throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, key + " 不能为空");
        }
        return switch (key) {
            case "timeoutMs", "duration", "bpm" -> parseInt(key, rawValue);
            case "temperature" -> parseDouble(key, rawValue);
            case "aspectRatio" -> validateStringOption(key, rawValue, optionsFor(contentType, key));
            case "imageSize" -> validateStringOption(key, rawValue, Set.of("1K", "2K", "4K"));
            case "resolution" -> validateStringOption(key, rawValue, Set.of("720p", "1080p"));
            case "voice" -> validateStringOption(key, rawValue, Set.of("Kore", "Aoede", "Fenrir", "Puck", "Charon"));
            default -> throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, "不支持的默认参数: " + key);
        };
    }

    private List<String> allowedKeys(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> IMAGE_KEYS;
            case VIDEO -> VIDEO_KEYS;
            case AUDIO -> AUDIO_KEYS;
            case TEXT -> List.of();
        };
    }

    private Set<String> optionsFor(ContentType contentType, String key) {
        if (!"aspectRatio".equals(key)) {
            return Set.of();
        }
        return switch (contentType) {
            case IMAGE -> Set.of("1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9");
            case VIDEO -> Set.of("16:9", "9:16");
            default -> Set.of();
        };
    }

    private String validateStringOption(String key, Object rawValue, Set<String> allowedOptions) {
        String value = String.valueOf(rawValue).trim();
        if (!allowedOptions.isEmpty() && !allowedOptions.contains(value)) {
            throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, key + " 不支持: " + value);
        }
        return value;
    }

    private int parseInt(String key, Object rawValue) {
        try {
            int value = rawValue instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(rawValue));
            if (value <= 0) {
                throw new NumberFormatException("must be positive");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, key + " 必须是正整数");
        }
    }

    private double parseDouble(String key, Object rawValue) {
        try {
            double value = rawValue instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(rawValue));
            if (value <= 0) {
                throw new NumberFormatException("must be positive");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new AigcException(AigcErrorCode.GENERATION_PARAM_INVALID, key + " 必须是正数");
        }
    }

    private String stringParam(ContentType contentType, String key, String fallback) {
        Object value = getGoogleDefaultParams(contentType).get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private int intParam(ContentType contentType, String key, int fallback) {
        Object value = getGoogleDefaultParams(contentType).get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double doubleParam(ContentType contentType, String key, double fallback) {
        Object value = getGoogleDefaultParams(contentType).get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
