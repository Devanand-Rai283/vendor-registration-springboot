package com.streetvendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StreetVendorApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreetVendorApplication.class, args);
    }
}
