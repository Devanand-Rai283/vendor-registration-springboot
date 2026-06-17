package com.streetvendor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("vendor-test")
class RedisActuatorHealthIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private RedisConnectionFactory connectionFactory;

    @MockitoBean
    private RedisConnection connection;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void whenRedisReachable_actuatorHealthReturnsUp() throws Exception {
        when(connectionFactory.getConnection()).thenReturn(connection);
        org.springframework.data.redis.connection.RedisServerCommands serverCommands = mock(org.springframework.data.redis.connection.RedisServerCommands.class);
        when(connection.serverCommands()).thenReturn(serverCommands);
        java.util.Properties props = new java.util.Properties();
        props.put("redis_version", "7.0.0");
        when(serverCommands.info()).thenReturn(props);

        mockMvc.perform(get("/actuator/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("UP"));
    }

    @Test
    void whenRedisUnreachable_actuatorHealthReturnsDown() throws Exception {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.serverCommands()).thenThrow(new RuntimeException("Redis unreachable"));

        mockMvc.perform(get("/actuator/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.redis.status").value("DOWN"));
    }
}
