package com.streetvendor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streetvendor.security.CustomAccessDeniedHandler;
import com.streetvendor.security.CustomAuthenticationEntryPoint;
import com.streetvendor.security.JwtAuthenticationFilter;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final Environment environment;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        Environment environment) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.environment = environment;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                var auth = http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authBuilder -> authBuilder
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/api/auth/register").permitAll()
                                                .requestMatchers("/api/auth/login").permitAll()
                                                .requestMatchers("/api/auth/refresh").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/nearby").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/*/menu").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/vendors").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.GET, "/api/vendors/me").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.POST, "/api/uploads/**").hasRole("VENDOR")
                                                .requestMatchers(HttpMethod.POST, "/api/admin/vendors/**")
                                                .hasRole("ADMIN"));

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
}
