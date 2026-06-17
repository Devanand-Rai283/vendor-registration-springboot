package com.streetvendor.menu;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.discovery.cache.CacheKeyGenerator;
import com.streetvendor.discovery.cache.DiscoveryCacheService;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.dto.response.MenuCategoryResponse;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.service.impl.MenuCategoryServiceImpl;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceImplTest {

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private DiscoveryCacheService discoveryCacheService;

    @InjectMocks
    private MenuCategoryServiceImpl menuCategoryService;

    private UUID vendorId;
    private UUID categoryId;
    private Vendor vendor;
    private User vendorUser;
    private MenuCategory category;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        vendorUser = new User(vendorId, "vendor@example.com", "hash", Role.VENDOR, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        vendor = new Vendor(vendorId, vendorUser, "Test Business");
        vendor.setStatus(VendorStatus.APPROVED);
        category = new MenuCategory(categoryId, vendor, "Snacks", 1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setVendorAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                vendorUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setCustomerAuthentication() {
        User customerUser = new User(UUID.randomUUID(), "customer@example.com", "hash", Role.CUSTOMER, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customerUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User createOtherVendorUser() {
        return new User(UUID.randomUUID(), "other@example.com", "hash", Role.VENDOR, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
    }

    private Vendor createOtherVendor(User user) {
        return new Vendor(user.getId(), user, "Other Business");
    }

    private void setOtherVendorAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void shouldCreateCategorySuccessfully() {
        setVendorAuthentication();
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Snacks")).thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuCategoryResponse response = menuCategoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Snacks", response.getName());
        assertEquals(1, response.getDisplayOrder());
        verify(menuCategoryRepository).save(any(MenuCategory.class));
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldThrowUnauthorizedWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                menuCategoryService.createCategory(request));

        assertEquals("Not authenticated", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldThrowForbiddenWhenNonVendorUser() {
        setCustomerAuthentication();

        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                menuCategoryService.createCategory(request));

        assertEquals("Only vendors can manage menu categories", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldThrowResourceNotFoundWhenVendorProfileMissing() {
        setVendorAuthentication();
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.createCategory(request));

        assertEquals("Vendor profile not found", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldThrowConflictWhenDuplicateNameOnCreate() {
        setVendorAuthentication();
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Snacks")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                menuCategoryService.createCategory(request));

        assertEquals("Category name already exists for this vendor", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldGetAllCategoriesForVendor() {
        setVendorAuthentication();
        MenuCategory category2 = new MenuCategory(UUID.randomUUID(), vendor, "Drinks", 2);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId))
                .thenReturn(List.of(category, category2));

        List<MenuCategoryResponse> responses = menuCategoryService.getCategories();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Snacks", responses.get(0).getName());
        assertEquals("Drinks", responses.get(1).getName());
    }

    @Test
    void shouldGetCategoryById() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));

        MenuCategoryResponse response = menuCategoryService.getCategoryById(categoryId);

        assertNotNull(response);
        assertEquals(categoryId, response.getId());
        assertEquals("Snacks", response.getName());
    }

    @Test
    void shouldThrowResourceNotFoundWhenCategoryDoesNotExist() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.getCategoryById(categoryId));

        assertEquals("Category not found", exception.getMessage());
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        setVendorAuthentication();
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Fresh Snacks")).thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuCategoryResponse response = menuCategoryService.updateCategory(categoryId, request);

        assertNotNull(response);
        assertEquals("Fresh Snacks", response.getName());
        assertEquals(2, response.getDisplayOrder());
        verify(menuCategoryRepository).save(category);
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldThrowResourceNotFoundWhenUpdatingNonExistentCategory() {
        setVendorAuthentication();
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.updateCategory(categoryId, request));

        assertEquals("Category not found", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldThrowConflictWhenDuplicateNameOnUpdate() {
        setVendorAuthentication();
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Drinks", 1);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Drinks")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                menuCategoryService.updateCategory(categoryId, request));

        assertEquals("Category name already exists for this vendor", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldAllowUpdateWhenNameUnchanged() {
        setVendorAuthentication();
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Snacks", 2);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Snacks")).thenReturn(true);
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuCategoryResponse response = menuCategoryService.updateCategory(categoryId, request);

        assertNotNull(response);
        assertEquals("Snacks", response.getName());
        assertEquals(2, response.getDisplayOrder());
        verify(menuCategoryRepository).save(category);
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldDeleteCategorySuccessfully() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));

        menuCategoryService.deleteCategory(categoryId);

        verify(menuCategoryRepository).delete(category);
        verify(discoveryCacheService).evict(CacheKeyGenerator.vendorMenuKey(vendorId));
    }

    @Test
    void shouldThrowResourceNotFoundWhenDeletingNonExistentCategory() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.deleteCategory(categoryId));

        assertEquals("Category not found", exception.getMessage());
        verify(menuCategoryRepository, never()).delete(any(MenuCategory.class));
    }

    @Test
    void shouldDenyCrossVendorAccessOnGetCategory() {
        User otherVendorUser = createOtherVendorUser();
        Vendor otherVendor = createOtherVendor(otherVendorUser);
        setOtherVendorAuthentication(otherVendorUser);

        when(vendorRepository.findByUserId(otherVendorUser.getId())).thenReturn(Optional.of(otherVendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, otherVendor.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.getCategoryById(categoryId));

        assertEquals("Category not found", exception.getMessage());
    }

    @Test
    void shouldDenyCrossVendorAccessOnUpdateCategory() {
        User otherVendorUser = createOtherVendorUser();
        Vendor otherVendor = createOtherVendor(otherVendorUser);
        setOtherVendorAuthentication(otherVendorUser);

        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Hacked Category", 99);

        when(vendorRepository.findByUserId(otherVendorUser.getId())).thenReturn(Optional.of(otherVendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, otherVendor.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.updateCategory(categoryId, request));

        assertEquals("Category not found", exception.getMessage());
        verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void shouldDenyCrossVendorAccessOnDeleteCategory() {
        User otherVendorUser = createOtherVendorUser();
        Vendor otherVendor = createOtherVendor(otherVendorUser);
        setOtherVendorAuthentication(otherVendorUser);

        when(vendorRepository.findByUserId(otherVendorUser.getId())).thenReturn(Optional.of(otherVendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, otherVendor.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                menuCategoryService.deleteCategory(categoryId));

        assertEquals("Category not found", exception.getMessage());
        verify(menuCategoryRepository, never()).delete(any(MenuCategory.class));
    }

    @Test
    void shouldOnlyReturnAuthenticatedVendorCategories() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId))
                .thenReturn(List.of(category));

        List<MenuCategoryResponse> responses = menuCategoryService.getCategories();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(categoryId, responses.get(0).getId());
        verify(menuCategoryRepository).findByVendorIdOrderByDisplayOrderAsc(vendorId);
    }

    @Test
    void shouldReturnEmptyCollectionWhenVendorHasNoCategories() {
        setVendorAuthentication();

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId))
                .thenReturn(List.of());

        List<MenuCategoryResponse> responses = menuCategoryService.getCategories();

        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void shouldLinkCreatedCategoryToAuthenticatedVendor() {
        setVendorAuthentication();
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("New Category", 0);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "New Category")).thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> {
            MenuCategory saved = invocation.getArgument(0);
            assertEquals(vendor, saved.getVendor());
            return saved;
        });

        MenuCategoryResponse response = menuCategoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("New Category", response.getName());
        verify(menuCategoryRepository).save(any(MenuCategory.class));
    }

    @Test
    void shouldPreserveCreatedAtOnUpdate() {
        setVendorAuthentication();
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Updated Snacks", 3);

        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendor));
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendorId))
                .thenReturn(Optional.of(category));
        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendorId, "Updated Snacks")).thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuCategoryResponse response = menuCategoryService.updateCategory(categoryId, request);

        assertNotNull(response);
        assertEquals("Updated Snacks", response.getName());
        assertEquals(3, response.getDisplayOrder());
    }
}
