package com.streetvendor.menu.repository;

import com.streetvendor.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findByVendorIdOrderByDisplayOrderAsc(UUID vendorId);

    Optional<MenuCategory> findByIdAndVendorId(UUID categoryId, UUID vendorId);

    boolean existsByVendorIdAndNameIgnoreCase(UUID vendorId, String name);
}
