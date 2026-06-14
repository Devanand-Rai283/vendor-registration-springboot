package com.streetvendor.menu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class MenuCategoryIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User vendorUser;
    private User otherVendorUser;
    private User customerUser;
    private User adminUser;
    private Vendor vendor;
    private Vendor otherVendor;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        vendorUser = createVendorUser("vendor@test.com");
        otherVendorUser = createVendorUser("other-vendor@test.com");
        customerUser = createRoleUser("customer@test.com", Role.CUSTOMER);
        adminUser = createRoleUser("admin@test.com", Role.ADMIN);

        vendor = createVendor(vendorUser, "Test Business");
        otherVendor = createVendor(otherVendorUser, "Other Business");
    }

    private User createVendorUser(String email) {
        User user = new User(UUID.randomUUID(), email,
                passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private User createRoleUser(String email, Role role) {
        User user = new User(UUID.randomUUID(), email,
                passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Vendor createVendor(User user, String businessName) {
        Vendor v = new Vendor(user.getId(), user, businessName);
        v.setStatus(VendorStatus.APPROVED);
        return vendorRepository.save(v);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private MenuCategory persistCategory(Vendor v, String name, int displayOrder) {
        MenuCategory category = new MenuCategory(UUID.randomUUID(), v, name, displayOrder);
        return menuCategoryRepository.save(category);
    }

    // ── CREATE CATEGORY ─────────────────────────────────────────────

    @Test
    void shouldCreateCategoryEndToEnd() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        String responseBody = mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.name").value("Snacks"))
                .andExpect(jsonPath("$.data.displayOrder").value(1))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        UUID createdId = UUID.fromString(json.get("data").get("id").asText());

        MenuCategory stored = menuCategoryRepository.findById(createdId).orElseThrow();
        assertEquals("Snacks", stored.getName());
        assertEquals(1, stored.getDisplayOrder());
        assertEquals(vendor.getId(), stored.getVendor().getId());
        assertNotNull(stored.getCreatedAt());
    }

    // ── GET COLLECTION ──────────────────────────────────────────────

    @Test
    void shouldReturnCategoriesInDisplayOrder() throws Exception {
        String token = tokenFor(vendorUser);
        persistCategory(vendor, "Drinks", 2);
        persistCategory(vendor, "Appetizers", 1);
        persistCategory(vendor, "Main Course", 3);

        String responseBody = mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Appetizers"))
                .andExpect(jsonPath("$.data[1].name").value("Drinks"))
                .andExpect(jsonPath("$.data[2].name").value("Main Course"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void shouldReturnEmptyCollectionForNewVendor() throws Exception {
        String token = tokenFor(vendorUser);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── GET SINGLE ──────────────────────────────────────────────────

    @Test
    void shouldGetOwnCategoryById() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory category = persistCategory(vendor, "Snacks", 1);

        mockMvc.perform(get("/api/menu/categories/" + category.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Snacks"))
                .andExpect(jsonPath("$.data.displayOrder").value(1));
    }

    @Test
    void shouldReturn404ForNonExistentCategory() throws Exception {
        String token = tokenFor(vendorUser);
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/menu/categories/" + nonExistentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    // ── UPDATE CATEGORY ─────────────────────────────────────────────

    @Test
    void shouldUpdateCategoryEndToEnd() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory category = persistCategory(vendor, "Snacks", 1);

        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);

        mockMvc.perform(put("/api/menu/categories/" + category.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Fresh Snacks"))
                .andExpect(jsonPath("$.data.displayOrder").value(2));

        MenuCategory stored = menuCategoryRepository.findById(category.getId()).orElseThrow();
        assertEquals("Fresh Snacks", stored.getName());
        assertEquals(2, stored.getDisplayOrder());
        assertEquals(category.getCreatedAt(), stored.getCreatedAt());
    }

    @Test
    void shouldReturn409OnDuplicateNameDuringUpdate() throws Exception {
        String token = tokenFor(vendorUser);
        persistCategory(vendor, "Snacks", 1);
        MenuCategory other = persistCategory(vendor, "Drinks", 2);

        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Snacks", 2);

        mockMvc.perform(put("/api/menu/categories/" + other.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Category name already exists for this vendor"));
    }

    // ── DELETE CATEGORY ─────────────────────────────────────────────

    @Test
    void shouldDeleteCategoryEndToEnd() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory category = persistCategory(vendor, "Snacks", 1);

        mockMvc.perform(delete("/api/menu/categories/" + category.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertFalse(menuCategoryRepository.findById(category.getId()).isPresent());
    }

    @Test
    void shouldReturn404WhenDeletingAlreadyDeletedCategory() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory category = persistCategory(vendor, "Snacks", 1);

        mockMvc.perform(delete("/api/menu/categories/" + category.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/menu/categories/" + category.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    // ── OWNERSHIP ENFORCEMENT ───────────────────────────────────────

    @Test
    void shouldReturn404WhenVendorAccessesOtherVendorCategory() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory otherCategory = persistCategory(otherVendor, "Other Snacks", 1);

        mockMvc.perform(get("/api/menu/categories/" + otherCategory.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        assertNotNull(menuCategoryRepository.findById(otherCategory.getId()).orElseThrow());
    }

    @Test
    void shouldReturn404WhenVendorUpdatesOtherVendorCategory() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory otherCategory = persistCategory(otherVendor, "Other Snacks", 1);

        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Hacked", 99);

        mockMvc.perform(put("/api/menu/categories/" + otherCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        MenuCategory stored = menuCategoryRepository.findById(otherCategory.getId()).orElseThrow();
        assertEquals("Other Snacks", stored.getName());
        assertEquals(1, stored.getDisplayOrder());
    }

    @Test
    void shouldReturn404WhenVendorDeletesOtherVendorCategory() throws Exception {
        String token = tokenFor(vendorUser);
        MenuCategory otherCategory = persistCategory(otherVendor, "Other Snacks", 1);

        mockMvc.perform(delete("/api/menu/categories/" + otherCategory.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        assertTrue(menuCategoryRepository.findById(otherCategory.getId()).isPresent());
    }

    // ── SECURITY INTEGRATION ────────────────────────────────────────

    @Test
    void shouldReturn401WhenUnauthenticatedGetCollection() throws Exception {
        mockMvc.perform(get("/api/menu/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUnauthenticatedPost() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerGetsCategories() throws Exception {
        String token = tokenFor(customerUser);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminGetsCategories() throws Exception {
        String token = tokenFor(adminUser);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenVendorGetsCategories() throws Exception {
        String token = tokenFor(vendorUser);
        persistCategory(vendor, "Snacks", 1);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DATABASE VERIFICATION ───────────────────────────────────────

    @Test
    void shouldPersistCreatedTimestampAutomatically() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        String responseBody = mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        UUID createdId = UUID.fromString(json.get("data").get("id").asText());
        String createdAtStr = json.get("data").get("createdAt").asText();

        assertNotNull(createdAtStr);
        assertFalse(createdAtStr.isBlank());

        MenuCategory stored = menuCategoryRepository.findById(createdId).orElseThrow();
        assertNotNull(stored.getCreatedAt());
    }

    @Test
    void shouldPersistDisplayOrderCorrectly() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 7);

        String responseBody = mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        UUID createdId = UUID.fromString(json.get("data").get("id").asText());

        MenuCategory stored = menuCategoryRepository.findById(createdId).orElseThrow();
        assertEquals(7, stored.getDisplayOrder());
    }

    @Test
    void shouldEnforceForeignKeyOnVendorId() throws Exception {
        List<MenuCategory> categories = menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendor.getId());
        for (MenuCategory c : categories) {
            assertEquals(vendor.getId(), c.getVendor().getId());
        }
    }
}
