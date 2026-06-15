package com.anjing.aigc.model.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.Resource;

import java.net.URI;

@Data
@Builder
public class AigcStorageDownloadResource {

    private Resource resource;

    private URI redirectUri;

    private String fileName;

    private String contentType;

    private Long contentLength;

    public boolean isRedirect() {
        return redirectUri != null;
    }
}
