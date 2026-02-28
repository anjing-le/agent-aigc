package com.anjing;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 🚀 Backend Template 启动类
 * 
 * <p>企业级Spring Boot脚手架应用入口</p>
 * 
 * <h3>🎯 核心功能：</h3>
 * <ul>
 *   <li>🔄 AOP切面支持 - 分布式锁、日志、性能监控</li>
 *   <li>⏰ 定时任务支持 - @Scheduled注解</li>
 *   <li>🔀 异步处理支持 - @Async注解</li>
 *   <li>💾 缓存支持 - @Cacheable注解</li>
 *   <li>📊 JPA审计支持 - 自动记录创建/更新时间</li>
 *   <li>🌐 Servlet组件扫描 - Filter、Listener等</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@SpringBootApplication
@ServletComponentScan
@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableJpaAuditing
@Slf4j
public class Application
{

    public static void main(String[] args) throws UnknownHostException
    {
        
        // 记录启动开始时间
        long startTime = System.currentTimeMillis();
        
        // 创建SpringApplication实例
        SpringApplication app = new SpringApplication(Application.class);
        
        // 启动Spring Boot应用并获取上下文
        ConfigurableApplicationContext applicationContext = app.run(args);
        
        // 获取环境配置
        Environment env = applicationContext.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String applicationName = env.getProperty("spring.application.name", "agent-aigc");
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profile = String.join(",", env.getActiveProfiles());
        
        // 处理上下文路径
        if (StringUtils.isEmpty(contextPath) || "/".equals(contextPath)) {
            contextPath = "";
        }
        
        // 计算启动耗时
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        
        // 打印启动成功信息
        log.info("\n" +
                "🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉\n" +
                "🚀  {} 启动成功！\n" +
                "🌍  本地访问地址:     http://localhost:{}{}\n" +
                "🌐  外部访问地址:     http://{}:{}{}\n" +
                "📚  Swagger文档:     http://{}:{}{}/swagger-ui/index.html\n" +
                "📋  OpenAPI文档:     http://{}:{}{}/v3/api-docs\n" +
                "💾  Druid监控:       http://{}:{}{}/druid/index.html (admin/admin123)\n" +
                "🔧  运行环境:         {}\n" +
                "⏱️  启动耗时:         {} 秒\n" +
                "🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉",
                applicationName,
                port, contextPath,
                ip, port, contextPath,
                ip, port, contextPath,
                ip, port, contextPath,
                ip, port, contextPath,
                StringUtils.isEmpty(profile) ? "default" : profile,
                duration
        );
        
        // 🔍 打印自定义Bean信息（开发环境）
        // if (Arrays.asList(env.getActiveProfiles()).contains("dev")) {
        //     printCustomBeans(applicationContext);
        // }
    }
    
    /**
     * 🔍 打印自定义注册的Bean信息
     * 
     * <p>仅在开发环境下输出，帮助开发者了解Spring容器中的Bean注册情况</p>
     * 
     * @param applicationContext Spring应用上下文
     */
    private static void printCustomBeans(ConfigurableApplicationContext applicationContext)
    {
        log.info("\n" +
                "📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦\n" +
                "🔍  自定义Bean注册信息 (开发环境)\n" +
                "📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦"
        );
        
        // 获取所有Bean名称并排序
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        
        int customBeanCount = 0;
        
        for (String beanName : beanNames)
        {
            if (applicationContext.containsBean(beanName))
            {
                Object bean = applicationContext.getBean(beanName);
                String packageName = bean.getClass().getPackage() != null ? 
                    bean.getClass().getPackage().getName() : "";
                
                // 只显示我们项目包下的Bean
                if (packageName.startsWith("com.anjing"))
                {
                    customBeanCount++;
                    log.info("📌  Bean名称: {} | Bean类型: {}", 
                            beanName, bean.getClass().getSimpleName());
                }
            } else {
                log.warn("⚠️  Bean名称: {} 未在容器中找到！", beanName);
            }
        }
        
        log.info("\n" +
                "📊  统计信息: 共注册 {} 个自定义Bean\n" +
                "📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦📦",
                customBeanCount
        );
    }
}
