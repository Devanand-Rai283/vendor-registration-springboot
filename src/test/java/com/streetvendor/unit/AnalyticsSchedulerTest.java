package com.streetvendor.unit;

import com.streetvendor.analytics.job.AnalyticsSnapshotJob;
import com.streetvendor.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "analytics.cron-expression=0 0 2 * * *",
        "analytics.timezone=Asia/Kolkata",
        "spring.data.redis.ping-on-startup=false"
})
@ActiveProfiles("health-test")
class AnalyticsSchedulerTest {

    @MockitoBean
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsSnapshotJob analyticsSnapshotJob;

    @Test
    void shouldInvokeAnalyticsServiceWithYesterdayDateInConfiguredTimezone() {
        // Act
        analyticsSnapshotJob.runSnapshotJob();

        // Assert
        LocalDate expectedDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
        verify(analyticsService).generateSnapshots(expectedDate);
    }
}
