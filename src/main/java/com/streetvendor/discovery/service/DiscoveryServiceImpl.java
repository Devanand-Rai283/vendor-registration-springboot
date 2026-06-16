package com.streetvendor.discovery.service;

import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.discovery.dto.BoundingBox;
import com.streetvendor.discovery.dto.MenuCategoryResponseDto;
import com.streetvendor.discovery.dto.MenuItemResponseDto;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import com.streetvendor.discovery.dto.VendorSummaryResponse;
import com.streetvendor.discovery.util.BoundingBoxCalculator;
import com.streetvendor.discovery.util.DistanceCalculator;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link DiscoveryService} orchestrating the nearby vendor
 * search use case.
 * <p>
 * The discovery algorithm follows a two-phase approach:
 * <ol>
 * <li><strong>Bounding-box pre-filter:</strong> A rectangular region is
 * computed around the search center using
 * {@link BoundingBoxCalculator}. This cheap SQL {@code BETWEEN} query
 * eliminates vendors clearly outside the search area using indexed
 * {@code latitude}/{@code longitude} columns.</li>
 * <li><strong>Haversine refinement:</strong> Each candidate vendor's exact
 * great-circle distance is computed using
 * {@link DistanceCalculator}. Candidates whose distance exceeds the
 * requested radius are discarded. This step is necessary because the
 * bounding box is a rectangle on a flat projection, not a true circle
 * on the Earth's curved surface — vendors near the corners of the box
 * may be farther from the center than the specified radius.</li>
 * </ol>
 * Results are sorted by distance ascending and paginated in memory before
 * being returned as {@link VendorSummaryResponse} DTOs — JPA entities are
 * never exposed at the API boundary.
 * </p>
 */
