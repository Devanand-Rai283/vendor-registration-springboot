package com.streetvendor.discovery.service;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.config.AbstractRedisIntegrationTest;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import com.streetvendor.menu.service.MenuItemService;
import com.streetvendor.vendor.service.VendorService;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Transactional
public class DiscoveryServiceCacheIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private VendorService vendorService;

    @MockitoSpyBean
    private VendorRepository vendorRepository;

    @MockitoSpyBean
    private MenuCategoryRepository menuCategoryRepository;

    @MockitoSpyBean
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User vendorUser;
    private Vendor testVendor;
    private MenuCategory testCategory;
    private MenuItem testItem;

    private static final double LAT = 12.9716;
    private static final double LNG = 77.5946;

    @BeforeEach
    void setupData() {
        // Clear redis between runs
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        vendorUser = new User(UUID.randomUUID(), "cache-test-vendor@example.com", "secure-hash", Role.VENDOR, AccountStatus.ACTIVE);
        vendorUser = userRepository.save(vendorUser);

        testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Cache Test Vendor");
        testVendor.setLatitude(BigDecimal.valueOf(LAT));
        testVendor.setLongitude(BigDecimal.valueOf(LNG));
        testVendor.setStatus(VendorStatus.APPROVED);
        testVendor.setFoodType("Burgers");
        testVendor.setAddress("456 Cache Lane");
        testVendor.setAverageRating(BigDecimal.valueOf(4.5));
        testVendor = vendorRepository.save(testVendor);

        testCategory = new MenuCategory(UUID.randomUUID(), testVendor, "Main Course", 0);
        testCategory = menuCategoryRepository.save(testCategory);

        testItem = new MenuItem(UUID.randomUUID(), testCategory, testVendor, "Big Cache Burger", BigDecimal.valueOf(9.99));
        testItem.setAvailable(true);
        testItem.setDescription("A very cacheable burger");
        testItem.setDietaryTag("Veg");
        testItem.setImageUrl("http://example.com/burger.jpg");
        testItem = menuItemRepository.save(testItem);
    }

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(User user, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of(new SimpleGrantedAuthority(authority))));
    }

    @Test
    void testVendorSearchCacheMiss() {
        // Ensure cache is empty
        String keyPattern = "search:vendors:*";
        assertThat(redisTemplate.keys(keyPattern)).isEmpty();

        // Perform the first call (cache miss)
        NearbyVendorResponse response = discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.vendors()).hasSize(1);
        assertThat(response.vendors().get(0).businessName()).isEqualTo("Cache Test Vendor");

        // Verify repository was queried
        verify(vendorRepository, atLeastOnce()).findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any());

        // Verify cache is populated (keys exist)
        assertThat(redisTemplate.keys(keyPattern)).isNotEmpty();
    }

    @Test
    void testVendorSearchCacheHit() {
        // First call to populate cache
        discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);

        // Reset spy invocation count
        clearInvocations(vendorRepository);

        // Perform second call (cache hit)
        NearbyVendorResponse response = discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.vendors()).hasSize(1);
        assertThat(response.vendors().get(0).businessName()).isEqualTo("Cache Test Vendor");

        // Verify repository was NOT called again
        verify(vendorRepository, never()).findByStatusAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testVendorMenuCacheMiss() {
        UUID vendorId = testVendor.getId();
        String expectedKey = "vendor:menu:" + vendorId;
        assertThat(redisTemplate.hasKey(expectedKey)).isFalse();

        // Perform the first call (cache miss)
        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertThat(response).isNotNull();
        assertThat(response.vendorId()).isEqualTo(vendorId);
        assertThat(response.vendorName()).isEqualTo("Cache Test Vendor");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).name()).isEqualTo("Main Course");
        assertThat(response.categories().get(0).items()).hasSize(1);
        assertThat(response.categories().get(0).items().get(0).name()).isEqualTo("Big Cache Burger");

        // Verify repositories were queried
        verify(vendorRepository, atLeastOnce()).findById(vendorId);
        verify(menuCategoryRepository, atLeastOnce()).findByVendorIdOrderByDisplayOrderAsc(vendorId);
        verify(menuItemRepository, atLeastOnce()).findByVendorIdAndIsAvailableTrue(vendorId);

        // Verify cache is populated (key exists)
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
    }

    @Test
    void testVendorMenuCacheHit() {
        UUID vendorId = testVendor.getId();

        // First call to populate cache
        discoveryService.getVendorMenu(vendorId);

        // Reset spies
        clearInvocations(vendorRepository, menuCategoryRepository, menuItemRepository);

        // Perform second call (cache hit)
        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        assertThat(response).isNotNull();
        assertThat(response.vendorId()).isEqualTo(vendorId);
        assertThat(response.vendorName()).isEqualTo("Cache Test Vendor");

        // Verify repositories were NOT called again
        verify(vendorRepository, never()).findById(any());
        verify(menuCategoryRepository, never()).findByVendorIdOrderByDisplayOrderAsc(any());
        verify(menuItemRepository, never()).findByVendorIdAndIsAvailableTrue(any());
    }

    @Test
    void testCacheKeysExist() {
        UUID vendorId = testVendor.getId();

        // Run both methods to populate cache
        discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);
        discoveryService.getVendorMenu(vendorId);

        // Verify search key pattern exists and vendor menu key exists
        String searchKeyPattern = "search:vendors:*";
        assertThat(redisTemplate.keys(searchKeyPattern)).hasSize(1);

        String menuKey = "vendor:menu:" + vendorId;
        assertThat(redisTemplate.hasKey(menuKey)).isTrue();
    }

    @Test
    void testSerializationAndTypePreservation() {
        UUID vendorId = testVendor.getId();

        // 1. Run vendor menu search to populate cache
        discoveryService.getVendorMenu(vendorId);

        // Retrieve the cached menu response DTO from discoveryService (which will read from Redis)
        VendorMenuResponseDto cachedMenu = discoveryService.getVendorMenu(vendorId);

        // Confirm type preservation (remain DTOs, not LinkedHashMap)
        assertThat(cachedMenu).isInstanceOf(VendorMenuResponseDto.class);
        assertThat(cachedMenu.vendorId()).isEqualTo(vendorId);
        assertThat(cachedMenu.categories()).isNotEmpty();
        assertThat(cachedMenu.categories().get(0)).isInstanceOf(com.streetvendor.discovery.dto.MenuCategoryResponseDto.class);
        assertThat(cachedMenu.categories().get(0).items().get(0)).isInstanceOf(com.streetvendor.discovery.dto.MenuItemResponseDto.class);

        // Check nested collections and UUIDs
        assertThat(cachedMenu.categories().get(0).id()).isNotNull().isInstanceOf(UUID.class);
        assertThat(cachedMenu.categories().get(0).items().get(0).id()).isNotNull().isInstanceOf(UUID.class);

        // 2. Run vendor search to populate cache
        discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);

        // Fetch search results from cache (second call)
        NearbyVendorResponse cachedSearch = discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);

        assertThat(cachedSearch).isNotNull();
        assertThat(cachedSearch.vendors()).isNotEmpty();
        assertThat(cachedSearch.vendors().get(0)).isInstanceOf(VendorSummaryResponse.class);
        assertThat(cachedSearch.vendors().get(0).id()).isEqualTo(vendorId);
    }

    @Test
    void testVendorMenuCacheInvalidation() {
        UUID vendorId = testVendor.getId();
        String expectedKey = "vendor:menu:" + vendorId;

        // 1. Populate the cache first
        VendorMenuResponseDto responseBefore = discoveryService.getVendorMenu(vendorId);
        assertThat(responseBefore).isNotNull();

        // 2. Verify key exists in Redis
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();

        // 3. Set authentication context as the vendor owner
        setAuthentication(vendorUser, "ROLE_VENDOR");

        // 4. Modify the menu through the actual production service (e.g. toggle availability)
        assertThat(testItem.isAvailable()).isTrue();
        UpdateMenuItemAvailabilityRequest updateRequest = new UpdateMenuItemAvailabilityRequest(false);
        menuItemService.updateAvailability(testItem.getId(), updateRequest);

        // 5. Verify the key no longer exists in Redis (Evicted!)
        assertThat(redisTemplate.hasKey(expectedKey)).isFalse();

        // 6. Reset repository spies to clear invocation history
        clearInvocations(vendorRepository, menuCategoryRepository, menuItemRepository);

        // 7. Call discoveryService.getVendorMenu(vendorId) again
        VendorMenuResponseDto responseAfter = discoveryService.getVendorMenu(vendorId);

        // 8. Verify repository was accessed again (cache miss after eviction)
        verify(vendorRepository, atLeastOnce()).findById(vendorId);
        verify(menuCategoryRepository, atLeastOnce()).findByVendorIdOrderByDisplayOrderAsc(vendorId);
        verify(menuItemRepository, atLeastOnce()).findByVendorIdAndIsAvailableTrue(vendorId);

        // 9. Verify cache was recreated
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
    }

    @Test
    void testVendorSearchCacheInvalidation() {
        // Setup a PENDING_REVIEW vendor first
        User pendingUser = new User(UUID.randomUUID(), "pending-vendor-invalidation@example.com", "secure-hash", Role.VENDOR, AccountStatus.ACTIVE);
        pendingUser = userRepository.save(pendingUser);

        Vendor pendingVendor = new Vendor(UUID.randomUUID(), pendingUser, "Pending Invalidation Vendor");
        pendingVendor.setLatitude(BigDecimal.valueOf(LAT));
        pendingVendor.setLongitude(BigDecimal.valueOf(LNG));
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);
        pendingVendor.setFoodType("Ice Cream");
        pendingVendor.setAddress("789 Cool St");
        pendingVendor.setAverageRating(BigDecimal.valueOf(4.0));
        pendingVendor = vendorRepository.save(pendingVendor);

        String searchKeyPattern = "search:vendors:*";

        // 1. Call findNearbyVendors to populate the cache
        NearbyVendorResponse searchBefore = discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);
        assertThat(searchBefore.vendors()).hasSize(1); // Only approved vendor exists in search

        // 2. Verify search cache keys exist in Redis
        assertThat(redisTemplate.keys(searchKeyPattern)).isNotEmpty();

        // 3. Authenticate as admin user to allow vendor approval
        User adminUser = new User(UUID.randomUUID(), "admin@example.com", "hash", Role.ADMIN, AccountStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);
        setAuthentication(adminUser, "ROLE_ADMIN");

        // 4. Approve the pending vendor to trigger invalidation
        vendorService.approveVendor(pendingVendor.getId());

        // 5. Verify search cache keys are removed from Redis
        assertThat(redisTemplate.keys(searchKeyPattern)).isEmpty();

        // 6. Reset repository spy invocation history
        clearInvocations(vendorRepository);

        // 7. Call findNearbyVendors again
        NearbyVendorResponse searchAfter = discoveryService.findNearbyVendors(LAT, LNG, 5.0, 0, 10);
        assertThat(searchAfter.vendors()).hasSize(2); // Should include both approved vendors now!

        // 8. Verify repository was queried again
        verify(vendorRepository, atLeastOnce()).findByStatusAndLatitudeBetweenAndLongitudeBetween(
                eq(VendorStatus.APPROVED), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any());

        // 9. Verify fresh cache generated
        assertThat(redisTemplate.keys(searchKeyPattern)).isNotEmpty();
    }
}
