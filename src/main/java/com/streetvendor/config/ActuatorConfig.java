package com.streetvendor.config;

import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

@Configuration
public class ActuatorConfig {

    @Bean
    public StatusAggregator statusAggregator() {
        return new StatusAggregator() {
            private final StatusAggregator delegate = StatusAggregator.getDefault();

            @Override
            public Status getAggregateStatus(Set<Status> statuses) {
                boolean isReadinessPath = false;
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        String uri = request.getRequestURI();
                        if (uri != null && (uri.endsWith("/readiness") || uri.endsWith("/readiness/"))) {
                            isReadinessPath = true;
                        }
                    }
                } catch (Exception e) {
                    // Fallback to false in case of any issues accessing RequestContextHolder
                }

                if (isReadinessPath && statuses.contains(Status.DOWN)) {
                    return Status.OUT_OF_SERVICE;
                }
                return delegate.getAggregateStatus(statuses);
            }
        };
    }
}
