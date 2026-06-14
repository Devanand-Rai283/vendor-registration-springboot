package com.streetvendor.vendor.repository;

import com.streetvendor.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<Vendor> findByUserId(UUID userId);
}