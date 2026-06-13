package com.streetvendor.unit;

import com.streetvendor.config.OpenApiConfig;
import com.streetvendor.config.SpringDocController;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class OpenApiMetadataTest {

    private MockMvc mockMvc;

    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openAPI = openApiConfig.streetVendorPlatformOpenAPI();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SpringDocController())
                .build();
    }

    @Test
    void shouldContainCorrectTitle() {
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI);
        org.junit.jupiter.api.Assertions.assertEquals("Street Vendor Platform API", openAPI.getInfo().getTitle());
    }

    @Test
    void shouldContainCorrectVersion() {
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI);
        org.junit.jupiter.api.Assertions.assertEquals("v1", openAPI.getInfo().getVersion());
    }

    @Test
    void shouldContainCorrectDescription() {
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI);
        org.junit.jupiter.api.Assertions.assertTrue(openAPI.getInfo().getDescription().contains("Backend APIs for the Street Vendor Platform"));
    }

    @Test
    void shouldContainBearerAuthSecurityScheme() {
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI);
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void shouldContainJwtBearerFormat() {
        org.junit.jupiter.api.Assertions.assertNotNull(openAPI);
        var bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        org.junit.jupiter.api.Assertions.assertEquals(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP, bearerAuth.getType());
        org.junit.jupiter.api.Assertions.assertEquals("bearer", bearerAuth.getScheme());
        org.junit.jupiter.api.Assertions.assertEquals("JWT", bearerAuth.getBearerFormat());
    }

    @Test
    void shouldExposeApiDocsWithoutSecurityBlock() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError("API docs endpoint blocked by security (got " + status + ")");
                    }
                });
    }
}
