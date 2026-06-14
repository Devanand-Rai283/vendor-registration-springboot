package com.streetvendor.vendor;

import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class AdminVendorControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private VendorService vendorService;

    @Test
    void shouldReturn200WhenApprovingPendingVendor() throws Exception {
        UUID vendorId = UUID.randomUUID();
        VendorResponse response = new VendorResponse(vendorId, VendorStatus.APPROVED, "Vendor approved successfully.", null);
        when(vendorService.approveVendor(any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor approved successfully."))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.message").value("Vendor approved successfully."));
    }

    @Test
    void shouldReturn404WhenVendorNotFound() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.approveVendor(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Vendor not found"));

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vendor not found"));
    }

    @Test
    void shouldReturn409WhenVendorNotPendingReview() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.approveVendor(any(UUID.class)))
                .thenThrow(new ConflictException("Cannot approve vendor in status: APPROVED"));

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot approve vendor in status: APPROVED"));
    }
}
