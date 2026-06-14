package com.streetvendor.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.dto.response.MenuCategoryResponse;
import com.streetvendor.menu.service.MenuCategoryService;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
class MenuCategoryControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private MenuCategoryService menuCategoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID categoryId = UUID.randomUUID();

    @Test
    void shouldReturn201OnSuccessfulCategoryCreation() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);
        MenuCategoryResponse response = new MenuCategoryResponse(categoryId, "Snacks", 1, Instant.now());

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.name").value("Snacks"))
                .andExpect(jsonPath("$.data.displayOrder").value(1));
    }

    @Test
    void shouldReturn400OnValidationFailure() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("", null);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn409OnDuplicateCategoryName() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenThrow(new ConflictException("Category name already exists for this vendor"));

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Category name already exists for this vendor"));
    }

    @Test
    void shouldReturn200OnGetCategories() throws Exception {
        MenuCategoryResponse response1 = new MenuCategoryResponse(categoryId, "Snacks", 1, Instant.now());
        MenuCategoryResponse response2 = new MenuCategoryResponse(UUID.randomUUID(), "Drinks", 2, Instant.now());

        when(menuCategoryService.getCategories()).thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Snacks"))
                .andExpect(jsonPath("$.data[1].name").value("Drinks"));
    }

    @Test
    void shouldReturn200OnGetCategoriesEmptyList() throws Exception {
        when(menuCategoryService.getCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturn200OnGetCategoryById() throws Exception {
        MenuCategoryResponse response = new MenuCategoryResponse(categoryId, "Snacks", 1, Instant.now());

        when(menuCategoryService.getCategoryById(categoryId)).thenReturn(response);

        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.name").value("Snacks"));
    }

    @Test
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(menuCategoryService.getCategoryById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/api/menu/categories/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void shouldReturn200OnSuccessfulCategoryUpdate() throws Exception {
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);
        MenuCategoryResponse response = new MenuCategoryResponse(categoryId, "Fresh Snacks", 2, Instant.now());

        when(menuCategoryService.updateCategory(any(UUID.class), any(UpdateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Fresh Snacks"))
                .andExpect(jsonPath("$.data.displayOrder").value(2));
    }

    @Test
    void shouldReturn400OnUpdateValidationFailure() throws Exception {
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("", null);

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn204OnSuccessfulCategoryDeletion() throws Exception {
        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(menuCategoryService).deleteCategory(categoryId);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCategory() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Category not found"))
                .when(menuCategoryService).deleteCategory(nonExistentId);

        mockMvc.perform(delete("/api/menu/categories/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void shouldNotAcceptVendorIdFromRequestParameters() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);
        MenuCategoryResponse response = new MenuCategoryResponse(categoryId, "Snacks", 1, Instant.now());

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("vendorId", UUID.randomUUID().toString()))
                .andExpect(status().isCreated());

        verify(menuCategoryService).createCategory(any(CreateMenuCategoryRequest.class));
    }

    @Test
    void shouldUseAuthenticatedVendorContext() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);
        MenuCategoryResponse response = new MenuCategoryResponse(categoryId, "Snacks", 1, Instant.now());

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(categoryId.toString()));

        verify(menuCategoryService).createCategory(any(CreateMenuCategoryRequest.class));
    }

    @Test
    void shouldReturn403WhenNonVendorTriesToCreate() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenThrow(new ForbiddenException("Only vendors can manage menu categories"));

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only vendors can manage menu categories"));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(menuCategoryService.createCategory(any(CreateMenuCategoryRequest.class)))
                .thenThrow(new UnauthorizedException("Not authenticated"));

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Not authenticated"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCategory() throws Exception {
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);
        UUID nonExistentId = UUID.randomUUID();

        when(menuCategoryService.updateCategory(any(UUID.class), any(UpdateMenuCategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/api/menu/categories/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }
}
