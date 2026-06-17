package com.streetvendor.discovery.repository;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class FoodSearchApprovalFilteringTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private FoodSearchRepository foodSearchRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Vendor approvedVendor;
    private MenuItem approvedItem;
    private MenuItem pendingItem;
    private MenuItem rejectedItem;

    @BeforeEach
    void setUp() {
        User approvedUser = userRepository.save(
                new User(UUID.randomUUID(), "approved@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        User pendingUser = userRepository.save(
                new User(UUID.randomUUID(), "pending@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        User rejectedUser = userRepository.save(
                new User(UUID.randomUUID(), "rejected@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));

        approvedVendor = new Vendor(UUID.randomUUID(), approvedUser, "Approved Kitchen");
        approvedVendor.setFoodType("Indian");
        approvedVendor.setStatus(VendorStatus.APPROVED);
        approvedVendor.setAverageRating(new BigDecimal("4.5"));
        approvedVendor = vendorRepository.save(approvedVendor);

        Vendor pendingVendor = new Vendor(UUID.randomUUID(), pendingUser, "Pending Kitchen");
        pendingVendor.setFoodType("Chinese");
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);
        pendingVendor = vendorRepository.save(pendingVendor);

        Vendor rejectedVendor = new Vendor(UUID.randomUUID(), rejectedUser, "Rejected Kitchen");
        rejectedVendor.setFoodType("Italian");
        rejectedVendor.setStatus(VendorStatus.REJECTED);
        rejectedVendor = vendorRepository.save(rejectedVendor);

        MenuCategory approvedCategory = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), approvedVendor, "Main Course", 0));
        MenuCategory pendingCategory = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), pendingVendor, "Main Course", 0));
        MenuCategory rejectedCategory = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), rejectedVendor, "Main Course", 0));

        approvedItem = foodSearchRepository.save(
                createMenuItem("Chicken Curry", "Spicy chicken", new BigDecimal("12.99"),
                        "non-veg", true, approvedVendor, approvedCategory));
        foodSearchRepository.save(
                createMenuItem("Unavailable Soup", "Not available", new BigDecimal("5.99"),
                        "veg", false, approvedVendor, approvedCategory));
        pendingItem = foodSearchRepository.save(
                createMenuItem("Pending Noodles", "From pending vendor", new BigDecimal("8.99"),
                        "veg", true, pendingVendor, pendingCategory));
        rejectedItem = foodSearchRepository.save(
                createMenuItem("Rejected Pasta", "From rejected vendor", new BigDecimal("15.99"),
                        "veg", true, rejectedVendor, rejectedCategory));
        foodSearchRepository.flush();
    }

    private MenuItem createMenuItem(String name, String description, BigDecimal price,
                                    String dietaryTag, boolean isAvailable, Vendor vendor, MenuCategory category) {
        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, name, price);
        item.setDescription(description);
        item.setDietaryTag(dietaryTag);
        item.setAvailable(isAvailable);
        return item;
    }

    @Test
    void shouldReturnApprovedItemsWhenKeywordMatches() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Chicken", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent())
                .extracting(FoodSearchResponseDto::itemName)
                .containsExactly(approvedItem.getName());
    }

    @Test
    void shouldExcludeItemsWhenVendorIsPending() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Noodles", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent())
                .extracting(FoodSearchResponseDto::itemName)
                .doesNotContain(pendingItem.getName());
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldExcludeItemsWhenVendorIsRejected() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Pasta", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent())
                .extracting(FoodSearchResponseDto::itemName)
                .doesNotContain(rejectedItem.getName());
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldReturnOnlyApprovedInMixedDataset() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Chicken", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).vendorName()).isEqualTo(approvedVendor.getBusinessName());
    }

    @Test
    void shouldPreservePaginationWithApprovalFiltering() {
        MenuCategory sides = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), approvedVendor, "Sides", 1));
        for (int i = 0; i < 6; i++) {
            foodSearchRepository.save(
                    createMenuItem("Extra Dish " + i, "Test item", new BigDecimal("5.00"),
                            "veg", true, approvedVendor, sides));
        }
        foodSearchRepository.flush();

        Pageable firstPage = PageRequest.of(0, 3);
        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, firstPage);

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(6);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        Page<FoodSearchResponseDto> page2 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, PageRequest.of(1, 3));

        assertThat(page2.getContent()).hasSize(3);
        assertThat(page2.getNumber()).isEqualTo(1);
    }
}
