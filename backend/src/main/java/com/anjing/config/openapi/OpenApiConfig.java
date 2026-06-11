package com.anjing.config.openapi;

import com.anjing.model.constants.PlatformContractConstants;
import com.anjing.model.constants.RequestHeaderConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI anjingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Anjing Agent AIGC API")
                        .version("1.0.0")
                        .description("Runtime API contract for AIGC creation, frontend typing, AI code generation, and service integration."))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current application origin")));
    }

    @Bean
    public GroupedOpenApi runtimeOpenApi() {
        return GroupedOpenApi.builder()
                .group("runtime")
                .pathsToMatch(PlatformContractConstants.API_PREFIX + "/**")
                .build();
    }

    @Bean
    public OperationCustomizer platformHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            addHeader(operation, RequestHeaderConstants.REQUEST_ID, "Request id for frontend/backend log correlation.");
            addHeader(operation, RequestHeaderConstants.TRACE_ID, "Trace id propagated across services.");
            addHeader(operation, RequestHeaderConstants.TENANT_ID, "Tenant id for multi-tenant scenarios.");
            addHeader(operation, RequestHeaderConstants.USER_ID, "Current user id propagated by gateway or auth adapter.");
            addHeader(operation, RequestHeaderConstants.USER_NAME, "Current user display name propagated by gateway or auth adapter.");
            addHeader(operation, RequestHeaderConstants.USER_ROLES, "Current user role list propagated by gateway or auth adapter.");
            addHeader(operation, RequestHeaderConstants.CALLER_ID, "Calling service id for service-to-service requests.");
            addHeader(operation, RequestHeaderConstants.TIME_ZONE, "Client time zone, for example Asia/Shanghai.");
            addHeader(operation, RequestHeaderConstants.ACCEPT_LANGUAGE, "Client locale preference.");
            return operation;
        };
    }

    private static void addHeader(Operation operation, String name, String description) {
        if (operation.getParameters() != null
                && operation.getParameters().stream().anyMatch(parameter -> name.equals(parameter.getName()))) {
            return;
        }

        operation.addParametersItem(new Parameter()
                .in("header")
                .name(name)
                .required(false)
                .description(description)
                .schema(new StringSchema()));
    }
}
