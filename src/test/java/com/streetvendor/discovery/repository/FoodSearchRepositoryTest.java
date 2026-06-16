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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class FoodSearchRepositoryTest {

    @Autowired
    private FoodSearchRepository foodSearchRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User approvedUser;
    private User chineseUser;
    private User pendingUser;
    private User rejectedUser;
    private Vendor approvedVendor;
    private Vendor chineseVendor;
    private Vendor pendingVendor;
    private Vendor rejectedVendor;
    private MenuCategory category;
    private MenuCategory chineseCategory;

    @BeforeEach
    void setUp() {
        approvedUser = userRepository.save(
                new User(UUID.randomUUID(), "approved@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        chineseUser = userRepository.save(
                new User(UUID.randomUUID(), "chinese@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        pendingUser = userRepository.save(
                new User(UUID.randomUUID(), "pending@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        rejectedUser = userRepository.save(
                new User(UUID.randomUUID(), "rejected@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));

        approvedVendor = new Vendor(UUID.randomUUID(), approvedUser, "Approved Kitchen");
        approvedVendor.setFoodType("Indian");
        approvedVendor.setStatus(VendorStatus.APPROVED);
        approvedVendor.setAverageRating(new BigDecimal("4.5"));
        approvedVendor = vendorRepository.save(approvedVendor);

        chineseVendor = new Vendor(UUID.randomUUID(), chineseUser, "Chinese Wok");
        chineseVendor.setFoodType("Chinese");
        chineseVendor.setStatus(VendorStatus.APPROVED);
        chineseVendor.setAverageRating(new BigDecimal("4.2"));
        chineseVendor = vendorRepository.save(chineseVendor);

        pendingVendor = new Vendor(UUID.randomUUID(), pendingUser, "Pending Kitchen");
        pendingVendor.setFoodType("Chinese");
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);
        pendingVendor = vendorRepository.save(pendingVendor);

        rejectedVendor = new Vendor(UUID.randomUUID(), rejectedUser, "Rejected Kitchen");
        rejectedVendor.setFoodType("Italian");
        rejectedVendor.setStatus(VendorStatus.REJECTED);
        rejectedVendor = vendorRepository.save(rejectedVendor);

        category = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), approvedVendor, "Main Course", 0));
        chineseCategory = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), chineseVendor, "Main Course", 0));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), pendingVendor, "Main Course", 0));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), rejectedVendor, "Main Course", 0));

        foodSearchRepository.save(createMenuItem("Chicken Curry", "Spicy chicken", new BigDecimal("12.99"),
                "non-veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem("Paneer Masala", "Cottage cheese curry", new BigDecimal("10.99"),
                "veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem("Chicken Biryani", "Fragrant rice with chicken",
                new BigDecimal("14.99"), "non-veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem("Veg Biryani", "Fragrant rice with vegetables",
                new BigDecimal("11.99"), "veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem("Chinese Noodles", "Stir-fried noodles",
                new BigDecimal("8.99"), "veg", true, chineseVendor));
        foodSearchRepository.save(createMenuItem("Unavailable Item", "Should not appear",
                new BigDecimal("9.99"), "veg", false, approvedVendor));
        foodSearchRepository.save(createMenuItem("Pending Chicken", "From pending vendor",
                new BigDecimal("8.99"), "non-veg", true, pendingVendor));
        foodSearchRepository.save(createMenuItem("Rejected Pasta", "From rejected vendor",
                new BigDecimal("15.99"), "veg", true, rejectedVendor));

        foodSearchRepository.flush();
    }

    private MenuItem createMenuItem(String name, String description, BigDecimal price,
                                    String dietaryTag, boolean isAvailable, Vendor vendor) {
        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, name, price);
        item.setDescription(description);
        item.setDietaryTag(dietaryTag);
        item.setAvailable(isAvailable);
        return item;
    }

    @Test
    void shouldReturnMatchingItemsWhenKeywordMatches() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "chicken", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(FoodSearchResponseDto::itemName)
                .containsExactlyInAnyOrder("Chicken Curry", "Chicken Biryani");
    }

    @Test
    void shouldReturnMatchingItemsWhenKeywordIsCaseInsensitive() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "CHICKEN", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void shouldReturnResultsWhenFoodTypeMatches() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "noodles", "Chinese", null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Chinese Noodles");
    }

    @Test
    void shouldReturnEmptyWhenFoodTypeDoesNotMatch() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "noodles", "Indian", null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldReturnResultsWhenDietaryTagMatches() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "biryani", null, "veg", VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Veg Biryani");
    }

    @Test
    void shouldReturnResultsWhenAllFiltersCombined() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "curry", "Indian", "non-veg", VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Chicken Curry");
    }

    @Test
    void shouldExcludeItemsWhenVendorIsPending() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "pending", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldExcludeItemsWhenVendorIsRejected() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "rejected", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldExcludeUnavailableItemsWhenSearchingFoods() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "unavailable", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldReturnAvailableItemsWhenKeywordMatches() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Paneer", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Paneer Masala");
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "nonexistent", null, null, VendorStatus.APPROVED, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getNumber()).isZero();
    }

    @Test
    void shouldReturnAllAvailableItemsFromApprovedVendorsWhenKeywordIsEmpty() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void shouldReturnEmptyWhenDietaryTagDoesNotMatch() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "chicken", null, "veg", VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldMapAllProjectionFieldsWhenResultFound() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Chicken Curry", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        FoodSearchResponseDto dto = result.getContent().get(0);
        assertThat(dto.menuItemId()).isNotNull();
        assertThat(dto.itemName()).isEqualTo("Chicken Curry");
        assertThat(dto.description()).isEqualTo("Spicy chicken");
        assertThat(dto.price()).isEqualByComparingTo(new BigDecimal("12.99"));
        assertThat(dto.dietaryTag()).isEqualTo("non-veg");
        assertThat(dto.vendorId()).isEqualTo(approvedVendor.getId());
        assertThat(dto.vendorName()).isEqualTo("Approved Kitchen");
        assertThat(dto.foodType()).isEqualTo("Indian");
        assertThat(dto.averageRating()).isEqualByComparingTo(new BigDecimal("4.5"));
    }

    @Test
    void shouldReturnCorrectPageWithPagination() {
        Pageable firstPage = PageRequest.of(0, 2);
        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "biryani", null, null, VendorStatus.APPROVED, firstPage);

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getNumber()).isZero();
        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getTotalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnMultiplePagesWhenDatasetExceedsPageSize() {
        for (int i = 0; i < 6; i++) {
            foodSearchRepository.save(createMenuItem(
                    "Extra Item " + i, "Test item " + i, new BigDecimal("5.00"),
                    "veg", true, approvedVendor));
        }
        foodSearchRepository.flush();

        Pageable firstPage = PageRequest.of(0, 5);
        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, firstPage);

        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(6);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        Page<FoodSearchResponseDto> page2 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, PageRequest.of(1, 5));

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getNumber()).isEqualTo(1);
    }

    @Test
    void shouldOnlyReturnAvailableItemsFromApprovedVendors() {
        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "chicken", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        List<String> names = result.getContent().stream().map(FoodSearchResponseDto::itemName).toList();
        assertThat(names).containsExactlyInAnyOrder("Chicken Curry", "Chicken Biryani");
        assertThat(names).doesNotContain("Unavailable Item", "Pending Chicken", "Rejected Pasta");
    }

    @Test
    void shouldPreservePaginationWhenNonApprovedVendorsExist() {
        for (int i = 0; i < 3; i++) {
            foodSearchRepository.save(createMenuItem(
                    "Extra Approved " + i, "From approved", new BigDecimal("3.00"),
                    "veg", true, approvedVendor));
        }
        foodSearchRepository.save(createMenuItem(
                "Extra Pending", "From pending", new BigDecimal("5.00"),
                "veg", true, pendingVendor));
        foodSearchRepository.save(createMenuItem(
                "Extra Rejected", "From rejected", new BigDecimal("6.00"),
                "veg", true, rejectedVendor));
        foodSearchRepository.flush();

        Pageable firstPage = PageRequest.of(0, 2);
        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, firstPage);

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        Page<FoodSearchResponseDto> page2 = foodSearchRepository.searchFoods(
                "Extra", null, null, VendorStatus.APPROVED, PageRequest.of(1, 2));

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getNumber()).isEqualTo(1);
        assertThat(page2.getContent()).extracting(FoodSearchResponseDto::itemName)
                .allMatch(name -> name.startsWith("Extra Approved"));
    }

    @Test
    void shouldReturnOnlyAvailableItemsInMixedAvailabilityDataset() {
        foodSearchRepository.save(createMenuItem(
                "Soup Available", "Available soup", new BigDecimal("5.00"),
                "non-veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem(
                "Soup Unavailable", "Unavailable soup", new BigDecimal("5.00"),
                "non-veg", false, approvedVendor));
        foodSearchRepository.flush();

        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Soup", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Soup Available");
    }

    @Test
    void shouldCombineAvailabilityAndVendorApprovalFiltering() {
        foodSearchRepository.save(createMenuItem(
                "Snack Available", "Available approved", new BigDecimal("4.00"),
                "veg", true, approvedVendor));
        foodSearchRepository.save(createMenuItem(
                "Snack Unavailable", "Unavailable approved", new BigDecimal("4.00"),
                "veg", false, approvedVendor));
        foodSearchRepository.save(createMenuItem(
                "Snack Pending", "Pending available", new BigDecimal("4.00"),
                "veg", true, pendingVendor));
        foodSearchRepository.save(createMenuItem(
                "Snack Rejected", "Rejected available", new BigDecimal("4.00"),
                "veg", true, rejectedVendor));
        foodSearchRepository.flush();

        Page<FoodSearchResponseDto> result = foodSearchRepository.searchFoods(
                "Snack", null, null, VendorStatus.APPROVED, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("Snack Available");
    }

    @Test
    void shouldPreservePaginationWithAvailabilityFiltering() {
        for (int i = 0; i < 4; i++) {
            foodSearchRepository.save(createMenuItem(
                    "Special " + i, "Available", new BigDecimal("5.00"),
                    "veg", true, approvedVendor));
        }
        for (int i = 0; i < 6; i++) {
            foodSearchRepository.save(createMenuItem(
                    "Special Unavailable " + i, "Unavailable", new BigDecimal("5.00"),
                    "veg", false, approvedVendor));
        }
        foodSearchRepository.flush();

        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "Special", null, null, VendorStatus.APPROVED, PageRequest.of(0, 3));

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(4);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        Page<FoodSearchResponseDto> page2 = foodSearchRepository.searchFoods(
                "Special", null, null, VendorStatus.APPROVED, PageRequest.of(1, 3));

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getNumber()).isEqualTo(1);
        assertThat(page2.getContent()).extracting(FoodSearchResponseDto::itemName)
                .allMatch(name -> name.startsWith("Special "));
    }

    @Test
    void shouldHaveConsistentCountQueryAcrossPages() {
        for (int i = 0; i < 7; i++) {
            foodSearchRepository.save(createMenuItem(
                    "Dish " + i, "Test dish", new BigDecimal("6.00"),
                    "veg", true, approvedVendor));
        }
        foodSearchRepository.flush();

        Page<FoodSearchResponseDto> page0 = foodSearchRepository.searchFoods(
                "Dish", null, null, VendorStatus.APPROVED, PageRequest.of(0, 3));

        assertThat(page0.getContent()).hasSize(3);
        assertThat(page0.getTotalElements()).isEqualTo(7);
        assertThat(page0.getTotalPages()).isEqualTo(3);

        Page<FoodSearchResponseDto> page1 = foodSearchRepository.searchFoods(
                "Dish", null, null, VendorStatus.APPROVED, PageRequest.of(1, 3));

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getNumber()).isEqualTo(1);

        Page<FoodSearchResponseDto> page2 = foodSearchRepository.searchFoods(
                "Dish", null, null, VendorStatus.APPROVED, PageRequest.of(2, 3));

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getNumber()).isEqualTo(2);

        long manualCount = page0.getTotalElements();
        assertThat(manualCount).isEqualTo(page1.getTotalElements());
        assertThat(manualCount).isEqualTo(page2.getTotalElements());
    }
}
