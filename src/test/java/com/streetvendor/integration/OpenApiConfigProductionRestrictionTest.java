package com.streetvendor.integration;

import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("prod-test")
class OpenApiConfigProductionRestrictionTest extends AbstractSecurityTest {

    @Test
    void shouldNotExposeApiDocsInProduction() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        throw new AssertionError("Expected non-200 status but got 200");
                    }
                });
    }

    @Test
    void shouldNotExposeSwaggerUiInProduction() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        throw new AssertionError("Expected non-200 status but got 200");
                    }
                });
    }
}
