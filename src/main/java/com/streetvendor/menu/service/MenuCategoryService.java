package com.streetvendor.menu.service;

import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.dto.response.MenuCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryService {

    MenuCategoryResponse createCategory(CreateMenuCategoryRequest request);

    MenuCategoryResponse updateCategory(UUID categoryId, UpdateMenuCategoryRequest request);

    void deleteCategory(UUID categoryId);

    List<MenuCategoryResponse> getCategories();

    MenuCategoryResponse getCategoryById(UUID categoryId);
}
