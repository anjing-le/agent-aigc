package com.anjing.aigc.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageBackendStatusResponse {

    private String backend;

    private Boolean enabled;

    private Boolean configured;

    private Boolean available;

    private Boolean readable;

    private Boolean writable;

    private Boolean cleanupSupported;

    private String basePath;

    private String urlPrefix;

    private String provider;

    private Boolean endpointConfigured;

    private Boolean bucketConfigured;

    private Boolean cdnConfigured;

    private String message;
}
