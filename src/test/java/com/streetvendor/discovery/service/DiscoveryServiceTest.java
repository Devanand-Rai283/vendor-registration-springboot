package com.streetvendor.discovery.service;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.discovery.cache.CacheKeyGenerator;
import com.streetvendor.discovery.cache.DiscoveryCacheService;
import com.streetvendor.discovery.config.DiscoveryCacheProperties;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private DiscoveryCacheService discoveryCacheService;

    @Mock
    private DiscoveryCacheProperties cacheProperties;

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getVendorMenu_ReturnsFromCache_OnCacheHit() {
        UUID vendorId = UUID.randomUUID();
        String expectedKey = CacheKeyGenerator.vendorMenuKey(vendorId);
        VendorMenuResponseDto cachedMenu = new VendorMenuResponseDto(vendorId, "Cached Vendor", List.of());

        when(discoveryCacheService.get(expectedKey, VendorMenuResponseDto.class)).thenReturn(Optional.of(cachedMenu));

        VendorMenuResponseDto result = discoveryService.getVendorMenu(vendorId);

        assertThat(result).isEqualTo(cachedMenu);
        verify(discoveryCacheService).get(expectedKey, VendorMenuResponseDto.class);
        verifyNoInteractions(vendorRepository, menuCategoryRepository, menuItemRepository);
        verify(discoveryCacheService, never()).put(anyString(), any(), any());
    }

    @Test
    void getVendorMenu_ExecutesAndCaches_OnCacheMiss() {
        UUID vendorId = UUID.randomUUID();
        String expectedKey = CacheKeyGenerator.vendorMenuKey(vendorId);
        Duration expectedTtl = Duration.ofMinutes(15);

        // Mocks for cache miss
        when(discoveryCacheService.get(expectedKey, VendorMenuResponseDto.class)).thenReturn(Optional.empty());
        when(cacheProperties.getVendorMenuTtl()).thenReturn(expectedTtl);

        // Mocks for existing logic
        User user = new User(vendorId, "vendor@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendor = new Vendor(vendorId, user, "Fresh Tacos");
        vendor.setStatus(VendorStatus.APPROVED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        MenuCategory category = new MenuCategory(UUID.randomUUID(), vendor, "Tacos", 1);
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId)).thenReturn(List.of(category));

        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, "Carne Asada Taco", new BigDecimal("5.00"));
        item.setAvailable(true);
        when(menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId)).thenReturn(List.of(item));

        // Execute
        VendorMenuResponseDto result = discoveryService.getVendorMenu(vendorId);

        // Assertions
        assertThat(result).isNotNull();
        assertThat(result.vendorId()).isEqualTo(vendorId);
        assertThat(result.vendorName()).isEqualTo("Fresh Tacos");

        // Verification
        verify(discoveryCacheService).get(expectedKey, VendorMenuResponseDto.class);
        verify(vendorRepository).findById(vendorId);
        verify(discoveryCacheService).put(eq(expectedKey), any(VendorMenuResponseDto.class), eq(expectedTtl));
    }
}
