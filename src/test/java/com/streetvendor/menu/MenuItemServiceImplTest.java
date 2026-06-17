package com.streetvendor.menu;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.discovery.cache.CacheKeyGenerator;
import com.streetvendor.discovery.cache.DiscoveryCacheService;
import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.menu.service.impl.MenuItemServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock private MenuItemRepository menuItemRepository;
    @Mock private MenuCategoryRepository menuCategoryRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private DiscoveryCacheService discoveryCacheService;
    @InjectMocks private MenuItemServiceImpl menuItemService;

    private UUID vendorId;
    private UUID categoryId;
    private UUID itemId;
    private User vendorUser;
    private Vendor vendor;
    private MenuCategory category;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        vendor = new Vendor(vendorId, vendorUser, "Approved Vendor");
        vendor.setStatus(VendorStatus.APPROVED);
        category = new MenuCategory(categoryId, vendor, "Snacks", 1);
        item = new MenuItem(itemId, category, vendor, "Samosa", new BigDecimal("25.00"));
        setAuthentication(vendorUser, "ROLE_VENDOR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(User user, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    @Test
    void shouldCreateMenuItemForApprovedVendor() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(categoryId, "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, true);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = menuItemService.createItem(request);

        assertEquals("Samosa", response.getName());
        assertEquals(new BigDecimal("25.00"), response.getPrice());
        assertTrue(response.isAvailable());
        verify(menuItemRepository).save(any(MenuItem.class));
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldRejectPendingVendor() {
        vendor.setStatus(VendorStatus.PENDING_REVIEW);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        assertThrows(ForbiddenException.class, () -> menuItemService.getItems());
        verify(menuItemRepository, never()).findByVendorIdOrderByCreatedAtAsc(any());
    }

    @Test
    void shouldRejectCustomerRole() {
        User customer = new User(UUID.randomUUID(), "customer@example.com", "hash", Role.CUSTOMER, AccountStatus.ACTIVE);
        setAuthentication(customer, "ROLE_CUSTOMER");

        assertThrows(ForbiddenException.class, () -> menuItemService.getItems());
        verify(vendorRepository, never()).findByUserId(any());
    }

    @Test
    void shouldPreventUsingAnotherVendorsCategory() {
        CreateMenuItemRequest request = new CreateMenuItemRequest(categoryId, "Samosa", null, BigDecimal.ZERO, null, null, true);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.createItem(request));
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void shouldListOnlyAuthenticatedVendorsItems() {
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByVendorIdOrderByCreatedAtAsc(vendorId)).thenReturn(List.of(item));

        var responses = menuItemService.getItems();

        assertEquals(1, responses.size());
        assertEquals(itemId, responses.get(0).getId());
    }

    @Test
    void shouldUpdateMenuItemAndPreserveVendorOwnership() {
        UUID newCategoryId = UUID.randomUUID();
        MenuCategory newCategory = new MenuCategory(newCategoryId, vendor, "Meals", 2);
        UpdateMenuItemRequest request = new UpdateMenuItemRequest(newCategoryId, "Masala Dosa", "Crispy", new BigDecimal("70.00"), "VEG", "https://example.com/dosa.png", false);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.of(item));
        when(menuCategoryRepository.findByIdAndVendorId(newCategoryId, vendorId)).thenReturn(Optional.of(newCategory));
        when(menuItemRepository.save(item)).thenReturn(item);

        var response = menuItemService.updateItem(itemId, request);

        assertEquals("Masala Dosa", response.getName());
        assertEquals(new BigDecimal("70.00"), response.getPrice());
        assertFalse(response.isAvailable());
        assertEquals(newCategoryId, response.getCategoryId());
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldUpdateAvailabilityOnlyForOwnedItem() {
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        var response = menuItemService.updateAvailability(itemId, new UpdateMenuItemAvailabilityRequest(false));

        assertFalse(response.isAvailable());
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldReturnNotFoundForDifferentVendorsItem() {
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.updateAvailability(itemId, new UpdateMenuItemAvailabilityRequest(false)));
    }

    @Test
    void shouldDeleteOwnedItem() {
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.of(item));

        menuItemService.deleteItem(itemId);

        verify(menuItemRepository).delete(item);
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }
}
