package com.streetvendor.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.controller.VendorController;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class VendorControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private VendorService vendorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn201OnSuccessfulVendorCreation() throws Exception {
        CreateVendorRequest request = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );
        VendorResponse response = new VendorResponse(UUID.randomUUID(), VendorStatus.PENDING_REVIEW, "Vendor profile created successfully.", null);

        when(vendorService.createVendor(any(CreateVendorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor profile created successfully."))
                .andExpect(jsonPath("$.data.vendorId").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.message").value("Vendor profile created successfully."));
    }

    @Test
    void shouldReturn400OnValidationFailure() throws Exception {
        CreateVendorRequest request = new CreateVendorRequest(
                "", // blank businessName
                "Owner",
                "", // blank phone
                "Indian",
                "Delicious food",
                new BigDecimal("100.0"), // latitude > 90
                new BigDecimal("200.0"), // longitude > 180
                "123 Main St"
        );

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn409OnDuplicateVendor() throws Exception {
        CreateVendorRequest request = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );

        when(vendorService.createVendor(any(CreateVendorRequest.class)))
                .thenThrow(new com.streetvendor.common.exception.ConflictException("Vendor profile already exists"));

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Vendor profile already exists"));
    }

    @Test
    void shouldReturn403WhenNonVendorTriesToCreate() throws Exception {
        CreateVendorRequest request = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );

        when(vendorService.createVendor(any(CreateVendorRequest.class)))
                .thenThrow(new com.streetvendor.common.exception.ForbiddenException("Only vendors can create vendor profiles"));

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only vendors can create vendor profiles"));
    }
}