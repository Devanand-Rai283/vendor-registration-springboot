package com.streetvendor.menu.service.impl;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.dto.response.MenuCategoryResponse;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.service.MenuCategoryService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final VendorRepository vendorRepository;

    public MenuCategoryServiceImpl(MenuCategoryRepository menuCategoryRepository,
                                   VendorRepository vendorRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional
    public MenuCategoryResponse createCategory(CreateMenuCategoryRequest request) {
        Vendor vendor = resolveVendorFromAuth();

        if (menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendor.getId(), request.getName())) {
            throw new ConflictException("Category name already exists for this vendor");
        }

        MenuCategory category = new MenuCategory(
                UUID.randomUUID(),
                vendor,
                request.getName(),
                request.getDisplayOrder()
        );

        MenuCategory savedCategory = menuCategoryRepository.save(category);
        return toResponse(savedCategory);
    }

    @Override
    @Transactional
    public MenuCategoryResponse updateCategory(UUID categoryId, UpdateMenuCategoryRequest request) {
        Vendor vendor = resolveVendorFromAuth();

        MenuCategory category = menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendor.getId(), request.getName())
                && !category.getName().equalsIgnoreCase(request.getName())) {
            throw new ConflictException("Category name already exists for this vendor");
        }

        category.setName(request.getName());
        category.setDisplayOrder(request.getDisplayOrder());

        MenuCategory updatedCategory = menuCategoryRepository.save(category);
        return toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        Vendor vendor = resolveVendorFromAuth();

        MenuCategory category = menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        menuCategoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategories() {
        Vendor vendor = resolveVendorFromAuth();

        return menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategoryById(UUID categoryId) {
        Vendor vendor = resolveVendorFromAuth();

        MenuCategory category = menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return toResponse(category);
    }

    private Vendor resolveVendorFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        if (user.getRole() != Role.VENDOR) {
            throw new ForbiddenException("Only vendors can manage menu categories");
        }

        return vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));
    }

    private MenuCategoryResponse toResponse(MenuCategory category) {
        return new MenuCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.getCreatedAt()
        );
    }
}
