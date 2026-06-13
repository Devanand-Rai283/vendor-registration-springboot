package com.streetvendor.integration;

import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("openapi-test")
class OpenApiConfigNonProductionTest extends AbstractIntegrationTest {

    @Test
    void shouldExposeApiDocsInNonProduction() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError("API docs endpoint blocked by security (got " + status + ")");
                    }
                });
    }

    @Test
    void shouldExposeSwaggerUiInNonProduction() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Swagger UI blocked by security (got " + status + ")");
                    }
                });
    }
}
