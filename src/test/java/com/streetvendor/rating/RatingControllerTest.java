package com.streetvendor.rating;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.rating.dto.CreateRatingRequest;
import com.streetvendor.rating.dto.RatingResponse;
import com.streetvendor.rating.service.RatingService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class RatingControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private RatingService ratingService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String customerToken;
    private String vendorToken;
    private String adminToken;
    private UUID customerUserId;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        User customerUser = new User(customerUserId, "customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        UUID vendorUserId = UUID.randomUUID();
        User vendorUser = new User(vendorUserId, "vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        UUID adminUserId = UUID.randomUUID();
        User adminUser = new User(adminUserId, "admin@example.com", "pass", Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(adminUser);

        customerToken = jwtService.generateAccessToken(customerUserId, "customer@example.com", "CUSTOMER");
        vendorToken = jwtService.generateAccessToken(vendorUserId, "vendor@example.com", "VENDOR");
        adminToken = jwtService.generateAccessToken(adminUserId, "admin@example.com", "ADMIN");
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 5, "Good");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsVendor() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 5, "Good");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsAdmin() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 5, "Good");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn201OnSuccessfulRating() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent");
        RatingResponse response = new RatingResponse(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                5,
                "Excellent",
                Instant.now()
        );

        when(ratingService.createRating(eq(customerUserId), any(CreateRatingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.reviewText").value("Excellent"));
    }

    @Test
    void shouldReturn400OnValidationFailure_MissingOrderId() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(null, 5, "Excellent");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400OnValidationFailure_StarsLessThanOne() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 0, "Bad");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400OnValidationFailure_StarsGreaterThanFive() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 6, "Excellent");

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409OnDuplicateReview() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 5, "Duplicate");

        when(ratingService.createRating(eq(customerUserId), any(CreateRatingRequest.class)))
                .thenThrow(new ConflictException("A rating/review already exists for this order"));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400OnNonCompletedOrder() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest(UUID.randomUUID(), 5, "Not completed");

        when(ratingService.createRating(eq(customerUserId), any(CreateRatingRequest.class)))
                .thenThrow(new IllegalArgumentException("Order status is not COMPLETED"));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
