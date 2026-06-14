package com.streetvendor.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "r2")
public class R2Properties {

    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final String region;
    private final String endpoint;

    public R2Properties(@NotBlank String accessKey,
                        @NotBlank String secretKey,
                        @NotBlank String bucketName,
                        @NotBlank String region,
                        @NotBlank String endpoint) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
