package com.streetvendor.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class VendorMenuIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private User persistUser(String email) {
        return userRepository.save(
                new User(UUID.randomUUID(), email, "hash", Role.VENDOR, AccountStatus.ACTIVE));
    }

    private Vendor persistVendor(User user, String businessName, VendorStatus status) {
        Vendor vendor = new Vendor(UUID.randomUUID(), user, businessName);
        vendor.setStatus(status);
        vendor.setFoodType("Test Food");
        vendor.setAddress("Test Address");
        vendor.setLatitude(BigDecimal.valueOf(12.9716));
        vendor.setLongitude(BigDecimal.valueOf(77.5946));
        vendor.setAverageRating(BigDecimal.valueOf(4.5));
        return vendorRepository.save(vendor);
    }

    private MenuCategory persistCategory(Vendor vendor, String name, int displayOrder) {
        return menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), vendor, name, displayOrder));
    }

    private MenuItem persistItem(MenuCategory category, Vendor vendor, String name,
                                 BigDecimal price, boolean available) {
        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, name, price);
        item.setDescription(name + " description");
        item.setDietaryTag("VEG");
        item.setImageUrl("http://example.com/" + name);
        item.setAvailable(available);
        return menuItemRepository.save(item);
    }

    @Test
    void successfulMenuRetrieval_returnsCategoriesAndItems() throws Exception {
        User user = persistUser("vendor-success@example.com");
        Vendor vendor = persistVendor(user, "Vendor Menu Vendor", VendorStatus.APPROVED);

        MenuCategory c1 = persistCategory(vendor, "Category A", 1);
        MenuCategory c2 = persistCategory(vendor, "Category B", 2);

        persistItem(c1, vendor, "Item 1", BigDecimal.valueOf(10.00), true);
        persistItem(c2, vendor, "Item 2", BigDecimal.valueOf(12.00), true);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", vendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorName").value("Vendor Menu Vendor"))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name").value("Category A"))
                .andExpect(jsonPath("$.categories[0].items[0].name").value("Item 1"))
                .andExpect(jsonPath("$.categories[1].name").value("Category B"))
                .andExpect(jsonPath("$.categories[1].items[0].name").value("Item 2"));
    }

    @Test
    void vendorNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/vendors/{vendorId}/menu", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendingAndRejectedVendor_returns404() throws Exception {
        User pendingUser = persistUser("vendor-pending@example.com");
        Vendor pendingVendor = persistVendor(pendingUser, "Pending Vendor", VendorStatus.PENDING_REVIEW);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", pendingVendor.getId()))
                .andExpect(status().isNotFound());

        User rejectedUser = persistUser("vendor-rejected@example.com");
        Vendor rejectedVendor = persistVendor(rejectedUser, "Rejected Vendor", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", rejectedVendor.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unavailableItemsAreExcluded() throws Exception {
        User user = persistUser("vendor-unavailable@example.com");
        Vendor vendor = persistVendor(user, "Vendor Unavailable Items", VendorStatus.APPROVED);

        MenuCategory category = persistCategory(vendor, "Category A", 1);

        persistItem(category, vendor, "Available Item", BigDecimal.valueOf(10.00), true);
        persistItem(category, vendor, "Unavailable Item", BigDecimal.valueOf(11.00), false);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", vendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].items", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].items[0].name").value("Available Item"))
                .andExpect(jsonPath("$.categories[0].items[0].available").value(true));
    }

    @Test
    void emptyCategoriesAreExcluded() throws Exception {
        User user = persistUser("vendor-empty-cats@example.com");
        Vendor vendor = persistVendor(user, "Vendor Empty Categories", VendorStatus.APPROVED);

        MenuCategory populated = persistCategory(vendor, "Category A", 1);
        MenuCategory empty = persistCategory(vendor, "Category B", 2);

        persistItem(populated, vendor, "Available Item", BigDecimal.valueOf(10.00), true);
        persistItem(empty, vendor, "Unavailable Item", BigDecimal.valueOf(11.00), false);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", vendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].name").value("Category A"));
    }

    @Test
    void categoryOrderingPreservedByDisplayOrder() throws Exception {
        User user = persistUser("vendor-ordering@example.com");
        Vendor vendor = persistVendor(user, "Vendor Ordering", VendorStatus.APPROVED);

        MenuCategory c1 = persistCategory(vendor, "Category 1", 1);
        MenuCategory c2 = persistCategory(vendor, "Category 2", 2);
        MenuCategory c3 = persistCategory(vendor, "Category 3", 3);

        persistItem(c1, vendor, "Item 1", BigDecimal.valueOf(10.00), true);
        persistItem(c2, vendor, "Item 2", BigDecimal.valueOf(11.00), true);
        persistItem(c3, vendor, "Item 3", BigDecimal.valueOf(12.00), true);

        mockMvc.perform(get("/api/vendors/{vendorId}/menu", vendor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(3)))
                .andExpect(jsonPath("$.categories[0].displayOrder").value(1))
                .andExpect(jsonPath("$.categories[1].displayOrder").value(2))
                .andExpect(jsonPath("$.categories[2].displayOrder").value(3));
    }
}
