package com.anjing.model.constants;

/**
 * API路径常量类
 * 🏗️ 分层架构设计：业务模块 + 系统管理
 * 
 * 【架构理念】
 * 🎯 业务层：面向用户的核心业务功能
 * ⚙️ 管理层：系统管理和运维功能
 * 🔧 通用层：基础设施和公共服务
 * 
 * 【设计目的】
 * 1. 模块清晰：按业务领域组织API路径
 * 2. 易于维护：统一管理所有接口路径
 * 3. 避免冲突：规范化的路径命名
 * 4. 便于扩展：新模块按规范增加
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class ApiConstants {

    /**
     * 🎯 用户业务模块
     * 
     * 核心功能：用户注册、登录、信息管理、权限控制
     */
    public static class User {
        
        /** 🔐 认证相关 */
        public static final String LOGIN = "/api/auth/login";
        public static final String LOGOUT = "/api/auth/logout";
        public static final String REFRESH_TOKEN = "/api/auth/refresh";
        public static final String CURRENT_USER = "/api/auth/me";
        
        /** 👤 用户管理 */
        public static final String CREATE_USER = "/api/users";
        public static final String GET_USER = "/api/users/{userId}";
        public static final String UPDATE_USER = "/api/users/{userId}";
        public static final String DELETE_USER = "/api/users/{userId}";
        public static final String USER_LIST = "/api/users";
        
        /** 🔧 用户操作 */
        public static final String CHANGE_PASSWORD = "/api/users/{userId}/password/change";
        public static final String RESET_PASSWORD = "/api/users/{userId}/password/reset";
        public static final String BATCH_UPDATE_STATUS = "/api/users/batch/status";
        
        /** ✅ 验证接口 */
        public static final String CHECK_USERNAME = "/api/users/username/available";
        public static final String CHECK_EMAIL = "/api/users/email/available";
        
        /** 📊 统计信息 */
        public static final String USER_STATS = "/api/users/stats";
    }

    /**
     * ⚙️ 系统管理模块
     * 
     * 核心功能：系统监控、配置管理、日志查看
     */
    public static class Admin {
        
        /** 📊 系统监控 */
        public static final String DASHBOARD = "/api/admin/dashboard";
        public static final String SYSTEM_INFO = "/api/admin/system/info";
        public static final String HEALTH_CHECK = "/api/admin/health";
        
        /** 📋 日志管理 */
        public static final String LOGS = "/api/admin/logs";
        public static final String OPERATION_LOGS = "/api/admin/logs/operations";
        public static final String ERROR_LOGS = "/api/admin/logs/errors";
        
        /** ⚙️ 配置管理 */
        public static final String CONFIGS = "/api/admin/configs";
        public static final String UPDATE_CONFIG = "/api/admin/configs/{key}";
        
        /** 🗄️ 数据管理 */
        public static final String DATABASE_BACKUP = "/api/admin/database/backup";
        public static final String DATA_EXPORT = "/api/admin/data/export";
        public static final String DATA_IMPORT = "/api/admin/data/import";
    }

    /**
     * 🔧 通用服务模块
     * 
     * 核心功能：文件上传、缓存管理、工具接口
     */
    public static class Common {
        
        /** 📁 文件服务 */
        public static final String UPLOAD_FILE = "/api/common/upload";
        public static final String UPLOAD_IMAGE = "/api/common/upload/image";
        public static final String DOWNLOAD_FILE = "/api/common/download/{fileId}";
        public static final String DELETE_FILE = "/api/common/files/{fileId}";
        
        /** 🗄️ 缓存服务 */
        public static final String CACHE_CLEAR = "/api/common/cache/clear";
        public static final String CACHE_INFO = "/api/common/cache/info";
        public static final String CACHE_KEYS = "/api/common/cache/keys";
        
        /** 🛠️ 工具接口 */
        public static final String GENERATE_ID = "/api/common/tools/id";
        public static final String ENCODE_PASSWORD = "/api/common/tools/password/encode";
        public static final String SEND_EMAIL = "/api/common/tools/email/send";
        public static final String SEND_SMS = "/api/common/tools/sms/send";
        
        /** 📊 验证码 */
        public static final String CAPTCHA_GENERATE = "/api/common/captcha/generate";
        public static final String CAPTCHA_VERIFY = "/api/common/captcha/verify";
    }

    /**
     * 🔗 第三方集成模块
     * 
     * 核心功能：外部服务集成、API代理
     */
    public static class Integration {
        
        /** ☁️ 云服务 */
        public static final String OSS_UPLOAD = "/api/integration/oss/upload";
        public static final String OSS_DELETE = "/api/integration/oss/delete";
        
        /** 💰 支付服务 */
        public static final String PAYMENT_CREATE = "/api/integration/payment/create";
        public static final String PAYMENT_CALLBACK = "/api/integration/payment/callback";
        public static final String PAYMENT_QUERY = "/api/integration/payment/query";
        
        /** 📧 通知服务 */
        public static final String EMAIL_SEND = "/api/integration/email/send";
        public static final String SMS_SEND = "/api/integration/sms/send";
        public static final String PUSH_SEND = "/api/integration/push/send";
    }

    /**
     * 🏷️ API版本管理
     */
    public static class Version {
        public static final String V1 = "/api/v1";
        public static final String V2 = "/api/v2";
        public static final String LATEST = "/api";
    }

    /**
     * 🔒 权限相关常量
     */
    public static class Permission {
        public static final String ADMIN_PREFIX = "/api/admin";
        public static final String USER_PREFIX = "/api/users";
        public static final String PUBLIC_PREFIX = "/api/public";
        public static final String COMMON_PREFIX = "/api/common";
    }

    private ApiConstants() {
        // 工具类，禁止实例化
    }
}
