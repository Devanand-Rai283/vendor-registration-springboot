package com.streetvendor.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class VendorSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void vendorsTableShouldExist() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'VENDORS'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertTrue(count != null && count > 0, "vendors table should exist");
    }

    @Test
    void vendorsShouldHaveAllRequiredColumns() {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = 'VENDORS' ORDER BY ordinal_position";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class);
        assertTrue(columns.contains("ID"), "vendors should have id column");
        assertTrue(columns.contains("USER_ID"), "vendors should have user_id column");
        assertTrue(columns.contains("BUSINESS_NAME"), "vendors should have business_name column");
        assertTrue(columns.contains("OWNER_NAME"), "vendors should have owner_name column");
        assertTrue(columns.contains("PHONE"), "vendors should have phone column");
        assertTrue(columns.contains("FOOD_TYPE"), "vendors should have food_type column");
        assertTrue(columns.contains("DESCRIPTION"), "vendors should have description column");
        assertTrue(columns.contains("LATITUDE"), "vendors should have latitude column");
        assertTrue(columns.contains("LONGITUDE"), "vendors should have longitude column");
        assertTrue(columns.contains("ADDRESS"), "vendors should have address column");
        assertTrue(columns.contains("STATUS"), "vendors should have status column");
        assertTrue(columns.contains("AVERAGE_RATING"), "vendors should have average_rating column");
        assertTrue(columns.contains("TOTAL_REVIEWS"), "vendors should have total_reviews column");
        assertTrue(columns.contains("CREATED_AT"), "vendors should have created_at column");
        assertTrue(columns.contains("UPDATED_AT"), "vendors should have updated_at column");
        assertTrue(columns.contains("REJECTION_REASON"), "vendors should have rejection_reason column");
    }


}