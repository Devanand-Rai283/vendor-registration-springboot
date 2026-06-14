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
class AuditLogSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void auditLogsTableShouldExist() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'AUDIT_LOGS'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertTrue(count != null && count > 0, "audit_logs table should exist");
    }

    @Test
    void auditLogsShouldHaveAllRequiredColumns() {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = 'AUDIT_LOGS' ORDER BY ordinal_position";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class);
        assertTrue(columns.contains("ID"), "audit_logs should have id column");
        assertTrue(columns.contains("EVENT_TYPE"), "audit_logs should have event_type column");
        assertTrue(columns.contains("VENDOR_ID"), "audit_logs should have vendor_id column");
        assertTrue(columns.contains("ADMIN_USER_ID"), "audit_logs should have admin_user_id column");
        assertTrue(columns.contains("DETAILS"), "audit_logs should have details column");
        assertTrue(columns.contains("CREATED_AT"), "audit_logs should have created_at column");
        assertTrue(columns.contains("UPDATED_AT"), "audit_logs should have updated_at column");
    }
}