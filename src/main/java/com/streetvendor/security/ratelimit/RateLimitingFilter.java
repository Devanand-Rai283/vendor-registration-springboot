package com.streetvendor.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streetvendor.common.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = createObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String endpoint = resolveEndpoint(request.getMethod(), request.getRequestURI());
        if (endpoint == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);

        try {
            rateLimitService.checkRateLimit(clientIp, endpoint);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException e) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    429,
                    "Too many requests. Please try again later.",
                    request.getRequestURI());

            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }

    static String resolveEndpoint(String method, String path) {
        if ("POST".equals(method)) {
            if ("/api/auth/login".equals(path)) {
                return RateLimitRule.LOGIN;
            }
            if ("/api/auth/register".equals(path)) {
                return RateLimitRule.REGISTER;
            }
            return null;
        }

        if ("GET".equals(method)) {
            if ("/api/vendors/nearby".equals(path)) {
                return RateLimitRule.DISCOVERY;
            }
            if (path.matches("^/api/vendors/[^/]+/menu$")) {
                return RateLimitRule.DISCOVERY;
            }
            if ("/api/search".equals(path)) {
                return RateLimitRule.DISCOVERY;
            }
            return null;
        }

        return null;
    }

    static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
