package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.TokenResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LoginRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UserResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.filter.JsonLoginFilter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

@Component
public class LoginOpenApiCustomizer implements OpenApiCustomizer {

    private static final String JSON = "application/json";
    private static final String SCHEMA_PREFIX = "#/components/schemas/";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        register(openApi.getComponents(), LoginRequest.class);
        register(openApi.getComponents(), UserResponse.class);
        register(openApi.getComponents(), TokenResponse.class);

        Operation operation = new Operation()
                .addTagsItem("Authentication")
                .summary("Log in with a local account")
                .description("Validates the credentials and returns an access token and a refresh token.")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(content(LoginRequest.class)))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("Logged in")
                                .content(content(TokenResponse.class)))
                        .addApiResponse("400", new ApiResponse().description("Invalid request body"))
                        .addApiResponse("401", new ApiResponse().description("Invalid email or password")));

        openApi.path(JsonLoginFilter.LOGIN_PATH, new PathItem().post(operation));
    }

    private static void register(Components components, Class<?> type) {
        ModelConverters.getInstance().read(type).forEach(components::addSchemas);
    }

    private static Content content(Class<?> type) {
        return new Content().addMediaType(
                JSON,
                new MediaType().schema(new Schema<>().$ref(SCHEMA_PREFIX + type.getSimpleName()))
        );
    }
}
