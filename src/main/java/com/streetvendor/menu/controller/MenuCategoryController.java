package com.streetvendor.menu.controller;

import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.dto.response.MenuCategoryResponse;
import com.streetvendor.menu.service.MenuCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu/categories")
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;

    public MenuCategoryController(MenuCategoryService menuCategoryService) {
        this.menuCategoryService = menuCategoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createCategory(
            @Valid @RequestBody CreateMenuCategoryRequest request) {
        MenuCategoryResponse response = menuCategoryService.createCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getCategories() {
        List<MenuCategoryResponse> responses = menuCategoryService.getCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> getCategoryById(@PathVariable UUID id) {
        MenuCategoryResponse response = menuCategoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuCategoryRequest request) {
        MenuCategoryResponse response = menuCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        menuCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
