package com.streetvendor.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpringDocController {

    @GetMapping("/api/docs")
    public String apiDocs() {
        return "SpringDoc endpoint";
    }
}
