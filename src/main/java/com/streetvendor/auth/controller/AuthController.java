package com.streetvendor.auth.controller;

import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.LoginResult;
import com.streetvendor.auth.dto.LoginResponse;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.dto.RegisterResponse;
import com.streetvendor.auth.service.AuthService;
import com.streetvendor.auth.service.LogoutService;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LogoutService logoutService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationDays;

    public AuthController(AuthService authService, LogoutService logoutService) {
        this.authService = authService;
        this.logoutService = logoutService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResult result = authService.login(request);

        long maxAge = Duration.ofDays(refreshExpirationDays).getSeconds();
        response.addCookie(createRefreshCookie(result.refreshToken(), maxAge));

        return ResponseEntity.ok(ApiResponse.success("Login successful", result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        LoginResult result = authService.refresh(refreshToken);

        long maxAge = Duration.ofDays(refreshExpirationDays).getSeconds();
        response.addCookie(createRefreshCookie(result.refreshToken(), maxAge));

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutService.logout(refreshToken);
        }
        response.addCookie(createClearCookie());
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Cookie createClearCookie() {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    private Cookie createRefreshCookie(String token, long maxAge) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) maxAge);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}
