package com.streetvendor.menu.controller;

import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.dto.response.MenuItemResponse;
import com.streetvendor.menu.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu/items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(@Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse response = menuItemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getItems() {
        return ResponseEntity.ok(ApiResponse.success("Menu items retrieved", menuItemService.getItems()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItemById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Menu item retrieved", menuItemService.getItemById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse response = menuItemService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated successfully", response));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemAvailabilityRequest request) {
        MenuItemResponse response = menuItemService.updateAvailability(id, request);
        return ResponseEntity.ok(ApiResponse.success("Menu item availability updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        menuItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
