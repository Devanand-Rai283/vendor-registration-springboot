package com.streetvendor.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import com.streetvendor.security.JwtAuthenticationFilter;
import com.streetvendor.security.ratelimit.RateLimitingFilter;

import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigFailFastTest {

    @Test
    void missingCorsConfigurationShouldFailStartup() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("security.cors-allowed-origins")).thenReturn(null);
        
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        RateLimitingFilter rateLimitFilter = mock(RateLimitingFilter.class);

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, rateLimitFilter, environment);

        IllegalStateException exception = assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);
        
        assertEquals("No CORS origins configured. Configure security.cors-allowed-origins (CORS_ALLOWED_ORIGINS) before starting the application.", exception.getMessage());
    }
    
    @Test
    void emptyCorsConfigurationShouldFailStartup() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("security.cors-allowed-origins")).thenReturn("   ,  ");
        
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        RateLimitingFilter rateLimitFilter = mock(RateLimitingFilter.class);

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, rateLimitFilter, environment);

        IllegalStateException exception = assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);
        
        assertEquals("No CORS origins configured. Configure security.cors-allowed-origins (CORS_ALLOWED_ORIGINS) before starting the application.", exception.getMessage());
    }

    @Test
    void multipleOriginsShouldBeParsedCorrectly() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("security.cors-allowed-origins")).thenReturn("https://frontend.com, http://localhost:3000 , https://another.com");
        
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        RateLimitingFilter rateLimitFilter = mock(RateLimitingFilter.class);

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, rateLimitFilter, environment);

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");
        
        List<String> origins = config.getAllowedOrigins();
        assertEquals(3, origins.size());
        assertTrue(origins.contains("https://frontend.com"));
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("https://another.com"));
    }
}
