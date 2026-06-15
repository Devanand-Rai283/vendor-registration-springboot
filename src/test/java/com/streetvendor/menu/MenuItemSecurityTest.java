package com.streetvendor.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.dto.request.CreateMenuItemRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemAvailabilityRequest;
import com.streetvendor.menu.dto.request.UpdateMenuItemRequest;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class MenuItemSecurityTest extends AbstractSecurityTest {

    @Autowired private UserRepository userRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private MenuCategoryRepository menuCategoryRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User vendorUser;
    private User otherVendorUser;
    private User customerUser;
    private User adminUser;
    private User pendingVendorUser;
    private Vendor vendor;
    private Vendor otherVendor;
    private Vendor pendingVendor;
    private MenuCategory category;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        vendorUser = createUser("item-vendor@test.com", Role.VENDOR);
        otherVendorUser = createUser("item-other@test.com", Role.VENDOR);
        customerUser = createUser("item-customer@test.com", Role.CUSTOMER);
        adminUser = createUser("item-admin@test.com", Role.ADMIN);
        pendingVendorUser = createUser("item-pending@test.com", Role.VENDOR);
        vendor = createVendor(vendorUser, VendorStatus.APPROVED);
        otherVendor = createVendor(otherVendorUser, VendorStatus.APPROVED);
        pendingVendor = createVendor(pendingVendorUser, VendorStatus.PENDING_REVIEW);
        category = menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), vendor, "Snacks", 1));
        MenuCategory otherCategory = menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), otherVendor, "Other", 1));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), pendingVendor, "Pending", 1));
        item = menuItemRepository.save(new MenuItem(UUID.randomUUID(), category, vendor, "Tea", BigDecimal.TEN));
        menuItemRepository.save(new MenuItem(UUID.randomUUID(), otherCategory, otherVendor, "Other Tea", BigDecimal.TEN));
    }

    private User createUser(String email, Role role) {
        return userRepository.save(new User(UUID.randomUUID(), email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE));
    }

    private Vendor createVendor(User user, VendorStatus status) {
        Vendor v = new Vendor(UUID.randomUUID(), user, user.getEmail());
        v.setStatus(status);
        return vendorRepository.save(v);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private CreateMenuItemRequest createRequest() {
        return new CreateMenuItemRequest(category.getId(), "Samosa", null, BigDecimal.ONE, null, null, true);
    }

    private UpdateMenuItemRequest updateRequest() {
        return new UpdateMenuItemRequest(category.getId(), "Tea", null, BigDecimal.TEN, null, null, true);
    }

    @Test
    void shouldReturn401ForAnonymousOnAllMenuItemEndpoints() throws Exception {
        mockMvc.perform(get("/api/menu/items")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/menu/items/" + item.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/menu/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createRequest()))).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/menu/items/" + item.getId()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest()))).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/menu/items/" + item.getId() + "/availability").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false)))).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/menu/items/" + item.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForCustomerOnAllMenuItemEndpoints() throws Exception {
        assertForbiddenForRole(customerUser);
    }

    @Test
    void shouldReturn403ForAdminOnAllMenuItemEndpoints() throws Exception {
        assertForbiddenForRole(adminUser);
    }

    private void assertForbiddenForRole(User user) throws Exception {
        String token = tokenFor(user);
        mockMvc.perform(get("/api/menu/items").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/menu/items/" + item.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/menu/items").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createRequest()))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/menu/items/" + item.getId()).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest()))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/menu/items/" + item.getId() + "/availability").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false)))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/menu/items/" + item.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForUnapprovedVendor() throws Exception {
        String token = tokenFor(pendingVendorUser);
        mockMvc.perform(get("/api/menu/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventDifferentVendorAccess() throws Exception {
        String token = tokenFor(otherVendorUser);
        mockMvc.perform(get("/api/menu/items/" + item.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/menu/items/" + item.getId() + "/availability").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false))))
                .andExpect(status().isNotFound());
    }
}
