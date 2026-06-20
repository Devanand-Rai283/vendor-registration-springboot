package com.streetvendor.discovery;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.discovery.service.DiscoveryService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class DiscoverySecurityTest extends AbstractSecurityTest {

    @MockitoBean
    private DiscoveryService discoveryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String vendorToken;
    private String adminToken;

    private static final VendorSummaryResponse VENDOR = new VendorSummaryResponse(
            UUID.randomUUID(),
            "Security Test Vendor",
            "Test Food",
            "123 Test St",
            BigDecimal.valueOf(4.0),
            12.9716,
            77.5946,
            0.5
    );

    private static final NearbyVendorResponse RESPONSE = new NearbyVendorResponse(
            List.of(VENDOR), 0, 10, 1, 1
    );

    private static final FoodSearchResponseDto SEARCH_ITEM = new FoodSearchResponseDto(
            UUID.randomUUID(), "Security Taco", "Test taco", BigDecimal.valueOf(4.99),
            "VEG", UUID.randomUUID(), "Security Vendor", "Mexican", BigDecimal.valueOf(4.0)
    );

    @BeforeEach
    void setUpTokens() {
        customerToken = createToken("customer@test.com", Role.CUSTOMER);
        vendorToken = createToken("vendor@test.com", Role.VENDOR);
        adminToken = createToken("admin@test.com", Role.ADMIN);
    }

    private String createToken(String email, Role role) {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE);
        userRepository.save(user);
        return jwtService.generateAccessToken(userId, email, role.name());
    }

    @Test
    void anonymousRequestShouldReturn200() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousRequestShouldReturnDiscoveryResults() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].businessName").value("Security Test Vendor"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void customerRequestShouldReturn200() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk());
    }

    @Test
    void vendorRequestShouldReturn200() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + vendorToken)
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRequestShouldReturn200() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk());
    }

    @Test
    void jwtAuthShouldNotAlterDiscoveryResults() throws Exception {
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(RESPONSE);

        String anonymousJson = mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String customerJson = mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(anonymousJson).isEqualTo(customerJson);
    }

    @Test
    void anonymousRequestWithInvalidLatShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestWithInvalidRadiusShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "0.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedCustomerWithInvalidLatShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("lat", "91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedVendorWithInvalidRadiusShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + vendorToken)
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "0.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAnonymousAccessWhenSearchingFoods() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(SEARCH_ITEM)));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "taco"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowCustomerAccessWhenSearchingFoods() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(SEARCH_ITEM)));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("keyword", "taco"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowVendorAccessWhenSearchingFoods() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(SEARCH_ITEM)));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + vendorToken)
                        .param("keyword", "taco"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminAccessWhenSearchingFoods() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(SEARCH_ITEM)));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "taco"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestWhenKeywordIsBlank() throws Exception {
        when(discoveryService.searchFoods(eq(""), eq(null), eq(null), eq(0), eq(20)))
                .thenThrow(new IllegalArgumentException("Keyword is required"));

        mockMvc.perform(get("/api/search")
                        .param("keyword", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenKeywordIsBlankForAuthenticatedUser() throws Exception {
        when(discoveryService.searchFoods(eq(""), eq(null), eq(null), eq(0), eq(20)))
                .thenThrow(new IllegalArgumentException("Keyword is required"));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("keyword", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenKeywordIsWhitespace() throws Exception {
        when(discoveryService.searchFoods(eq("   "), eq(null), eq(null), eq(0), eq(20)))
                .thenThrow(new IllegalArgumentException("Keyword is required"));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeIsZero() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(0)))
                .thenThrow(new IllegalArgumentException("Page size must be greater than 0"));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "taco")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeExceedsMaximum() throws Exception {
        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(101)))
                .thenThrow(new IllegalArgumentException("Page size must not exceed 100"));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "taco")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAnonymousAccessWhenGettingVendorDetails() throws Exception {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.discovery.dto.VendorDetailDto dto = new com.streetvendor.discovery.dto.VendorDetailDto(
                vendorId, "Security Vendor", "Desc", "Type", BigDecimal.valueOf(4.0),
                "Addr", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)
        );
        when(discoveryService.getVendorDetails(vendorId)).thenReturn(dto);

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowCustomerAccessWhenGettingVendorDetails() throws Exception {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.discovery.dto.VendorDetailDto dto = new com.streetvendor.discovery.dto.VendorDetailDto(
                vendorId, "Security Vendor", "Desc", "Type", BigDecimal.valueOf(4.0),
                "Addr", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)
        );
        when(discoveryService.getVendorDetails(vendorId)).thenReturn(dto);

        mockMvc.perform(get("/api/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowVendorAccessWhenGettingVendorDetails() throws Exception {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.discovery.dto.VendorDetailDto dto = new com.streetvendor.discovery.dto.VendorDetailDto(
                vendorId, "Security Vendor", "Desc", "Type", BigDecimal.valueOf(4.0),
                "Addr", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)
        );
        when(discoveryService.getVendorDetails(vendorId)).thenReturn(dto);

        mockMvc.perform(get("/api/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminAccessWhenGettingVendorDetails() throws Exception {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.discovery.dto.VendorDetailDto dto = new com.streetvendor.discovery.dto.VendorDetailDto(
                vendorId, "Security Vendor", "Desc", "Type", BigDecimal.valueOf(4.0),
                "Addr", BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)
        );
        when(discoveryService.getVendorDetails(vendorId)).thenReturn(dto);

        mockMvc.perform(get("/api/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAnonymousAccessToVendorMe() throws Exception {
        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAnonymousAccessWhenGettingVendorRatings() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(discoveryService.getVendorReviews(eq(vendorId), org.mockito.ArgumentMatchers.any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/vendors/{vendorId}/ratings", vendorId))
                .andExpect(status().isOk());
    }
}
