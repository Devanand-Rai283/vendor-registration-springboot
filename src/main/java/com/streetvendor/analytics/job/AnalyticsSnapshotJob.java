package com.streetvendor.analytics.job;

import com.streetvendor.analytics.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class AnalyticsSnapshotJob {

    private final AnalyticsService analyticsService;
    private final String timezone;

    public AnalyticsSnapshotJob(AnalyticsService analyticsService,
                                @Value("${analytics.timezone:UTC}") String timezone) {
        this.analyticsService = analyticsService;
        this.timezone = timezone;
    }

    @Scheduled(
            cron = "${analytics.cron-expression}",
            zone = "${analytics.timezone}"
    )
    public void runSnapshotJob() {
        LocalDate yesterday = LocalDate.now(ZoneId.of(timezone)).minusDays(1);
        analyticsService.generateSnapshots(yesterday);
    }
}
