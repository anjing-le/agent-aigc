package com.anjing.aigc.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageStatusResponse {

    private String activeMode;

    private StorageBackendStatusResponse local;

    private StorageBackendStatusResponse oss;

    private Boolean assetCleanupSupported;

    private Boolean materialCleanupSupported;

    private String message;

    private String checkedAt;
}
