package com.streetvendor.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void shouldReturn400ForValidationFailure() throws Exception {
        String invalidBody = "{}";

        MvcResult result = mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("status", "message", "timestamp", "path");
    }

    @Test
    void shouldReturn401ForUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Custom unauthorized message"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/unauthorized"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("Exception");
    }

    @Test
    void shouldReturn403ForForbidden() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/forbidden"))

                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Custom forbidden message"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/forbidden"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("Exception");
    }

    @Test
    void shouldReturn404ForResourceNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/not-found"))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vendor not found"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("stacktrace");
        assertThat(body).doesNotContain("org.springframework");
    }

    @Test
    void shouldReturn409ForConflict() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/conflict"))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Duplicate email"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/conflict"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("stacktrace");
        assertThat(body).doesNotContain("org.springframework");
    }

    @Test
    void shouldReturn500ForUnexpectedException() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/error"))

                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/error"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("stacktrace");
        assertThat(body).doesNotContain("org.springframework");
    }

    @RestController
    static class TestController {

        @PostMapping("/test/validation")
        String testValidation(@Valid @RequestBody TestDto dto) {
            return "ok";
        }

        @GetMapping("/test/unauthorized")
        String testUnauthorized() {
            throw new UnauthorizedException("Custom unauthorized message");
        }

        @GetMapping("/test/forbidden")
        String testForbidden() {
            throw new ForbiddenException("Custom forbidden message");
        }

        @GetMapping("/test/not-found")
        String testNotFound() {
            throw new ResourceNotFoundException("Vendor not found");
        }

        @GetMapping("/test/conflict")
        String testConflict() {
            throw new ConflictException("Duplicate email");
        }

        @GetMapping("/test/error")
        String testError() {
            throw new RuntimeException("Unexpected internal error");
        }
    }

    static class TestDto {

        @NotBlank(message = "Name must not be blank")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
