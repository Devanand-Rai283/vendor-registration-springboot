package com.streetvendor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("auth-test")
@Transactional
class AuthRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRegisterUserAndPersistToDatabase() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

        assertEquals(1, userRepository.count());
        assertTrue(userRepository.existsByEmail("newuser@example.com"));
    }

    @Test
    void shouldStoreBCryptHashedPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "hashed@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findByEmail("hashed@example.com").orElseThrow();
        assertNotNull(savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$"));
        assertTrue(passwordEncoder.matches("Password1!", savedUser.getPasswordHash()));
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() throws Exception {
        RegisterRequest request1 = new RegisterRequest(
                "unique@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                null,
                null,
                null
        );
        RegisterRequest request2 = new RegisterRequest(
                "unique@example.com",
                "Password2!",
                Role.VENDOR,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        assertEquals(1, userRepository.count());
    }

    @Test
    void shouldSetDefaultAccountStatusToActive() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "active@example.com",
                "Password1!",
                Role.VENDOR,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

        User savedUser = userRepository.findByEmail("active@example.com").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, savedUser.getAccountStatus());
    }

    @Test
    void shouldGenerateUUIDForNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "uuid@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty());

        User savedUser = userRepository.findByEmail("uuid@example.com").orElseThrow();
        assertNotNull(savedUser.getId());
    }

    @Test
    void shouldRejectAdminRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "admin@example.com",
                "Password1!",
                Role.ADMIN,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin registration is not allowed"));

        assertEquals(0, userRepository.count());
    }

    @Test
    void shouldCreateCustomerProfileForCustomerRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "customer@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        User savedUser = userRepository.findByEmail("customer@example.com").orElseThrow();
        assertTrue(customerRepository.existsByUserId(savedUser.getId()));

        Customer customer = customerRepository.findByUserId(savedUser.getId()).orElseThrow();
        assertEquals("John Doe", customer.getFullName());
        assertEquals("1234567890", customer.getPhone());
        assertEquals("123 Main St", customer.getAddress());
        assertEquals(new BigDecimal("40.71280000"), customer.getLatitude());
        assertEquals(new BigDecimal("-74.00600000"), customer.getLongitude());
    }

    @Test
    void shouldNotCreateCustomerProfileForVendorRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "vendor@example.com",
                "Password1!",
                Role.VENDOR,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        User savedUser = userRepository.findByEmail("vendor@example.com").orElseThrow();
        assertFalse(customerRepository.existsByUserId(savedUser.getId()));
    }

    @Test
    void shouldEnforceUniqueUserIdConstraint() throws Exception {
        RegisterRequest request1 = new RegisterRequest(
                "user1@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                null,
                null,
                null
        );
        RegisterRequest request2 = new RegisterRequest(
                "user2@example.com",
                "Password1!",
                Role.CUSTOMER,
                "Jane Doe",
                "0987654321",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        assertEquals(2, userRepository.count());
        assertEquals(2, customerRepository.count());
    }
}
