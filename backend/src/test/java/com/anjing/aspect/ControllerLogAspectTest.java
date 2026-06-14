package com.anjing.aspect;

import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.request.ProviderCredentialUpdateRequest;
import com.anjing.model.response.APIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerLogAspectTest {

    private final ControllerLogAspect aspect = new ControllerLogAspect(new ObjectMapper());

    @Test
    void formatArgsMasksProviderCredential() {
        ProviderCredentialUpdateRequest request = new ProviderCredentialUpdateRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("GOOGLE");
        request.setProviderName("Google Nano Banana");
        request.setCredential("runtime-secret-value");

        String formatted = (String) ReflectionTestUtils.invokeMethod(aspect, "formatArgs", new Object[]{new Object[]{request}});

        assertFalse(formatted.contains("runtime-secret-value"));
        assertTrue(formatted.contains("\"credential\":\"***FILTERED***\""));
    }

    @Test
    void formatResultMasksNestedSensitiveFields() {
        APIResponse<Map<String, Object>> response = APIResponse.success(Map.of(
                "token", "access-token-value",
                "profile", Map.of("password", "plain-password")
        ));

        String formatted = (String) ReflectionTestUtils.invokeMethod(aspect, "formatResult", response);

        assertFalse(formatted.contains("access-token-value"));
        assertFalse(formatted.contains("plain-password"));
        assertTrue(formatted.contains("\"token\":\"***FILTERED***\""));
        assertTrue(formatted.contains("\"password\":\"***FILTERED***\""));
    }
}
