package com.streetvendor.menu.service.impl;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.dto.response.MenuItemResponse;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.menu.service.MenuItemService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final VendorRepository vendorRepository;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository,
                               MenuCategoryRepository menuCategoryRepository,
                               VendorRepository vendorRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional
    public MenuItemResponse createItem(CreateMenuItemRequest request) {
        Vendor vendor = resolveApprovedVendorFromAuth();
        MenuCategory category = resolveOwnedCategory(request.getCategoryId(), vendor);

        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, request.getName(), request.getPrice());
        item.setDescription(request.getDescription());
        item.setDietaryTag(request.getDietaryTag());
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.getAvailable() == null || request.getAvailable());

        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getItems() {
        Vendor vendor = resolveApprovedVendorFromAuth();
        return menuItemRepository.findByVendorIdOrderByCreatedAtAsc(vendor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getItemById(UUID itemId) {
        Vendor vendor = resolveApprovedVendorFromAuth();
        return toResponse(resolveOwnedItem(itemId, vendor));
    }

    @Override
    @Transactional
    public MenuItemResponse updateItem(UUID itemId, UpdateMenuItemRequest request) {
        Vendor vendor = resolveApprovedVendorFromAuth();
        MenuItem item = resolveOwnedItem(itemId, vendor);
        MenuCategory category = resolveOwnedCategory(request.getCategoryId(), vendor);

        item.setCategory(category);
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setDietaryTag(request.getDietaryTag());
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.getAvailable());

        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public MenuItemResponse updateAvailability(UUID itemId, UpdateMenuItemAvailabilityRequest request) {
        Vendor vendor = resolveApprovedVendorFromAuth();
        MenuItem item = resolveOwnedItem(itemId, vendor);
        item.setAvailable(request.getAvailable());
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(UUID itemId) {
        Vendor vendor = resolveApprovedVendorFromAuth();
        MenuItem item = resolveOwnedItem(itemId, vendor);
        menuItemRepository.delete(item);
    }

    private Vendor resolveApprovedVendorFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        if (user.getRole() != Role.VENDOR) {
            throw new ForbiddenException("Only vendors can manage menu items");
        }

        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));

        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new ForbiddenException("Only approved vendors can manage menu items");
        }

        return vendor;
    }

    private MenuCategory resolveOwnedCategory(UUID categoryId, Vendor vendor) {
        return menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private MenuItem resolveOwnedItem(UUID itemId, Vendor vendor) {
        return menuItemRepository.findByIdAndVendorId(itemId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getCategory().getId(),
                item.getVendor().getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getDietaryTag(),
                item.getImageUrl(),
                item.isAvailable(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
