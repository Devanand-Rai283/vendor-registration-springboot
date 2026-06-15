package com.streetvendor.menu;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class MenuItemIntegrationTest extends AbstractSecurityTest {

    @Autowired private UserRepository userRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private MenuCategoryRepository menuCategoryRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User vendorUser;
    private User otherVendorUser;
    private Vendor vendor;
    private Vendor otherVendor;
    private MenuCategory category;
    private MenuCategory otherCategory;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        vendorUser = createUser("vendor-items@test.com", Role.VENDOR);
        otherVendorUser = createUser("other-vendor-items@test.com", Role.VENDOR);
        vendor = createVendor(vendorUser, "Vendor", VendorStatus.APPROVED);
        otherVendor = createVendor(otherVendorUser, "Other Vendor", VendorStatus.APPROVED);
        category = menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), vendor, "Snacks", 1));
        otherCategory = menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), otherVendor, "Meals", 1));
    }

    private User createUser(String email, Role role) {
        return userRepository.save(new User(UUID.randomUUID(), email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE));
    }

    private Vendor createVendor(User user, String businessName, VendorStatus status) {
        Vendor v = new Vendor(UUID.randomUUID(), user, businessName);
        v.setStatus(status);
        return vendorRepository.save(v);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private MenuItem persistItem(Vendor v, MenuCategory c, String name, BigDecimal price) {
        return menuItemRepository.save(new MenuItem(UUID.randomUUID(), c, v, name, price));
    }

    @Test
    void shouldCompleteCrudFlowEndToEnd() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuItemRequest createRequest = new CreateMenuItemRequest(category.getId(), "Samosa", "Hot", new BigDecimal("25.00"), "VEG", null, true);

        String createBody = mockMvc.perform(post("/api/menu/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Samosa"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(createBody).get("data").get("id").asText());
        assertEquals(new BigDecimal("25.00"), menuItemRepository.findById(itemId).orElseThrow().getPrice());

        mockMvc.perform(get("/api/menu/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        UpdateMenuItemRequest updateRequest = new UpdateMenuItemRequest(category.getId(), "Paneer Roll", "Spicy", new BigDecimal("90.00"), "VEG", null, false);
        mockMvc.perform(put("/api/menu/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Paneer Roll"))
                .andExpect(jsonPath("$.data.price").value(90.00))
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(delete("/api/menu/items/" + itemId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertTrue(menuItemRepository.findById(itemId).isEmpty());
    }

    @Test
    void shouldUpdateAvailabilityImmediately() throws Exception {
        String token = tokenFor(vendorUser);
        MenuItem item = persistItem(vendor, category, "Tea", new BigDecimal("10.00"));

        mockMvc.perform(patch("/api/menu/items/" + item.getId() + "/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        assertFalse(menuItemRepository.findById(item.getId()).orElseThrow().isAvailable());
    }

    @Test
    void shouldRejectNegativePrice() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuItemRequest request = new CreateMenuItemRequest(category.getId(), "Invalid", null, new BigDecimal("-1.00"), null, null, true);

        mockMvc.perform(post("/api/menu/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreventCrossVendorCategoryUseAndItemAccess() throws Exception {
        String token = tokenFor(vendorUser);
        CreateMenuItemRequest request = new CreateMenuItemRequest(otherCategory.getId(), "Other", null, BigDecimal.ONE, null, null, true);

        mockMvc.perform(post("/api/menu/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        MenuItem otherItem = persistItem(otherVendor, otherCategory, "Other Item", BigDecimal.TEN);
        mockMvc.perform(get("/api/menu/items/" + otherItem.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPreserveExistingItemPriceWhenAvailabilityChangesForOrderSnapshotCompatibility() throws Exception {
        String token = tokenFor(vendorUser);
        MenuItem item = persistItem(vendor, category, "Coffee", new BigDecimal("30.00"));

        mockMvc.perform(patch("/api/menu/items/" + item.getId() + "/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMenuItemAvailabilityRequest(false))))
                .andExpect(status().isOk());

        assertEquals(new BigDecimal("30.00"), menuItemRepository.findById(item.getId()).orElseThrow().getPrice());
    }
}
