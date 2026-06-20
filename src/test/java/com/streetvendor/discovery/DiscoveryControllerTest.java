package com.streetvendor.discovery;

import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorReviewResponse;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.discovery.service.DiscoveryService;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldSearchFoodsSuccessfully() throws Exception {
        FoodSearchResponseDto item = new FoodSearchResponseDto(
                UUID.randomUUID(), "Taco", "Delicious taco", BigDecimal.valueOf(5.99),
                "VEG", UUID.randomUUID(), "Maria's Tacos", "Mexican", BigDecimal.valueOf(4.5));
        Page<FoodSearchResponseDto> page = new PageImpl<>(List.of(item));

        when(discoveryService.searchFoods(eq("taco"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "taco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemName").value("Taco"))
                .andExpect(jsonPath("$.content[0].price").value(5.99))
                .andExpect(jsonPath("$.content[0].vendorName").value("Maria's Tacos"));
    }

    @Test
    void shouldSearchFoodsWithAllParameters() throws Exception {
        FoodSearchResponseDto item = new FoodSearchResponseDto(
                UUID.randomUUID(), "Burger", "Beef burger", BigDecimal.valueOf(8.99),
                "NON_VEG", UUID.randomUUID(), "Bob's Burgers", "American", BigDecimal.valueOf(4.2));
        Page<FoodSearchResponseDto> page = new PageImpl<>(List.of(item));

        when(discoveryService.searchFoods(eq("burger"), eq("American"), eq("NON_VEG"), eq(1), eq(5)))
                .thenReturn(page);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "burger")
                        .param("foodType", "American")
                        .param("dietaryTag", "NON_VEG")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemName").value("Burger"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldSearchFoodsWithDefaultPageAndSize() throws Exception {
        FoodSearchResponseDto item = new FoodSearchResponseDto(
                UUID.randomUUID(), "Pizza", "Cheese pizza", BigDecimal.valueOf(10.99),
                "VEG", UUID.randomUUID(), "Pizza Place", "Italian", BigDecimal.valueOf(4.8));
        Page<FoodSearchResponseDto> page = new PageImpl<>(
                List.of(item), PageRequest.of(0, 20), 1);

        when(discoveryService.searchFoods(eq("pizza"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "pizza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldSearchFoodsReturnEmptyResults() throws Exception {
        Page<FoodSearchResponseDto> emptyPage = Page.empty();

        when(discoveryService.searchFoods(eq("nonexistent"), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldSearchFoodsReturnPaginationMetadata() throws Exception {
        List<FoodSearchResponseDto> items = List.of(
                new FoodSearchResponseDto(UUID.randomUUID(), "Item1", "Desc1", BigDecimal.valueOf(5.00),
                        "VEG", UUID.randomUUID(), "Vendor", "Type", BigDecimal.valueOf(4.0)),
                new FoodSearchResponseDto(UUID.randomUUID(), "Item2", "Desc2", BigDecimal.valueOf(6.00),
                        "VEG", UUID.randomUUID(), "Vendor", "Type", BigDecimal.valueOf(4.0))
        );
        Page<FoodSearchResponseDto> page = new PageImpl<>(items, PageRequest.of(0, 5), 2);

        when(discoveryService.searchFoods(eq("test"), eq(null), eq(null), eq(0), eq(5)))
                .thenReturn(page);

        mockMvc.perform(get("/api/search")
                        .param("keyword", "test")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromService() throws Exception {
        when(discoveryService.searchFoods(eq(""), eq(null), eq(null), eq(0), eq(20)))
                .thenThrow(new IllegalArgumentException("Keyword is required"));

        mockMvc.perform(get("/api/search")
                        .param("keyword", ""))
                .andExpect(status().isBadRequest());
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

    @Test
    void shouldGetVendorDetailsSuccessfully() throws Exception {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.discovery.dto.VendorDetailDto dto = new com.streetvendor.discovery.dto.VendorDetailDto(
                vendorId,
                "Maria's Tacos",
                "Tasty Mexican tacos",
                "Mexican",
                BigDecimal.valueOf(4.5),
                "123 Main St",
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946)
        );

        when(discoveryService.getVendorDetails(vendorId)).thenReturn(dto);

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessName").value("Maria's Tacos"))
                .andExpect(jsonPath("$.data.description").value("Tasty Mexican tacos"))
                .andExpect(jsonPath("$.data.latitude").value(12.9716))
                .andExpect(jsonPath("$.data.longitude").value(77.5946));
    }

    @Test
    void shouldReturn404WhenVendorDetailsNotFound() throws Exception {
        UUID vendorId = UUID.randomUUID();
        when(discoveryService.getVendorDetails(vendorId))
                .thenThrow(new com.streetvendor.common.exception.ResourceNotFoundException("Vendor not found"));

        mockMvc.perform(get("/api/vendors/{id}", vendorId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/vendors/" + vendorId));
    }

    @Test
    void shouldGetVendorReviewsSuccessfully() throws Exception {
        UUID vendorId = UUID.randomUUID();
        VendorReviewResponse review = new VendorReviewResponse(
                UUID.randomUUID(), 5, "Excellent!", "John D.", Instant.now()
        );
        Page<VendorReviewResponse> page = new PageImpl<>(
                List.of(review), PageRequest.of(0, 10), 1
        );

        when(discoveryService.getVendorReviews(eq(vendorId), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/vendors/{vendorId}/ratings", vendorId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].stars").value(5))
                .andExpect(jsonPath("$.content[0].reviewText").value("Excellent!"))
                .andExpect(jsonPath("$.content[0].customerDisplayName").value("John D."))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
