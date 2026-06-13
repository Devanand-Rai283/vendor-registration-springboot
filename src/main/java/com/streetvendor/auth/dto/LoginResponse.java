package com.streetvendor.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(

        @JsonProperty("accessToken")
        String accessToken,

        @JsonProperty("tokenType")
        String tokenType,

        @JsonProperty("expiresIn")
        long expiresIn
) {
}
