package com.anjing.config.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 🔒 本地锁配置类 - 分布式锁降级方案
 * 
 * <p>当Redis不可用或选择本地锁时，提供基于JVM的本地锁实现</p>
 * 
 * <h3>🎯 启用条件：</h3>
 * <ul>
 *   <li>app.features.distributed-lock.enabled=true</li>
 *   <li>app.features.distributed-lock.provider=local</li>
 * </ul>
 * 
 * <h3>⚠️ 注意事项：</h3>
 * <ul>
 *   <li>仅在单机部署时有效</li>
 *   <li>多实例部署时无法保证分布式互斥</li>
 *   <li>适合开发环境和单体应用</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(name = "app.features.distributed-lock.provider", havingValue = "local")
@Slf4j
public class LocalLockConfig {

    /**
     * 本地锁管理器
     */
    @Bean
    public LocalLockManager localLockManager() {
        log.warn("🔄 分布式锁降级: 使用本地锁实现 (仅适用于单机部署)");
        return new LocalLockManager();
    }

    /**
     * 本地锁管理器实现
     */
    public static class LocalLockManager {
        
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

        /**
         * 获取本地锁，直到成功为止。
         *
         * @param lockKey 锁键
         * @param expireTime 过期时间（秒，本地锁忽略此参数）
         */
        public void lock(String lockKey, long expireTime) {
            ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
            lock.lock();
            log.debug("🔒 本地锁获取成功: {}", lockKey);
        }
        
        /**
         * 获取锁
         * 
         * @param lockKey 锁键
         * @param waitTime 等待时间（秒）
         * @param expireTime 过期时间（秒，本地锁忽略此参数）
         * @return 是否获取成功
         */
        public boolean tryLock(String lockKey, long waitTime, long expireTime) {
            ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
            
            try {
                boolean acquired = lock.tryLock(waitTime, java.util.concurrent.TimeUnit.SECONDS);
                if (acquired) {
                    log.debug("🔒 本地锁获取成功: {}", lockKey);
                } else {
                    log.debug("🔒 本地锁获取失败: {} (等待超时)", lockKey);
                }
                return acquired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("🔒 本地锁获取被中断: {}", lockKey);
                return false;
            }
        }
        
        /**
         * 释放锁
         * 
         * @param lockKey 锁键
         */
        public void unlock(String lockKey) {
            ReentrantLock lock = locks.get(lockKey);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("🔓 本地锁释放成功: {}", lockKey);
                
                // 清理无人持有的锁
                if (!lock.hasQueuedThreads()) {
                    locks.remove(lockKey);
                }
            }
        }
        
        /**
         * 检查锁状态
         * 
         * @param lockKey 锁键
         * @return 是否被锁定
         */
        public boolean isLocked(String lockKey) {
            ReentrantLock lock = locks.get(lockKey);
            return lock != null && lock.isLocked();
        }
        
        /**
         * 获取锁统计信息
         */
        public String getStats() {
            int totalLocks = locks.size();
            long activeLocks = locks.values().stream().mapToLong(lock -> lock.isLocked() ? 1 : 0).sum();
            return String.format("总锁数: %d, 活跃锁: %d", totalLocks, activeLocks);
        }
    }
}
