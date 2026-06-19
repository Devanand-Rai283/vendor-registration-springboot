package com.streetvendor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "dev", "test"})
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI streetVendorPlatformOpenAPI() {
        Schema<?> errorSchema = new Schema<>()
                .addProperty("status", new Schema<>().type("integer").example(429))
                .addProperty("message", new Schema<>().type("string").example("Too many requests. Please try again later."))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                .addProperty("path", new Schema<>().type("string"));

        Schema<?> accountLockedSchema = new Schema<>()
                .addProperty("status", new Schema<>().type("integer").example(403))
                .addProperty("message", new Schema<>().type("string")
                        .example("Account temporarily locked due to repeated failed login attempts. Try again in 15 minutes."))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                .addProperty("path", new Schema<>().type("string"));

        ApiResponse tooManyRequests = new ApiResponse()
                .description("Too Many Requests — rate limit exceeded. Retry-After header indicates when to retry.")
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(errorSchema)));

        ApiResponse accountLocked = new ApiResponse()
                .description("Forbidden — account is temporarily locked due to repeated failed login attempts.")
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(accountLockedSchema)));

        return new OpenAPI()
                .info(new Info()
                        .title("Street Vendor Platform API")
                        .version("v1")
                        .description("Backend APIs for the Street Vendor Platform.\n\n" +
                                "## Rate Limiting\n\n" +
                                "Public endpoints are rate-limited per IP address using Redis-backed counters:\n\n" +
                                "- **POST /api/auth/login** — 10 requests per minute\n" +
                                "- **POST /api/auth/register** — 5 requests per minute\n" +
                                "- **GET /api/vendors/nearby** — 60 requests per minute\n" +
                                "- **GET /api/vendors/{id}/menu** — 60 requests per minute\n" +
                                "- **GET /api/search** — 60 requests per minute\n\n" +
                                "When a limit is exceeded, the API returns HTTP 429 with a `Retry-After` header indicating " +
                                "the number of seconds to wait before retrying.\n\n" +
                                "## Account Lockout\n\n" +
                                "Accounts are temporarily locked after repeated failed login attempts using Redis-backed counters:\n\n" +
                                "- Failed login attempts increment a Redis counter per email address\n" +
                                "- After **5 failed attempts**, the account is locked for **15 minutes**\n" +
                                "- Lockout check executes before password verification\n" +
                                "- Successful login clears the failure counter\n" +
                                "- The API returns HTTP 403 when an account is locked\n\n" +
                                "**Lockout response does not reveal:**\n" +
                                "- Current failure count\n" +
                                "- Threshold value\n" +
                                "- Internal lock state"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addResponses("TooManyRequests", tooManyRequests)
                        .addResponses("AccountLocked", accountLocked));
    }
}
