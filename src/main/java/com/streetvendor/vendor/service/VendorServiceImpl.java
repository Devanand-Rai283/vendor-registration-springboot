package com.streetvendor.vendor.service;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    public VendorServiceImpl(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public VendorResponse createVendor(CreateVendorRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.streetvendor.common.exception.UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new com.streetvendor.common.exception.UnauthorizedException("Invalid authentication");
        }

        User user = (User) principal;

        if (user.getRole() != com.streetvendor.auth.entity.Role.VENDOR) {
            throw new ForbiddenException("Only vendors can create vendor profiles");
        }

        if (vendorRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Vendor profile already exists");
        }

        Vendor vendor = new Vendor(UUID.randomUUID(), user, request.businessName());
        vendor.setOwnerName(request.ownerName());
        vendor.setPhone(request.phone());
        vendor.setFoodType(request.foodType());
        vendor.setDescription(request.description());
        vendor.setLatitude(request.latitude());
        vendor.setLongitude(request.longitude());
        vendor.setAddress(request.address());
        vendor.setStatus(VendorStatus.PENDING_REVIEW);

        Vendor savedVendor = vendorRepository.save(vendor);

        return new VendorResponse(
                savedVendor.getId(),
                savedVendor.getStatus(),
                "Vendor profile created successfully."
        );
    }
}