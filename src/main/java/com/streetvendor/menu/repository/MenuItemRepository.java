package com.streetvendor.menu.repository;

import com.streetvendor.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByVendorIdOrderByCreatedAtAsc(UUID vendorId);

    List<MenuItem> findByVendorIdAndIsAvailableTrue(UUID vendorId);

    Optional<MenuItem> findByIdAndVendorId(UUID itemId, UUID vendorId);
}
