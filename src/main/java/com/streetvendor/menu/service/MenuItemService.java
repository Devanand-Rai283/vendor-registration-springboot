package com.streetvendor.menu.service;

import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.dto.response.MenuItemResponse;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {

    MenuItemResponse createItem(CreateMenuItemRequest request);

    List<MenuItemResponse> getItems();

    MenuItemResponse getItemById(UUID itemId);

    MenuItemResponse updateItem(UUID itemId, UpdateMenuItemRequest request);

    MenuItemResponse updateAvailability(UUID itemId, UpdateMenuItemAvailabilityRequest request);

    void deleteItem(UUID itemId);
}
