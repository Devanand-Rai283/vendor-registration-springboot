package com.streetvendor.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.dto.request.CreateMenuCategoryRequest;
import com.streetvendor.menu.dto.request.UpdateMenuCategoryRequest;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.service.impl.MenuCategoryServiceImpl;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
class MenuCategorySecurityTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VendorRepository vendorRepository;

    @MockitoBean
    private MenuCategoryRepository menuCategoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User vendorUser;
    private User customerUser;
    private User adminUser;
    private Vendor vendor;
    private UUID categoryId;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();

        vendorUser = new User(UUID.randomUUID(), "vendor@example.com",
                passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com",
                passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        adminUser = new User(UUID.randomUUID(), "admin@example.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);

        userRepository.save(vendorUser);
        userRepository.save(customerUser);
        userRepository.save(adminUser);

        vendor = new Vendor(vendorUser.getId(), vendorUser, "Test Business");
        vendor.setStatus(VendorStatus.APPROVED);
        categoryId = UUID.randomUUID();

        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
    }

    private String generateToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private void stubCategoryOwnedByVendor() {
        MenuCategory category = new MenuCategory(categoryId, vendor, "Snacks", 1);
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId()))
                .thenReturn(Optional.of(category));
        when(menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendor.getId()))
                .thenReturn(java.util.List.of(category));
    }

    // ── Unauthenticated ──────────────────────────────────────────────

    @Test
    void shouldReturn401WhenGettingCategoriesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenGettingCategoryByIdWithoutToken() throws Exception {
        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenCreatingCategoryWithoutToken() throws Exception {
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUpdatingCategoryWithoutToken() throws Exception {
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Updated", 2);

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenDeletingCategoryWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── CUSTOMER role ────────────────────────────────────────────────

    @Test
    void shouldReturn403WhenCustomerGetsCategories() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCustomerGetsCategoryById() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCustomerCreatesCategory() throws Exception {
        String token = generateToken(customerUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCustomerUpdatesCategory() throws Exception {
        String token = generateToken(customerUser);
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Updated", 2);

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenCustomerDeletesCategory() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ── ADMIN role ───────────────────────────────────────────────────

    @Test
    void shouldReturn403WhenAdminGetsCategories() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminGetsCategoryById() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminCreatesCategory() throws Exception {
        String token = generateToken(adminUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminUpdatesCategory() throws Exception {
        String token = generateToken(adminUser);
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Updated", 2);

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminDeletesCategory() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ── VENDOR role (allowed) ────────────────────────────────────────

    @Test
    void shouldReturn200WhenVendorGetsCategories() throws Exception {
        String token = generateToken(vendorUser);
        stubCategoryOwnedByVendor();

        mockMvc.perform(get("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200WhenVendorGetsCategoryById() throws Exception {
        String token = generateToken(vendorUser);
        stubCategoryOwnedByVendor();

        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn201WhenVendorCreatesCategory() throws Exception {
        String token = generateToken(vendorUser);
        CreateMenuCategoryRequest request = new CreateMenuCategoryRequest("Snacks", 1);

        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendor.getId(), "Snacks"))
                .thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/menu/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn200WhenVendorUpdatesCategory() throws Exception {
        String token = generateToken(vendorUser);
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Fresh Snacks", 2);
        stubCategoryOwnedByVendor();

        when(menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(vendor.getId(), "Fresh Snacks"))
                .thenReturn(false);
        when(menuCategoryRepository.save(any(MenuCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn204WhenVendorDeletesCategory() throws Exception {
        String token = generateToken(vendorUser);
        stubCategoryOwnedByVendor();

        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ── Ownership: cross-vendor access ───────────────────────────────

    @Test
    void shouldReturn404WhenVendorAccessesOtherVendorCategory() throws Exception {
        String token = generateToken(vendorUser);
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenVendorUpdatesOtherVendorCategory() throws Exception {
        String token = generateToken(vendorUser);
        UpdateMenuCategoryRequest request = new UpdateMenuCategoryRequest("Hacked", 99);
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenVendorDeletesOtherVendorCategory() throws Exception {
        String token = generateToken(vendorUser);
        when(menuCategoryRepository.findByIdAndVendorId(categoryId, vendor.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/menu/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
