package com.anjing.model.constants;

/**
 * Generated from contracts/service-boundaries.json. Do not edit manually.
 * Run: node scripts/generate-service-boundaries-backend.js
 */
public final class ServiceBoundaryConstants {

    public static final int SCHEMA_VERSION = 1;
    public static final String APPLICATION_ID = "agent-aigc";
    public static final String API_PREFIX = "/api";
    public static final String[] BOUNDARY_IDS = { "auth", "test", "common", "aigc", "user", "admin", "integration" };

    private ServiceBoundaryConstants() {
    }

    public static final class Auth {
        public static final String ID = "auth";
        public static final String NAME = "Authentication";
        public static final String KIND = "runtime";
        public static final String OWNER = "infra-auth";
        public static final String CURRENT_HOST = "agent-aigc";
        public static final String BASE_PATH = "/api/auth";
        public static final String API_CONSTANTS_CLASS = "Auth";
        public static final String API_PATHS_KEY = "auth";
        public static final boolean OPENAPI = true;
        public static final String COPY_ACTION = "replace with real auth center or database-backed auth";
        public static final String[] ROUTES = { "login", "logout", "currentUser", "refreshToken" };

        private Auth() {
        }
    }

    public static final class Test {
        public static final String ID = "test";
        public static final String NAME = "Scaffold Test";
        public static final String KIND = "sample";
        public static final String OWNER = "agent-aigc";
        public static final String CURRENT_HOST = "agent-aigc";
        public static final String BASE_PATH = "/api/test";
        public static final String API_CONSTANTS_CLASS = "Test";
        public static final String API_PATHS_KEY = "test";
        public static final boolean OPENAPI = true;
        public static final String COPY_ACTION = "delete or replace after the copied project has its own health and sample strategy";
        public static final String[] ROUTES = { "health", "features", "ping", "bizException", "systemException", "items", "itemDetail" };

        private Test() {
        }
    }

    public static final class Common {
        public static final String ID = "common";
        public static final String NAME = "Common Platform";
        public static final String KIND = "reserved-runtime";
        public static final String OWNER = "infra-common";
        public static final String CURRENT_HOST = "agent-aigc";
        public static final String BASE_PATH = "/api/common";
        public static final String API_CONSTANTS_CLASS = "Common";
        public static final String API_PATHS_KEY = "common";
        public static final boolean OPENAPI = false;
        public static final String COPY_ACTION = "keep only endpoints implemented by the copied project";
        public static final String[] ROUTES = { "upload", "uploadImage", "uploadWangEditor", "download", "deleteFile" };

        private Common() {
        }
    }

    public static final class Aigc {
        public static final String ID = "aigc";
        public static final String NAME = "AIGC Creation";
        public static final String KIND = "runtime";
        public static final String OWNER = "agent-aigc";
        public static final String CURRENT_HOST = "agent-aigc";
        public static final String BASE_PATH = "/api/aigc";
        public static final String API_CONSTANTS_CLASS = "Aigc";
        public static final String API_PATHS_KEY = "aigc";
        public static final boolean OPENAPI = true;
        public static final String COPY_ACTION = "core business boundary for multimodal generation, assets, gallery, and model metadata";
        public static final String[] ROUTES = { "generate", "taskStatus", "models", "materials", "materialDetail", "materialUpload", "gallery", "gallerySave", "assets", "assetDetail" };

        private Aigc() {
        }
    }

    public static final class User {
        public static final String ID = "user";
        public static final String NAME = "User Management";
        public static final String KIND = "reserved";
        public static final String OWNER = "infra-auth";
        public static final String CURRENT_HOST = "future-service";
        public static final String BASE_PATH = "/api/users";
        public static final String API_CONSTANTS_CLASS = "User";
        public static final String API_PATHS_KEY = "";
        public static final boolean OPENAPI = false;
        public static final String COPY_ACTION = "use when the copied project implements real user management";
        public static final String[] ROUTES = {};

        private User() {
        }
    }

    public static final class Admin {
        public static final String ID = "admin";
        public static final String NAME = "Admin Operations";
        public static final String KIND = "reserved";
        public static final String OWNER = "infra-admin";
        public static final String CURRENT_HOST = "future-service";
        public static final String BASE_PATH = "/api/admin";
        public static final String API_CONSTANTS_CLASS = "Admin";
        public static final String API_PATHS_KEY = "";
        public static final boolean OPENAPI = false;
        public static final String COPY_ACTION = "use for operations dashboards, logs, and platform admin APIs";
        public static final String[] ROUTES = {};

        private Admin() {
        }
    }

    public static final class Integration {
        public static final String ID = "integration";
        public static final String NAME = "External Integration";
        public static final String KIND = "reserved";
        public static final String OWNER = "infra-integration";
        public static final String CURRENT_HOST = "future-service";
        public static final String BASE_PATH = "/api/integration";
        public static final String API_CONSTANTS_CLASS = "Integration";
        public static final String API_PATHS_KEY = "";
        public static final boolean OPENAPI = false;
        public static final String COPY_ACTION = "use for OSS, payment, notification, LLM provider, and other external adapters";
        public static final String[] ROUTES = {};

        private Integration() {
        }
    }
}