@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private final VendorRepository vendorRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public DiscoveryServiceImpl(
            VendorRepository vendorRepository,
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository) {
        this.vendorRepository = vendorRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public NearbyVendorResponse findNearbyVendors(double latitude, double longitude, double radiusKm, int page,
            int size) {
        BoundingBox box = BoundingBoxCalculator.calculate(latitude, longitude, radiusKm);

        Page<Vendor> vendorPage = vendorRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                VendorStatus.APPROVED,
                box.minLatitude(),
                box.maxLatitude(),
                box.minLongitude(),
                box.maxLongitude(),
                Pageable.unpaged());

        List<VendorSummaryResponse> allWithinRadius = vendorPage.getContent().stream()
                .map(vendor -> {
                    double distance = DistanceCalculator.calculateDistanceKm(
                            latitude, longitude,
                            vendor.getLatitude().doubleValue(),
                            vendor.getLongitude().doubleValue());
                    return toVendorSummary(vendor, distance);
                })
                .filter(summary -> summary.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(VendorSummaryResponse::distanceKm))
                .toList();

        int totalElements = allWithinRadius.size();
        int totalPages = calculateTotalPages(totalElements, size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<VendorSummaryResponse> pageContent = fromIndex < totalElements
                ? allWithinRadius.subList(fromIndex, toIndex)
                : List.of();

        return new NearbyVendorResponse(pageContent, page, size, totalElements, totalPages);
    }

    /**
     * Vendor existence validation intended to be reused by future
     * DiscoveryService use-cases (e.g., vendor menu viewing).
     * <p>
     * This validation performs a single repository lookup and throws the
     * project's standardized {@link ResourceNotFoundException} to produce an
     * HTTP 404 response via {@code GlobalExceptionHandler}.
     * </p>
     *
     * @param vendorId the requested vendor id
     * @return the persisted Vendor
     * @throws ResourceNotFoundException if the vendor does not exist
     */
    private Vendor getVendorOrThrow(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
    }

    /**
     * Vendor approval validation intended to be reused by future
     * DiscoveryService use-cases (e.g., vendor menu viewing).
     * <p>
     * This validation must not re-query the repository; it only evaluates the
     * already-loaded {@link Vendor} instance from the caller (Task 4).
     * </p>
     *
     * @param vendor the already-loaded vendor instance (must not be {@code null})
     * @throws ResourceNotFoundException if vendor is not APPROVED
     */
    private void validateVendorApproved(Vendor vendor) {
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            // Treat PENDING_REVIEW and REJECTED as not found (404) for
            // security-by-concealment.
            throw new ResourceNotFoundException("Vendor not found.");
        }
    }

    /**
     * Retrieves menu categories for a vendor with database-level ordering.
     * <p>
     * This method intentionally performs no in-memory sorting; it relies on
     * the repository's derived query to order by {@code displayOrder ASC}.
     * </p>
     *
     * @param vendorId the requested vendor id
     * @return all menu categories for the vendor ordered by displayOrder ascending
     */
    private List<MenuCategory> getOrderedMenuCategories(UUID vendorId) {
        return menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(vendorId);
    }

    private List<MenuItem> getAvailableMenuItems(UUID vendorId) {
        return menuItemRepository.findByVendorIdAndIsAvailableTrue(vendorId);
    }

    /**
     * Removes categories that have no matching available items.
     * <p>
     * Category ordering is preserved because filtering is applied in a way that
     * keeps the original iteration order of {@code categories} (which is already
     * sorted by {@code displayOrder ASC} from the repository).
     * </p>
     *
     * @param categories     the ordered categories for a vendor
     * @param availableItems the available menu items (already filtered by
     *                       is_available = true)
     * @return filtered categories containing only those with at least one available
     *         item
     */
    private List<MenuCategory> removeEmptyCategories(List<MenuCategory> categories, List<MenuItem> availableItems) {
        Set<UUID> populatedCategoryIds = availableItems.stream()
                .map(item -> item.getCategory().getId())
                .collect(Collectors.toSet());

        return categories.stream()
                .filter(category -> populatedCategoryIds.contains(category.getId()))
                .toList();
    }

    private static VendorSummaryResponse toVendorSummary(Vendor vendor, double distanceKm) {
        return new VendorSummaryResponse(
                vendor.getId(),
                vendor.getBusinessName(),
                vendor.getFoodType(),
                vendor.getAddress(),
                vendor.getAverageRating(),
                vendor.getLatitude().doubleValue(),
                vendor.getLongitude().doubleValue(),
                distanceKm);
    }

    public VendorMenuResponseDto getVendorMenu(UUID vendorId) {
        Vendor vendor = getVendorOrThrow(vendorId);
        validateVendorApproved(vendor);

        List<MenuItem> availableItems = getAvailableMenuItems(vendorId);
        List<MenuCategory> categories = getOrderedMenuCategories(vendorId);
        categories = removeEmptyCategories(categories, availableItems);

        Map<UUID, List<MenuItem>> itemsByCategory = availableItems.stream()
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        List<MenuCategoryResponseDto> categoryDtos = categories.stream()
                .map(category -> {
                    List<MenuItem> itemsForCategory = itemsByCategory.getOrDefault(category.getId(), List.of());

                    List<MenuItemResponseDto> itemDtos = itemsForCategory.stream()
                            .map(item -> new MenuItemResponseDto(
                                    item.getId(),
                                    item.getName(),
                                    item.getDescription(),
                                    item.getPrice(),
                                    item.getDietaryTag(),
                                    item.getImageUrl(),
                                    item.isAvailable()))
                            .toList();

                    return new MenuCategoryResponseDto(
                            category.getId(),
                            category.getName(),
                            category.getDisplayOrder(),
                            itemDtos);
                })
                .toList();

        return new VendorMenuResponseDto(
                vendor.getId(),
                vendor.getBusinessName(),
                categoryDtos);
    }

    private static int calculateTotalPages(int totalElements, int pageSize) {
        if (totalElements == 0 || pageSize == 0) {
            return 0;
        }
        return (totalElements - 1) / pageSize + 1;
    }
}
