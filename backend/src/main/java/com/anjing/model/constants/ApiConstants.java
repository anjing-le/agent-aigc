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

    public static final String API_PREFIX = PlatformContractConstants.API_PREFIX;

    /**
     * 🔐 认证模块
     */
    public static class Auth {
        public static final String BASE = API_PREFIX + "/auth";

        public static final String LOGIN = "/login";
        public static final String LOGOUT = "/logout";
        public static final String ME = "/me";
        public static final String REFRESH = "/refresh";

        public static final String LOGIN_FULL = BASE + LOGIN;
        public static final String LOGOUT_FULL = BASE + LOGOUT;
        public static final String ME_FULL = BASE + ME;
        public static final String REFRESH_FULL = BASE + REFRESH;

        private Auth() {
        }
    }

    /**
     * 🧪 脚手架示例和自检接口
     */
    public static class Test {
        public static final String BASE = API_PREFIX + "/test";

        public static final String HEALTH = "/health";
        public static final String FEATURES = "/features";
        public static final String PING = "/ping";
        public static final String EXCEPTION_BIZ = "/exception/biz";
        public static final String EXCEPTION_SYSTEM = "/exception/system";
        public static final String ITEMS = "/items";
        public static final String ITEM_DETAIL = "/items/{id}";

        public static final String HEALTH_FULL = BASE + HEALTH;
        public static final String FEATURES_FULL = BASE + FEATURES;
        public static final String PING_FULL = BASE + PING;
        public static final String EXCEPTION_BIZ_FULL = BASE + EXCEPTION_BIZ;
        public static final String EXCEPTION_SYSTEM_FULL = BASE + EXCEPTION_SYSTEM;
        public static final String ITEMS_FULL = BASE + ITEMS;
        public static final String ITEM_DETAIL_FULL = BASE + ITEM_DETAIL;

        private Test() {
        }
    }

    /**
     * 🎯 用户业务模块
     * 
     * 核心功能：用户注册、登录、信息管理、权限控制
     */
    public static class User {
        public static final String BASE = API_PREFIX + "/users";
        
        /** 🔐 认证相关 */
        public static final String LOGIN = Auth.LOGIN_FULL;
        public static final String LOGOUT = Auth.LOGOUT_FULL;
        public static final String REFRESH_TOKEN = Auth.REFRESH_FULL;
        public static final String CURRENT_USER = Auth.ME_FULL;
        
        /** 👤 用户管理 */
        public static final String CREATE_USER = "";
        public static final String GET_USER = "/{userId}";
        public static final String UPDATE_USER = "/{userId}";
        public static final String DELETE_USER = "/{userId}";
        public static final String USER_LIST = "";
        
        /** 🔧 用户操作 */
        public static final String CHANGE_PASSWORD = "/{userId}/password/change";
        public static final String RESET_PASSWORD = "/{userId}/password/reset";
        public static final String BATCH_UPDATE_STATUS = "/batch/status";
        
        /** ✅ 验证接口 */
        public static final String CHECK_USERNAME = "/username/available";
        public static final String CHECK_EMAIL = "/email/available";
        
        /** 📊 统计信息 */
        public static final String USER_STATS = "/stats";

        public static final String CREATE_USER_FULL = BASE + CREATE_USER;
        public static final String GET_USER_FULL = BASE + GET_USER;
        public static final String UPDATE_USER_FULL = BASE + UPDATE_USER;
        public static final String DELETE_USER_FULL = BASE + DELETE_USER;
        public static final String USER_LIST_FULL = BASE + USER_LIST;
        public static final String CHANGE_PASSWORD_FULL = BASE + CHANGE_PASSWORD;
        public static final String RESET_PASSWORD_FULL = BASE + RESET_PASSWORD;
        public static final String BATCH_UPDATE_STATUS_FULL = BASE + BATCH_UPDATE_STATUS;
        public static final String CHECK_USERNAME_FULL = BASE + CHECK_USERNAME;
        public static final String CHECK_EMAIL_FULL = BASE + CHECK_EMAIL;
        public static final String USER_STATS_FULL = BASE + USER_STATS;

        private User() {
        }
    }

    /**
     * ⚙️ 系统管理模块
     * 
     * 核心功能：系统监控、配置管理、日志查看
     */
    public static class Admin {
        public static final String BASE = API_PREFIX + "/admin";
        
        /** 📊 系统监控 */
        public static final String DASHBOARD = "/dashboard";
        public static final String SYSTEM_INFO = "/system/info";
        public static final String HEALTH_CHECK = "/health";
        
        /** 📋 日志管理 */
        public static final String LOGS = "/logs";
        public static final String OPERATION_LOGS = "/logs/operations";
        public static final String ERROR_LOGS = "/logs/errors";
        
        /** ⚙️ 配置管理 */
        public static final String CONFIGS = "/configs";
        public static final String UPDATE_CONFIG = "/configs/{key}";
        
        /** 🗄️ 数据管理 */
        public static final String DATABASE_BACKUP = "/database/backup";
        public static final String DATA_EXPORT = "/data/export";
        public static final String DATA_IMPORT = "/data/import";

        public static final String DASHBOARD_FULL = BASE + DASHBOARD;
        public static final String SYSTEM_INFO_FULL = BASE + SYSTEM_INFO;
        public static final String HEALTH_CHECK_FULL = BASE + HEALTH_CHECK;
        public static final String LOGS_FULL = BASE + LOGS;
        public static final String OPERATION_LOGS_FULL = BASE + OPERATION_LOGS;
        public static final String ERROR_LOGS_FULL = BASE + ERROR_LOGS;
        public static final String CONFIGS_FULL = BASE + CONFIGS;
        public static final String UPDATE_CONFIG_FULL = BASE + UPDATE_CONFIG;
        public static final String DATABASE_BACKUP_FULL = BASE + DATABASE_BACKUP;
        public static final String DATA_EXPORT_FULL = BASE + DATA_EXPORT;
        public static final String DATA_IMPORT_FULL = BASE + DATA_IMPORT;

        private Admin() {
        }
    }

    /**
     * 🔧 通用服务模块
     * 
     * 核心功能：文件上传、缓存管理、工具接口
     */
    public static class Common {
        public static final String BASE = API_PREFIX + "/common";
        
        /** 📁 文件服务 */
        public static final String UPLOAD_FILE = "/upload";
        public static final String UPLOAD_IMAGE = "/upload/image";
        public static final String UPLOAD_WANG_EDITOR = "/upload/wangeditor";
        public static final String DOWNLOAD_FILE = "/download/{fileId}";
        public static final String DELETE_FILE = "/files/{fileId}";
        
        /** 🗄️ 缓存服务 */
        public static final String CACHE_CLEAR = "/cache/clear";
        public static final String CACHE_INFO = "/cache/info";
        public static final String CACHE_KEYS = "/cache/keys";
        
        /** 🛠️ 工具接口 */
        public static final String GENERATE_ID = "/tools/id";
        public static final String ENCODE_PASSWORD = "/tools/password/encode";
        public static final String SEND_EMAIL = "/tools/email/send";
        public static final String SEND_SMS = "/tools/sms/send";
        
        /** 📊 验证码 */
        public static final String CAPTCHA_GENERATE = "/captcha/generate";
        public static final String CAPTCHA_VERIFY = "/captcha/verify";

        public static final String UPLOAD_FILE_FULL = BASE + UPLOAD_FILE;
        public static final String UPLOAD_IMAGE_FULL = BASE + UPLOAD_IMAGE;
        public static final String UPLOAD_WANG_EDITOR_FULL = BASE + UPLOAD_WANG_EDITOR;
        public static final String DOWNLOAD_FILE_FULL = BASE + DOWNLOAD_FILE;
        public static final String DELETE_FILE_FULL = BASE + DELETE_FILE;
        public static final String CACHE_CLEAR_FULL = BASE + CACHE_CLEAR;
        public static final String CACHE_INFO_FULL = BASE + CACHE_INFO;
        public static final String CACHE_KEYS_FULL = BASE + CACHE_KEYS;
        public static final String GENERATE_ID_FULL = BASE + GENERATE_ID;
        public static final String ENCODE_PASSWORD_FULL = BASE + ENCODE_PASSWORD;
        public static final String SEND_EMAIL_FULL = BASE + SEND_EMAIL;
        public static final String SEND_SMS_FULL = BASE + SEND_SMS;
        public static final String CAPTCHA_GENERATE_FULL = BASE + CAPTCHA_GENERATE;
        public static final String CAPTCHA_VERIFY_FULL = BASE + CAPTCHA_VERIFY;

        private Common() {
        }
    }

    /**
     * 🔗 第三方集成模块
     * 
     * 核心功能：外部服务集成、API代理
     */
    public static class Integration {
        public static final String BASE = API_PREFIX + "/integration";
        
        /** ☁️ 云服务 */
        public static final String OSS_UPLOAD = "/oss/upload";
        public static final String OSS_DELETE = "/oss/delete";
        
        /** 💰 支付服务 */
        public static final String PAYMENT_CREATE = "/payment/create";
        public static final String PAYMENT_CALLBACK = "/payment/callback";
        public static final String PAYMENT_QUERY = "/payment/query";
        
        /** 📧 通知服务 */
        public static final String EMAIL_SEND = "/email/send";
        public static final String SMS_SEND = "/sms/send";
        public static final String PUSH_SEND = "/push/send";

        public static final String OSS_UPLOAD_FULL = BASE + OSS_UPLOAD;
        public static final String OSS_DELETE_FULL = BASE + OSS_DELETE;
        public static final String PAYMENT_CREATE_FULL = BASE + PAYMENT_CREATE;
        public static final String PAYMENT_CALLBACK_FULL = BASE + PAYMENT_CALLBACK;
        public static final String PAYMENT_QUERY_FULL = BASE + PAYMENT_QUERY;
        public static final String EMAIL_SEND_FULL = BASE + EMAIL_SEND;
        public static final String SMS_SEND_FULL = BASE + SMS_SEND;
        public static final String PUSH_SEND_FULL = BASE + PUSH_SEND;

        private Integration() {
        }
    }

    /**
     * AIGC 创作平台模块
     */
    public static class Aigc {
        public static final String BASE = API_PREFIX + "/aigc";

        public static final String GENERATE = "/generate";
        public static final String TASK_STATUS = "/task/{taskId}";
        public static final String TASK_RETRY = "/task/{taskId}/retry";
        public static final String MODELS = "/models";
        public static final String MODEL_PROBE = "/models/probe";
        public static final String MATERIALS = "/materials";
        public static final String MATERIAL_DETAIL = "/materials/{materialId}";
        public static final String MATERIAL_TASKS = "/materials/{materialId}/tasks";
        public static final String MATERIAL_UPLOAD = "/materials/upload";
        public static final String GALLERY = "/gallery";
        public static final String GALLERY_SAVE = "/gallery/save";
        public static final String ASSETS = "/assets";
        public static final String ASSET_DETAIL = "/assets/{assetId}";

        public static final String GENERATE_FULL = BASE + GENERATE;
        public static final String TASK_STATUS_FULL = BASE + TASK_STATUS;
        public static final String TASK_RETRY_FULL = BASE + TASK_RETRY;
        public static final String MODELS_FULL = BASE + MODELS;
        public static final String MODEL_PROBE_FULL = BASE + MODEL_PROBE;
        public static final String MATERIALS_FULL = BASE + MATERIALS;
        public static final String MATERIAL_DETAIL_FULL = BASE + MATERIAL_DETAIL;
        public static final String MATERIAL_TASKS_FULL = BASE + MATERIAL_TASKS;
        public static final String MATERIAL_UPLOAD_FULL = BASE + MATERIAL_UPLOAD;
        public static final String GALLERY_FULL = BASE + GALLERY;
        public static final String GALLERY_SAVE_FULL = BASE + GALLERY_SAVE;
        public static final String ASSETS_FULL = BASE + ASSETS;
        public static final String ASSET_DETAIL_FULL = BASE + ASSET_DETAIL;

        private Aigc() {
        }
    }

    /**
     * 🏷️ API版本管理
     */
    public static class Version {
        public static final String V1 = API_PREFIX + "/v1";
        public static final String V2 = API_PREFIX + "/v2";
        public static final String LATEST = API_PREFIX;

        private Version() {
        }
    }

    /**
     * 🔒 权限相关常量
     */
    public static class Permission {
        public static final String ADMIN_PREFIX = Admin.BASE;
        public static final String USER_PREFIX = User.BASE;
        public static final String PUBLIC_PREFIX = API_PREFIX + "/public";
        public static final String COMMON_PREFIX = Common.BASE;

        private Permission() {
        }
    }

    private ApiConstants() {
        // 工具类，禁止实例化
    }
}
