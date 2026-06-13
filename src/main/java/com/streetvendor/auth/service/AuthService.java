package com.streetvendor.auth.service;

import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.LoginResult;
import com.streetvendor.auth.dto.LoginResponse;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResult login(LoginRequest request);

    LoginResult refresh(String rawRefreshToken);
}
