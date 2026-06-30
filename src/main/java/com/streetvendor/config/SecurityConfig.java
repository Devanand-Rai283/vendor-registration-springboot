package com.streetvendor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streetvendor.security.CustomAccessDeniedHandler;
import com.streetvendor.security.CustomAuthenticationEntryPoint;
import com.streetvendor.security.JwtAuthenticationFilter;
import com.streetvendor.security.ratelimit.RateLimitingFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RateLimitingFilter rateLimitingFilter;
        private final Environment environment;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitingFilter rateLimitingFilter,
                        Environment environment) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.rateLimitingFilter = rateLimitingFilter;
                this.environment = environment;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                var auth = http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authBuilder -> authBuilder
                                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                                .requestMatchers("/api/auth/register").permitAll()
                                                .requestMatchers("/api/auth/login").permitAll()
                                                .requestMatchers("/api/auth/refresh").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/nearby").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/*/menu").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/*/ratings").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/search").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/vendors").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/me").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/dashboard/metrics").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/orders").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/orders/*").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/documents").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/vendors/me/profile").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/me/profile").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/*").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/*/analytics").hasAnyRole("VENDOR", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/uploads/**").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.POST, "/api/ratings").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.POST, "/api/payments/create-order").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/payments/orders/*/verify").hasRole("CUSTOMER")
                                                .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                                                .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/admin/dashboard").hasRole("ADMIN")
                                                .requestMatchers("/api/admin/vendors/**").hasRole("ADMIN"));

                if (!isProductionProfile()) {
                        auth = auth.authorizeHttpRequests(authBuilder -> authBuilder
                                        .requestMatchers("/api/docs/**").permitAll()
                                        .requestMatchers("/swagger-ui/**").permitAll()
                                        .requestMatchers("/swagger-ui.html").permitAll());
                }

                return auth
                                .authorizeHttpRequests(authBuilder -> authBuilder
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(customAuthenticationEntryPoint(
                                                                securityObjectMapper()))
                                                .accessDeniedHandler(customAccessDeniedHandler(
                                                                securityObjectMapper())))
                                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        private boolean isProductionProfile() {
                return Arrays.asList(environment.getActiveProfiles())
                                .stream()
                                .anyMatch(profile -> profile.contains("prod"));
        }

        @Bean
        public ObjectMapper securityObjectMapper() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                return objectMapper;
        }

        @Bean
        public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper securityObjectMapper) {
                return new CustomAuthenticationEntryPoint(securityObjectMapper);
        }

        @Bean
        public CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper securityObjectMapper) {
                return new CustomAccessDeniedHandler(securityObjectMapper);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                String originsProperty = environment.getProperty("security.cors-allowed-origins");
                
                if (originsProperty == null || originsProperty.trim().isEmpty()) {
                        throw new IllegalStateException("No CORS origins configured. Configure security.cors-allowed-origins (CORS_ALLOWED_ORIGINS) before starting the application.");
                }

                List<String> origins = Arrays.stream(originsProperty.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();

                if (origins.isEmpty()) {
                        throw new IllegalStateException("No CORS origins configured. Configure security.cors-allowed-origins (CORS_ALLOWED_ORIGINS) before starting the application.");
                }

                configuration.setAllowedOrigins(origins);

                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
