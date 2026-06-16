package com.streetvendor.discovery.repository;

import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.vendor.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FoodSearchRepository extends JpaRepository<MenuItem, UUID> {

    @Query(value = """
            SELECT new com.streetvendor.discovery.dto.FoodSearchResponseDto(
                mi.id, mi.name, mi.description, mi.price, mi.dietaryTag,
                v.id, v.businessName, v.foodType, v.averageRating
            )
            FROM MenuItem mi
            JOIN mi.vendor v
            WHERE LOWER(mi.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND v.status = :status
              AND mi.isAvailable = true
              AND (:foodType IS NULL OR v.foodType = :foodType)
              AND (:dietaryTag IS NULL OR mi.dietaryTag = :dietaryTag)
            """,
            countQuery = """
                    SELECT COUNT(mi.id)
                    FROM MenuItem mi
                    JOIN mi.vendor v
                    WHERE LOWER(mi.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      AND v.status = :status
                      AND mi.isAvailable = true
                      AND (:foodType IS NULL OR v.foodType = :foodType)
                      AND (:dietaryTag IS NULL OR mi.dietaryTag = :dietaryTag)
                    """)
    Page<FoodSearchResponseDto> searchFoods(
            @Param("keyword") String keyword,
            @Param("foodType") String foodType,
            @Param("dietaryTag") String dietaryTag,
            @Param("status") VendorStatus status,
            Pageable pageable);
}
