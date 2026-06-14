package com.streetvendor.vendor;

import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.dto.VendorStatusResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class VendorMeControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private VendorService vendorService;

    @Test
    void shouldReturn200AndVendorStatusResponse() throws Exception {
        UUID vendorId = UUID.randomUUID();
        VendorStatusResponse response = new VendorStatusResponse(
                vendorId, "Test Business", VendorStatus.PENDING_REVIEW, new BigDecimal("4.2"));

        when(vendorService.getMyVendorStatus()).thenReturn(response);

        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor status retrieved"))
                .andExpect(jsonPath("$.data.id").value(vendorId.toString()))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.averageRating").value(4.2));
    }

    @Test
    void shouldReturn404WhenVendorProfileNotFound() throws Exception {
        when(vendorService.getMyVendorStatus())
                .thenThrow(new ResourceNotFoundException("Vendor profile not found"));

        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vendor profile not found"));
    }

    @Test
    void shouldReturn403WhenNotVendor() throws Exception {
        when(vendorService.getMyVendorStatus())
                .thenThrow(new ForbiddenException("Only vendors can access their vendor profile"));

        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only vendors can access their vendor profile"));
    }
}
