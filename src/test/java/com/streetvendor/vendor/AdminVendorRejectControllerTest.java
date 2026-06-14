package com.streetvendor.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.dto.RejectVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class AdminVendorRejectControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private VendorService vendorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn200WhenRejectingPendingVendor() throws Exception {
        UUID vendorId = UUID.randomUUID();
        VendorResponse response = new VendorResponse(vendorId, VendorStatus.REJECTED, "Vendor rejected successfully.", "Expired FSSAI certificate");
        when(vendorService.rejectVendor(any(UUID.class), any(String.class))).thenReturn(response);

        RejectVendorRequest request = new RejectVendorRequest("Expired FSSAI certificate");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor rejected successfully."))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.message").value("Vendor rejected successfully."))
                .andExpect(jsonPath("$.data.rejectionReason").value("Expired FSSAI certificate"));
    }

    @Test
    void shouldReturn400WhenReasonIsBlank() throws Exception {
        UUID vendorId = UUID.randomUUID();
        RejectVendorRequest request = new RejectVendorRequest("");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn404WhenVendorNotFound() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.rejectVendor(eq(vendorId), any(String.class)))
                .thenThrow(new ResourceNotFoundException("Vendor not found"));

        RejectVendorRequest request = new RejectVendorRequest("Expired FSSAI certificate");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vendor not found"));
    }

    @Test
    void shouldReturn409WhenVendorNotPendingReview() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.rejectVendor(eq(vendorId), any(String.class)))
                .thenThrow(new ConflictException("Cannot reject vendor in status: APPROVED"));

        RejectVendorRequest request = new RejectVendorRequest("Expired FSSAI certificate");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot reject vendor in status: APPROVED"));
    }
}
