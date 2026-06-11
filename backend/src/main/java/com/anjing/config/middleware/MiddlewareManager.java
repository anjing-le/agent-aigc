package com.anjing.config.middleware;

import com.fasterxml.jackson.annotation.JsonValue;
import com.anjing.config.properties.FeatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎛️ 中间件管理器
 * 
 * <p>统一管理各种中间件和可选能力的配置状态检查</p>
 * 
 * <h3>🎯 核心功能：</h3>
 * <ul>
 *   <li>🔍 启动时状态检查</li>
 *   <li>📊 功能状态统计</li>
 *   <li>📋 清晰的状态报告</li>
 * </ul>
 * 
 * <h3>📌 设计理念：</h3>
 * <ul>
 *   <li>✅ disabled/configured/ready/degraded 状态清晰</li>
 *   <li>🚫 默认不主动连接外部中间件</li>
 *   <li>💡 适合作为未来健康检查和运维页面的基础</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 2.1
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MiddlewareManager {

    private final FeatureProperties featureProperties;
    private final Environment environment;

    /**
     * 中间件状态枚举
     */
    public enum MiddlewareStatus {
        DISABLED("disabled", "○", "已禁用"),
        CONFIGURED("configured", "◐", "已配置"),
        READY("ready", "●", "已就绪"),
        DEGRADED("degraded", "△", "已降级");

        private final String code;
        private final String icon;
        private final String description;

        MiddlewareStatus(String code, String icon, String description) {
            this.code = code;
            this.icon = icon;
            this.description = description;
        }

        @JsonValue
        public String getCode() { return code; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    /**
     * 中间件信息 - 包含版本信息
     */
    public static class MiddlewareInfo {
        private final String name;
        private final MiddlewareStatus status;
        private final String version;
        private final String details;

        public MiddlewareInfo(String name, MiddlewareStatus status, String version, String details) {
            this.name = name;
            this.status = status;
            this.version = version;
            this.details = details;
        }

        public String getName() { return name; }
        public MiddlewareStatus getStatus() { return status; }
        public String getStatusCode() { return status.getCode(); }
        public String getStatusDescription() { return status.getDescription(); }
        public boolean isEnabled() { return status != MiddlewareStatus.DISABLED; }
        public String getVersion() { return version; }
        public String getDetails() { return details; }
    }

    /**
     * 中间件状态摘要。
     */
    public static class MiddlewareSummary {
        private final int total;
        private final int enabled;
        private final Map<String, Long> byStatus;

        public MiddlewareSummary(int total, int enabled, Map<String, Long> byStatus) {
            this.total = total;
            this.enabled = enabled;
            this.byStatus = byStatus;
        }

        public int getTotal() { return total; }
        public int getEnabled() { return enabled; }
        public Map<String, Long> getByStatus() { return byStatus; }
    }

    /**
     * 可选能力状态报告。
     */
    public static class MiddlewareStatusReport {
        private final MiddlewareStatus status;
        private final MiddlewareSummary summary;
        private final List<MiddlewareInfo> features;

        public MiddlewareStatusReport(MiddlewareStatus status, MiddlewareSummary summary, List<MiddlewareInfo> features) {
            this.status = status;
            this.summary = summary;
            this.features = features;
        }

        public MiddlewareStatus getStatus() { return status; }
        public String getStatusCode() { return status.getCode(); }
        public String getStatusDescription() { return status.getDescription(); }
        public MiddlewareSummary getSummary() { return summary; }
        public List<MiddlewareInfo> getFeatures() { return features; }
    }

    /**
     * 应用启动完成后执行健康检查
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady()
    {
        List<MiddlewareInfo> middlewareList = currentStatus();
        printMiddlewareStatus(middlewareList);
        printFeatureSummary();
    }

    /**
     * 获取当前能力状态。
     */
    public List<MiddlewareInfo> currentStatus() {
        List<MiddlewareInfo> middlewareList = new ArrayList<>();

        // Redis健康检查
        middlewareList.add(checkRedis());
        
        // 缓存健康检查
        middlewareList.add(checkCache());
        
        // 分布式锁健康检查
        middlewareList.add(checkDistributedLock());
        
        // 数据库健康检查
        middlewareList.add(checkDatabase());
        
        // Kafka健康检查
        middlewareList.add(checkKafka());
        
        // MinIO健康检查
        middlewareList.add(checkMinio());
        
        // OSS健康检查
        middlewareList.add(checkOss());
        
        // 监控健康检查
        middlewareList.add(checkMonitoring());

        return middlewareList;
    }

    /**
     * 获取面向接口和运维页面的状态报告。
     */
    public MiddlewareStatusReport statusReport() {
        List<MiddlewareInfo> middlewareList = currentStatus();
        return new MiddlewareStatusReport(
                overallStatus(middlewareList),
                summary(middlewareList),
                middlewareList
        );
    }

    public MiddlewareStatus overallStatus(List<MiddlewareInfo> middlewareList) {
        boolean hasDegraded = middlewareList.stream()
                .anyMatch(info -> info.getStatus() == MiddlewareStatus.DEGRADED);
        if (hasDegraded) {
            return MiddlewareStatus.DEGRADED;
        }

        boolean hasConfigured = middlewareList.stream()
                .anyMatch(info -> info.getStatus() == MiddlewareStatus.CONFIGURED);
        return hasConfigured ? MiddlewareStatus.CONFIGURED : MiddlewareStatus.READY;
    }

    public MiddlewareSummary summary(List<MiddlewareInfo> middlewareList) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (MiddlewareStatus status : MiddlewareStatus.values()) {
            long count = middlewareList.stream()
                    .filter(info -> info.getStatus() == status)
                    .count();
            byStatus.put(status.getCode(), count);
        }

        int enabled = (int) middlewareList.stream()
                .filter(MiddlewareInfo::isEnabled)
                .count();
        return new MiddlewareSummary(middlewareList.size(), enabled, byStatus);
    }

    /**
     * Redis健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkRedis() {
        if (!featureProperties.getRedis().isEnabled()) {
            return new MiddlewareInfo("Redis", MiddlewareStatus.DISABLED, "7.2.x", "功能已禁用");
        }

        if (!StringUtils.hasText(featureProperties.getRedis().getHost()) || featureProperties.getRedis().getPort() <= 0) {
            return new MiddlewareInfo("Redis", MiddlewareStatus.DEGRADED, "7.2.x", "Redis 已启用，但 host/port 配置不完整");
        }

        String details = String.format("%s:%d (超时:%dms)", 
            featureProperties.getRedis().getHost(),
            featureProperties.getRedis().getPort(),
            featureProperties.getRedis().getTimeout());
        return new MiddlewareInfo("Redis", MiddlewareStatus.CONFIGURED, "7.2.x", details);
    }

    /**
     * 缓存健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkCache() {
        if (!featureProperties.getCache().isEnabled()) {
            return new MiddlewareInfo("Cache", MiddlewareStatus.DISABLED, "Spring 3.4.5", "缓存功能已禁用");
        }

        String cacheType = featureProperties.getCache().getType();
        if ("redis".equalsIgnoreCase(cacheType) && !featureProperties.getRedis().isEnabled()) {
            return new MiddlewareInfo("Cache", MiddlewareStatus.DEGRADED, "Spring 3.4.5", "缓存类型为 redis，但 Redis 已禁用");
        }

        String details = String.format("类型:%s | TTL:%ds", 
            cacheType, featureProperties.getCache().getDefaultTtl());
        
        MiddlewareStatus status = "memory".equalsIgnoreCase(cacheType) ? MiddlewareStatus.READY : MiddlewareStatus.CONFIGURED;
        return new MiddlewareInfo("Cache", status, "Spring 3.4.5", details);
    }

    /**
     * 分布式锁健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkDistributedLock() {
        if (!featureProperties.getDistributedLock().isEnabled()) {
            return new MiddlewareInfo("DistributedLock", MiddlewareStatus.DISABLED, "Local / Redisson", "分布式锁已禁用");
        }

        String provider = featureProperties.getDistributedLock().getProvider();
        String version = "local".equalsIgnoreCase(provider) ? "Local JVM" : "Redisson 3.35.0";
        if ("redisson".equalsIgnoreCase(provider) && !featureProperties.getRedis().isEnabled()) {
            return new MiddlewareInfo("DistributedLock", MiddlewareStatus.DEGRADED, version, "Redisson 锁已启用，但 Redis 已禁用");
        }

        String details = String.format("提供者:%s | 等待:%ds | 过期:%ds", 
            provider,
            featureProperties.getDistributedLock().getDefaultWaitTime(),
            featureProperties.getDistributedLock().getDefaultExpireTime());

        MiddlewareStatus status = "local".equalsIgnoreCase(provider) ? MiddlewareStatus.READY : MiddlewareStatus.CONFIGURED;
        return new MiddlewareInfo("DistributedLock", status, version, details);
    }

    /**
     * 数据库健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkDatabase() {
        if (!featureProperties.getDatabase().isEnabled()) {
            return new MiddlewareInfo("Database", MiddlewareStatus.DISABLED, databaseVersionLabel(), "数据库功能已禁用");
        }

        List<String> features = new ArrayList<>();
        if (featureProperties.getDatabase().isShowSql()) features.add("SQL日志");
        if (featureProperties.getDatabase().isEnableAuditing()) features.add("JPA审计");
        if (featureProperties.getDatabase().isEnablePoolMonitoring()) features.add("连接池监控");
        features.add("驱动:" + databaseDriverLabel());

        String details = features.isEmpty() ? "无附加特性" : "特性: " + String.join(", ", features);
        return new MiddlewareInfo("Database", MiddlewareStatus.READY, databaseVersionLabel(), details);
    }

    private String databaseVersionLabel() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (url.startsWith("jdbc:h2:")) {
            return "H2 (MySQL mode)";
        }
        if (url.startsWith("jdbc:mysql:")) {
            return "MySQL 8.2.0";
        }
        return "JDBC";
    }

    private String databaseDriverLabel() {
        String driver = environment.getProperty("spring.datasource.driver-class-name", "");
        if (!StringUtils.hasText(driver)) {
            return "auto";
        }
        if (driver.contains("h2")) {
            return "H2 Driver";
        }
        if (driver.contains("mysql")) {
            return "MySQL Driver";
        }
        int lastDot = driver.lastIndexOf('.');
        return lastDot >= 0 ? driver.substring(lastDot + 1) : driver;
    }

    /**
     * Kafka健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkKafka() {
        if (!featureProperties.getMiddleware().getKafka().isEnabled()) {
            return new MiddlewareInfo("Kafka", MiddlewareStatus.DISABLED, "3.8.x", "消息队列已禁用");
        }

        if (!StringUtils.hasText(featureProperties.getMiddleware().getKafka().getBootstrapServers())) {
            return new MiddlewareInfo("Kafka", MiddlewareStatus.DEGRADED, "3.8.x", "Kafka 已启用，但 bootstrap-servers 为空");
        }

        String details = String.format("服务器:%s | 组ID:%s", 
            featureProperties.getMiddleware().getKafka().getBootstrapServers(),
            featureProperties.getMiddleware().getKafka().getGroupId());
        return new MiddlewareInfo("Kafka", MiddlewareStatus.CONFIGURED, "3.8.x", details);
    }

    /**
     * MinIO健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkMinio() {
        if (!featureProperties.getMiddleware().getMinio().isEnabled()) {
            return new MiddlewareInfo("MinIO", MiddlewareStatus.DISABLED, "RELEASE.2024", "对象存储已禁用");
        }

        FeatureProperties.MiddlewareFeature.MinioConfig minio = featureProperties.getMiddleware().getMinio();
        if (!StringUtils.hasText(minio.getEndpoint())
                || !StringUtils.hasText(minio.getAccessKey())
                || !StringUtils.hasText(minio.getSecretKey())
                || !StringUtils.hasText(minio.getBucketName())) {
            return new MiddlewareInfo("MinIO", MiddlewareStatus.DEGRADED, "RELEASE.2024", "MinIO 已启用，但 endpoint/accessKey/secretKey/bucket 配置不完整");
        }

        String details = String.format("端点:%s | 桶:%s", 
            featureProperties.getMiddleware().getMinio().getEndpoint(),
            featureProperties.getMiddleware().getMinio().getBucketName());
        return new MiddlewareInfo("MinIO", MiddlewareStatus.CONFIGURED, "RELEASE.2024", details);
    }

    /**
     * OSS健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkOss() {
        if (!featureProperties.getMiddleware().getOss().isEnabled()) {
            return new MiddlewareInfo("OSS", MiddlewareStatus.DISABLED, "SDK 3.x", "云存储已禁用");
        }

        FeatureProperties.MiddlewareFeature.OssConfig oss = featureProperties.getMiddleware().getOss();
        if (!StringUtils.hasText(oss.getEndpoint())
                || !StringUtils.hasText(oss.getAccessKey())
                || !StringUtils.hasText(oss.getSecretKey())
                || !StringUtils.hasText(oss.getBucketName())) {
            return new MiddlewareInfo("OSS", MiddlewareStatus.DEGRADED, "SDK 3.x", "OSS 已启用，但 endpoint/accessKey/secretKey/bucket 配置不完整");
        }

        String details = String.format("提供商:%s | 桶:%s", 
            featureProperties.getMiddleware().getOss().getProvider(),
            featureProperties.getMiddleware().getOss().getBucketName());
        return new MiddlewareInfo("OSS", MiddlewareStatus.CONFIGURED, "SDK 3.x", details);
    }

    /**
     * 监控健康检查 - 包含版本信息
     */
    private MiddlewareInfo checkMonitoring() {
        if (!featureProperties.getMonitoring().isEnabled()) {
            return new MiddlewareInfo("Monitoring", MiddlewareStatus.DISABLED, "Actuator 3.4.5", "监控功能已禁用");
        }

        List<String> features = new ArrayList<>();
        if (featureProperties.getMonitoring().isEnablePerformance()) features.add("性能监控");
        if (featureProperties.getMonitoring().isEnableHealthCheck()) features.add("健康检查");
        if (featureProperties.getMonitoring().isEnableMetrics()) features.add("指标收集");

        String details = features.isEmpty() ? "无附加特性" : "特性: " + String.join(", ", features);
        return new MiddlewareInfo("Monitoring", MiddlewareStatus.READY, "Actuator 3.4.5", details);
    }

    /**
     * 打印中间件状态 - 包含版本信息
     */
    private void printMiddlewareStatus(List<MiddlewareInfo> middlewareList)
    {
        log.info("┌─────────────────┬──────────┬─────────────────┐");
        log.info("│ 中间件名称      │ 状态     │ 版本            │");
        log.info("├─────────────────┼──────────┼─────────────────┤");

        for (MiddlewareInfo info : middlewareList) {
            String name = String.format("%-15s", truncate(info.getName(), 15));
            String status = info.getStatus().getIcon() + " " + info.getStatus().getDescription();
            String version = String.format("%-15s", truncate(info.getVersion(), 15));
            
            log.info("│ {} │ {} │ {} │", name, String.format("%-8s", status), version);
        }

        log.info("└─────────────────┴──────────┴─────────────────┘");
    }

    /**
     * 打印功能摘要 - 最简版本
     */
    private void printFeatureSummary()
    {
        // 简化版本：只显示中间件状态表格，不再显示统计信息
        // 表格已经足够清晰地展示所有信息
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength)
    {
        if (str == null || str.length() <= maxLength)
        {
            return str == null ? "" : str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

}
