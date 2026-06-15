package com.streetvendor.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.dto.response.MenuItemResponse;
import com.streetvendor.menu.service.MenuItemService;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class MenuItemControllerTest extends AbstractIntegrationTest {

    @MockitoBean private MenuItemService menuItemService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID itemId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    private MenuItemResponse response() {
        return new MenuItemResponse(itemId, categoryId, vendorId, "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, true, Instant.now(), Instant.now());
    }

    @Test
    void shouldCreateMenuItem() throws Exception {
        when(menuItemService.createItem(any())).thenReturn(response());
        var request = new CreateMenuItemRequest(categoryId, "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, true);

        mockMvc.perform(post("/api/menu/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Menu item created successfully"))
                .andExpect(jsonPath("$.data.name").value("Samosa"))
                .andExpect(jsonPath("$.data.price").value(25.00));
    }

    @Test
    void shouldRejectNegativePriceOnCreate() throws Exception {
        var request = new CreateMenuItemRequest(categoryId, "Samosa", null, new BigDecimal("-0.01"), null, null, true);

        mockMvc.perform(post("/api/menu/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetItems() throws Exception {
        when(menuItemService.getItems()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/menu/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldGetItemById() throws Exception {
        when(menuItemService.getItemById(itemId)).thenReturn(response());

        mockMvc.perform(get("/api/menu/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId.toString()));
    }

    @Test
    void shouldUpdateItem() throws Exception {
        when(menuItemService.updateItem(any(), any())).thenReturn(response());
        var request = new UpdateMenuItemRequest(categoryId, "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, true);

        mockMvc.perform(put("/api/menu/items/" + itemId).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Menu item updated successfully"));
    }

    @Test
    void shouldPatchAvailability() throws Exception {
        MenuItemResponse unavailable = new MenuItemResponse(itemId, categoryId, vendorId, "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, false, Instant.now(), Instant.now());
        when(menuItemService.updateAvailability(any(), any())).thenReturn(unavailable);

        mockMvc.perform(patch("/api/menu/items/" + itemId + "/availability").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void shouldDeleteItem() throws Exception {
        mockMvc.perform(delete("/api/menu/items/" + itemId))
                .andExpect(status().isNoContent());

        verify(menuItemService).deleteItem(itemId);
    }

    @Test
    void shouldReturn404ForMissingItem() throws Exception {
        doThrow(new ResourceNotFoundException("Menu item not found")).when(menuItemService).deleteItem(itemId);

        mockMvc.perform(delete("/api/menu/items/" + itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Menu item not found"));
    }
}
