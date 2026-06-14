package com.streetvendor.vendor.service;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.dto.VendorStatusResponse;
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
    private final AuditService auditService;

    public VendorServiceImpl(VendorRepository vendorRepository, AuditService auditService) {
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
    }

    @Override
    public VendorResponse createVendor(CreateVendorRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        User user = (User) principal;

        if (user.getRole() != Role.VENDOR) {
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
                "Vendor profile created successfully.",
                null
        );
    }

    @Override
    public VendorStatusResponse getMyVendorStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        if (user.getRole() != Role.VENDOR) {
            throw new ForbiddenException("Only vendors can access their vendor profile");
        }

        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));

        return new VendorStatusResponse(
                vendor.getId(),
                vendor.getBusinessName(),
                vendor.getStatus(),
                vendor.getAverageRating()
        );
    }

    @Override
    public VendorResponse approveVendor(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        validateTransition(vendor.getStatus(), VendorStatus.APPROVED);

        vendor.setStatus(VendorStatus.APPROVED);
        Vendor savedVendor = vendorRepository.save(vendor);

        auditService.logEvent(AuditEventType.VENDOR_APPROVED, savedVendor.getId(), getCurrentAdminUserId(), null);

        return new VendorResponse(
                savedVendor.getId(),
                savedVendor.getStatus(),
                "Vendor approved successfully.",
                null
        );
    }

    @Override
    public VendorResponse rejectVendor(UUID vendorId, String reason) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        validateTransition(vendor.getStatus(), VendorStatus.REJECTED);

        vendor.setStatus(VendorStatus.REJECTED);
        vendor.setRejectionReason(reason);
        Vendor savedVendor = vendorRepository.save(vendor);

        auditService.logEvent(AuditEventType.VENDOR_REJECTED, savedVendor.getId(), getCurrentAdminUserId(), reason);

        return new VendorResponse(
                savedVendor.getId(),
                savedVendor.getStatus(),
                "Vendor rejected successfully.",
                savedVendor.getRejectionReason()
        );
    }

    private UUID getCurrentAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    public void validateTransition(VendorStatus current, VendorStatus target) {
        boolean allowed = (current == VendorStatus.PENDING_REVIEW && target == VendorStatus.APPROVED)
                       || (current == VendorStatus.PENDING_REVIEW && target == VendorStatus.REJECTED);
        if (!allowed) {
            throw new ConflictException(
                    "Cannot transition vendor from " + current + " to " + target);
        }
    }
}