package com.streetvendor.unit;

import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.discovery.repository.FoodSearchRepository;
import com.streetvendor.discovery.service.DiscoveryServiceImpl;
import com.streetvendor.discovery.util.BoundingBoxCalculator;
import com.streetvendor.discovery.util.DistanceCalculator;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.discovery.dto.MenuCategoryResponseDto;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import com.streetvendor.discovery.dto.MenuItemResponseDto;
import com.streetvendor.discovery.dto.VendorReviewResponse;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.rating.repository.RatingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.streetvendor.discovery.cache.DiscoveryCacheService;
import com.streetvendor.discovery.config.DiscoveryCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import java.time.Duration;
import static org.mockito.Mockito.lenient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private FoodSearchRepository foodSearchRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private DiscoveryCacheService discoveryCacheService;

    @Mock
    private DiscoveryCacheProperties cacheProperties;

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(cacheProperties.getVendorSearchTtl()).thenReturn(Duration.ofMinutes(5));
        lenient().when(cacheProperties.getVendorMenuTtl()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void shouldRejectNullKeywordWhenSearchingFoods() {
        assertThatThrownBy(() -> discoveryService.searchFoods(null, null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Keyword is required");
    }

    @Test
    void shouldRejectBlankKeywordWhenSearchingFoods() {
        assertThatThrownBy(() -> discoveryService.searchFoods("", null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Keyword is required");
    }

    @Test
    void shouldRejectWhitespaceKeywordWhenSearchingFoods() {
        assertThatThrownBy(() -> discoveryService.searchFoods("   ", null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Keyword is required");
    }

    @Test
    void shouldRejectPageSizeWhenZero() {
        assertThatThrownBy(() -> discoveryService.searchFoods("chicken", null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than 0");
    }

    @Test
    void shouldRejectPageSizeWhenNegative() {
        assertThatThrownBy(() -> discoveryService.searchFoods("chicken", null, null, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than 0");
    }

    @Test
    void shouldRejectPageSizeWhenExceedsMaximum() {
        assertThatThrownBy(() -> discoveryService.searchFoods("chicken", null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must not exceed 100");
    }

    @Test
    void shouldAcceptPageSizeWhenAtMaximum() {
        Page<FoodSearchResponseDto> expectedPage = Page.empty();
        when(foodSearchRepository.searchFoods(
                eq("chicken"), eq(null), eq(null), eq(VendorStatus.APPROVED), any()))
                .thenReturn(expectedPage);

        Page<FoodSearchResponseDto> result = discoveryService.searchFoods("chicken", null, null, 0, 100);

        assertSame(expectedPage, result);
    }

    @Test
    void shouldDefaultPageWhenPageIsNull() {
        when(foodSearchRepository.searchFoods(
                eq("chicken"), eq(null), eq(null), eq(VendorStatus.APPROVED), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        discoveryService.searchFoods("chicken", null, null, null, 20);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
    }

    @Test
    void shouldDefaultSizeWhenSizeIsNull() {
        when(foodSearchRepository.searchFoods(
                eq("chicken"), eq(null), eq(null), eq(VendorStatus.APPROVED), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        discoveryService.searchFoods("chicken", null, null, 0, null);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(20, captured.getPageSize());
    }

    @Test
    void shouldDefaultPageAndSizeWhenPaginationParametersAreNull() {
        when(foodSearchRepository.searchFoods(
                eq("chicken"), eq(null), eq(null), eq(VendorStatus.APPROVED), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        discoveryService.searchFoods("chicken", null, null, null, null);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(20, captured.getPageSize());
    }

    @Test
    void shouldDelegateToRepositoryWithAllFilters() {
        Page<FoodSearchResponseDto> expectedPage = new PageImpl<>(List.of());
        when(foodSearchRepository.searchFoods(
                eq("tacos"), eq("Mexican"), eq("veg"), eq(VendorStatus.APPROVED), any()))
                .thenReturn(expectedPage);

        Page<FoodSearchResponseDto> result = discoveryService.searchFoods("tacos", "Mexican", "veg", 1, 10);

        assertSame(expectedPage, result);
        verify(foodSearchRepository).searchFoods(
                eq("tacos"), eq("Mexican"), eq("veg"), eq(VendorStatus.APPROVED), any());
    }

    @Test
    void shouldDelegateToRepositoryWithNullFilters() {
        when(foodSearchRepository.searchFoods(
                eq("burger"), eq(null), eq(null), eq(VendorStatus.APPROVED), any()))
                .thenReturn(Page.empty());

        discoveryService.searchFoods("burger", null, null, 0, 20);

        verify(foodSearchRepository).searchFoods(
                eq("burger"), eq(null), eq(null), eq(VendorStatus.APPROVED), any());
    }

    @Test
    void shouldPassPageableToRepository() {
        when(foodSearchRepository.searchFoods(
                eq("pizza"), eq(null), eq(null), eq(VendorStatus.APPROVED), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        discoveryService.searchFoods("pizza", null, null, 3, 25);

        Pageable captured = pageableCaptor.getValue();
        assertInstanceOf(PageRequest.class, captured);
        assertEquals(3, captured.getPageNumber());
        assertEquals(25, captured.getPageSize());
    }

    private Vendor createVendor(UUID id, String businessName, double lat, double lng, VendorStatus status) {
        Vendor vendor = new Vendor(id, null, businessName);
        vendor.setLatitude(BigDecimal.valueOf(lat));
        vendor.setLongitude(BigDecimal.valueOf(lng));
        vendor.setStatus(status);
        vendor.setFoodType("Test Food");
        vendor.setAddress("123 Test St");
        vendor.setAverageRating(BigDecimal.valueOf(4.0));
        return vendor;
    }

    @Test
    void shouldReturnEmptyWhenNoVendorsInBoundingBox() {
        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 5.0, 0, 20);

        assertNotNull(response);
        assertTrue(response.vendors().isEmpty());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }

    @Test
    void shouldFilterVendorsOutsideRadius() {
        UUID nearId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        Vendor near = createVendor(nearId, "Nearby", 0.0, 0.01, VendorStatus.APPROVED);
        Vendor far = createVendor(farId, "Far", 0.0, 1.0, VendorStatus.APPROVED);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(near, far)));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 10.0, 0, 20);

        assertEquals(1, response.totalElements());
        assertEquals(1, response.vendors().size());
        assertEquals(nearId, response.vendors().get(0).id());
    }

    @Test
    void shouldSortVendorsByDistanceAscending() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Vendor v1 = createVendor(id1, "Far", 0.0, 0.1, VendorStatus.APPROVED);
        Vendor v2 = createVendor(id2, "Near", 0.0, 0.01, VendorStatus.APPROVED);
        Vendor v3 = createVendor(id3, "Middle", 0.0, 0.05, VendorStatus.APPROVED);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(v1, v2, v3)));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 50.0, 0, 20);

        assertEquals(3, response.vendors().size());
        assertEquals(id2, response.vendors().get(0).id());
        assertEquals(id3, response.vendors().get(1).id());
        assertEquals(id1, response.vendors().get(2).id());
    }

    @Test
    void shouldReturnFirstPageOfResults() {
        List<Vendor> vendors = createVendorList(15, 0.0, 0.01);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(vendors));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 50.0, 0, 5);

        assertEquals(15, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(5, response.size());
        assertEquals(5, response.vendors().size());
    }

    @Test
    void shouldReturnSecondPageOfResults() {
        List<Vendor> vendors = createVendorList(15, 0.0, 0.01);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(vendors));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 50.0, 1, 5);

        assertEquals(15, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(5, response.vendors().size());
    }

    @Test
    void shouldReturnPartialLastPage() {
        List<Vendor> vendors = createVendorList(7, 0.0, 0.01);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(vendors));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 50.0, 1, 5);

        assertEquals(7, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(2, response.vendors().size());
    }

    @Test
    void shouldReturnEmptyWhenPageOutOfRange() {
        List<Vendor> vendors = createVendorList(3, 0.0, 0.01);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(vendors));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 50.0, 5, 5);

        assertTrue(response.vendors().isEmpty());
        assertEquals(3, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void shouldMapVendorFieldsToDto() {
        UUID id = UUID.randomUUID();
        Vendor vendor = createVendor(id, "Test Business", 12.9716, 77.5946, VendorStatus.APPROVED);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(vendor)));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(12.9716, 77.5946, 5.0, 0, 20);

        assertEquals(1, response.vendors().size());
        VendorSummaryResponse summary = response.vendors().get(0);
        assertEquals(id, summary.id());
        assertEquals("Test Business", summary.businessName());
        assertEquals("Test Food", summary.foodType());
        assertEquals("123 Test St", summary.address());
        assertEquals(BigDecimal.valueOf(4.0), summary.averageRating());
        assertEquals(12.9716, summary.latitude(), 1e-6);
        assertEquals(77.5946, summary.longitude(), 1e-6);
        assertEquals(0.0, summary.distanceKm(), 1e-6);
    }

    @Test
    void shouldFilterOnlyApprovedVendors() {
        Vendor approved = createVendor(UUID.randomUUID(), "Approved", 0.0, 0.01, VendorStatus.APPROVED);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(approved)));

        NearbyVendorResponse response = discoveryService.findNearbyVendors(0.0, 0.0, 5.0, 0, 20);

        assertEquals(1, response.vendors().size());
    }

    @Test
    void shouldUseUnpagedPageableForRepositoryQuery() {
        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        discoveryService.findNearbyVendors(0.0, 0.0, 5.0, 0, 20);

        Pageable captured = pageableCaptor.getValue();
        assertTrue(captured.isUnpaged());
    }

    @Test
    void shouldFilterByExactRadius() {
        double lat = 12.9716;
        double lng = 77.5946;
        Vendor justInside = createVendor(UUID.randomUUID(), "Inside", lat, lng + 0.02, VendorStatus.APPROVED);
        Vendor justOutside = createVendor(UUID.randomUUID(), "Outside", lat, lng + 0.05, VendorStatus.APPROVED);

        when(vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(justInside, justOutside)));

        double distanceInside = DistanceCalculator.calculateDistanceKm(lat, lng, lat, lng + 0.02);
        double radius = distanceInside + 0.1;

        NearbyVendorResponse response = discoveryService.findNearbyVendors(lat, lng, radius, 0, 20);

        assertEquals(1, response.vendors().size());
        assertEquals("Inside", response.vendors().get(0).businessName());
    }

    @Test
    void shouldReturnVendorMenuWhenVendorApproved() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Test Vendor", 0.0, 0.0, VendorStatus.APPROVED);

        UUID catId = UUID.randomUUID();
        MenuCategory category = new MenuCategory(catId, vendor, "Beverages", 1);

        UUID itemId = UUID.randomUUID();
        MenuItem item = new MenuItem(itemId, category, vendor, "Coffee", BigDecimal.valueOf(3.50));
        item.setDescription("Hot coffee");
        item.setDietaryTag("veg");
        item.setImageUrl("http://example.com/coffee.jpg");
        item.setAvailable(true);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId)).thenReturn(List.of(item));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId)).thenReturn(List.of(category));

        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertNotNull(response);
        assertEquals(vendorId, response.vendorId());
        assertEquals("Test Vendor", response.vendorName());
        assertEquals(1, response.categories().size());

        MenuCategoryResponseDto catDto = response.categories().get(0);
        assertEquals(catId, catDto.id());
        assertEquals("Beverages", catDto.name());
        assertEquals(1, catDto.displayOrder());
        assertEquals(1, catDto.items().size());

        MenuItemResponseDto itemDto = catDto.items().get(0);
        assertEquals(itemId, itemDto.id());
        assertEquals("Coffee", itemDto.name());
        assertEquals("Hot coffee", itemDto.description());
        assertEquals(BigDecimal.valueOf(3.50), itemDto.price());
        assertEquals("veg", itemDto.dietaryTag());
        assertEquals("http://example.com/coffee.jpg", itemDto.imageUrl());
        assertTrue(itemDto.available());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorNotFound() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discoveryService.getVendorMenu(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorPending() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Pending Vendor", 0.0, 0.0, VendorStatus.PENDING_REVIEW);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorMenu(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorRejected() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Rejected Vendor", 0.0, 0.0, VendorStatus.REJECTED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorMenu(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldIncludeOnlyAvailableItems() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Test Vendor", 0.0, 0.0, VendorStatus.APPROVED);

        MenuCategory category = new MenuCategory(UUID.randomUUID(), vendor, "Main", 1);

        MenuItem availableItem = new MenuItem(UUID.randomUUID(), category, vendor, "Pizza", BigDecimal.valueOf(10.00));
        availableItem.setAvailable(true);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId)).thenReturn(List.of(availableItem));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId)).thenReturn(List.of(category));

        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertEquals(1, response.categories().size());
        assertEquals(1, response.categories().get(0).items().size());
        assertTrue(response.categories().get(0).items().get(0).available());
    }

    @Test
    void shouldExcludeEmptyCategories() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Test Vendor", 0.0, 0.0, VendorStatus.APPROVED);

        MenuCategory populatedCat = new MenuCategory(UUID.randomUUID(), vendor, "Populated", 1);
        MenuCategory emptyCat = new MenuCategory(UUID.randomUUID(), vendor, "Empty", 2);

        MenuItem item = new MenuItem(UUID.randomUUID(), populatedCat, vendor, "Burger", BigDecimal.valueOf(8.00));
        item.setAvailable(true);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId)).thenReturn(List.of(item));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId)).thenReturn(List.of(populatedCat, emptyCat));

        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertEquals(1, response.categories().size());
        assertEquals("Populated", response.categories().get(0).name());
    }

    @Test
    void shouldPreserveCategoryDisplayOrder() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Test Vendor", 0.0, 0.0, VendorStatus.APPROVED);

        MenuCategory cat1 = new MenuCategory(UUID.randomUUID(), vendor, "Desserts", 1);
        MenuCategory cat2 = new MenuCategory(UUID.randomUUID(), vendor, "Main Course", 2);
        MenuCategory cat3 = new MenuCategory(UUID.randomUUID(), vendor, "Appetizers", 3);

        MenuItem item1 = new MenuItem(UUID.randomUUID(), cat1, vendor, "Cake", BigDecimal.valueOf(5.00));
        item1.setAvailable(true);
        MenuItem item2 = new MenuItem(UUID.randomUUID(), cat2, vendor, "Steak", BigDecimal.valueOf(15.00));
        item2.setAvailable(true);
        MenuItem item3 = new MenuItem(UUID.randomUUID(), cat3, vendor, "Salad", BigDecimal.valueOf(7.00));
        item3.setAvailable(true);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId)).thenReturn(List.of(item1, item2, item3));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId)).thenReturn(List.of(cat1, cat2, cat3));

        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertEquals(3, response.categories().size());
        assertEquals(1, response.categories().get(0).displayOrder());
        assertEquals("Desserts", response.categories().get(0).name());
        assertEquals(2, response.categories().get(1).displayOrder());
        assertEquals("Main Course", response.categories().get(1).name());
        assertEquals(3, response.categories().get(2).displayOrder());
        assertEquals("Appetizers", response.categories().get(2).name());
    }

    private List<Vendor> createVendorList(int count, double baseLat, double baseLng) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createVendor(
                        UUID.randomUUID(),
                        "Vendor " + i,
                        baseLat,
                        baseLng + (i + 1) * 0.001,
                        VendorStatus.APPROVED))
                .toList();
    }

    @Test
    void shouldReturnVendorDetailsWhenVendorApproved() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Mama Sara's Kitchen", 1.2921, 36.8219, VendorStatus.APPROVED);
        vendor.setDescription("Delicious Swahili grills");

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        com.streetvendor.discovery.dto.VendorDetailDto details = discoveryService.getVendorDetails(vendorId);

        assertNotNull(details);
        assertEquals(vendorId, details.id());
        assertEquals("Mama Sara's Kitchen", details.businessName());
        assertEquals("Delicious Swahili grills", details.description());
        assertEquals("Test Food", details.foodType());
        assertEquals(BigDecimal.valueOf(4.0), details.averageRating());
        assertEquals("123 Test St", details.address());
        assertEquals(BigDecimal.valueOf(1.2921), details.latitude());
        assertEquals(BigDecimal.valueOf(36.8219), details.longitude());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorNotFoundForDetails() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discoveryService.getVendorDetails(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorPendingForDetails() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Pending Vendor", 0.0, 0.0, VendorStatus.PENDING_REVIEW);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorDetails(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorRejectedForDetails() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Rejected Vendor", 0.0, 0.0, VendorStatus.REJECTED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorDetails(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorSuspendedForDetails() {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.auth.entity.User user = new com.streetvendor.auth.entity.User(
                UUID.randomUUID(), "vendor@test.com", "password",
                com.streetvendor.auth.entity.Role.VENDOR,
                com.streetvendor.auth.entity.AccountStatus.SUSPENDED
        );
        Vendor vendor = new Vendor(vendorId, user, "Suspended Vendor");
        vendor.setLatitude(BigDecimal.valueOf(0.0));
        vendor.setLongitude(BigDecimal.valueOf(0.0));
        vendor.setStatus(VendorStatus.APPROVED);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorDetails(vendorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorNotFoundForReviews() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discoveryService.getVendorReviews(vendorId, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorPendingForReviews() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Pending Vendor", 0.0, 0.0, VendorStatus.PENDING_REVIEW);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorReviews(vendorId, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorRejectedForReviews() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = createVendor(vendorId, "Rejected Vendor", 0.0, 0.0, VendorStatus.REJECTED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorReviews(vendorId, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenVendorSuspendedForReviews() {
        UUID vendorId = UUID.randomUUID();
        com.streetvendor.auth.entity.User user = new com.streetvendor.auth.entity.User(
                UUID.randomUUID(), "vendor@test.com", "password",
                com.streetvendor.auth.entity.Role.VENDOR,
                com.streetvendor.auth.entity.AccountStatus.SUSPENDED
        );
        Vendor vendor = new Vendor(vendorId, user, "Suspended Vendor");
        vendor.setLatitude(BigDecimal.valueOf(0.0));
        vendor.setLongitude(BigDecimal.valueOf(0.0));
        vendor.setStatus(VendorStatus.APPROVED);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> discoveryService.getVendorReviews(vendorId, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
