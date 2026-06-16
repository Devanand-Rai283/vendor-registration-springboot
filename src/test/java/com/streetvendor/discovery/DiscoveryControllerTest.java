package com.streetvendor.discovery;

import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.discovery.service.DiscoveryService;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class DiscoveryControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private DiscoveryService discoveryService;

    @Test
    void shouldReturn200WithVendors() throws Exception {
        VendorSummaryResponse vendor = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Maria's Tacos",
                "Mexican",
                "123 Main St",
                BigDecimal.valueOf(4.5),
                12.9716,
                77.5946,
                0.42
        );
        NearbyVendorResponse response = new NearbyVendorResponse(
                List.of(vendor), 0, 10, 1, 1
        );

        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946")
                        .param("radius", "5.0")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].businessName").value("Maria's Tacos"))
                .andExpect(jsonPath("$.vendors[0].distanceKm").value(0.42))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturn200WithEmptyResults() throws Exception {
        NearbyVendorResponse emptyResponse = new NearbyVendorResponse(
                List.of(), 0, 10, 0, 0
        );

        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "1.0")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldUseDefaultValues() throws Exception {
        NearbyVendorResponse emptyResponse = new NearbyVendorResponse(
                List.of(), 0, 10, 0, 0
        );

        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLatitudeBelowMin() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "-91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLatitudeAboveMax() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLongitudeBelowMin() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "-181.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLongitudeAboveMax() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "181.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectZeroRadius() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "0.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeRadius() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "-1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectZeroSize() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeSize() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "0.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptBoundaryValues() throws Exception {
        NearbyVendorResponse response = new NearbyVendorResponse(List.of(), 0, 10, 0, 0);
        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "90.0")
                        .param("lng", "180.0")
                        .param("radius", "1.0")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WithErrorBodyOnInvalidInput() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/vendors/nearby"));
    }

    @Test
    void shouldReturn200WithDefaultPageAndSize() throws Exception {
        NearbyVendorResponse response = new NearbyVendorResponse(
                List.of(), 0, 10, 0, 0
        );

        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void shouldSupportMultiplePages() throws Exception {
        List<VendorSummaryResponse> vendors = List.of(
                new VendorSummaryResponse(UUID.randomUUID(), "V1", "A", "Addr1", BigDecimal.valueOf(4.0), 1.0, 1.0, 0.1),
                new VendorSummaryResponse(UUID.randomUUID(), "V2", "B", "Addr2", BigDecimal.valueOf(3.0), 1.0, 1.0, 0.2)
        );
        NearbyVendorResponse page0 = new NearbyVendorResponse(vendors, 0, 2, 4, 2);

        when(discoveryService.findNearbyVendors(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(page0);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "1.0")
                        .param("lng", "1.0")
                        .param("radius", "5.0")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
