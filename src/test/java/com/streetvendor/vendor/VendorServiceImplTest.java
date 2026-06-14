package com.streetvendor.vendor;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import com.streetvendor.vendor.service.VendorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private User vendorUser;
    private User customerUser;
    private CreateVendorRequest validRequest;

    @BeforeEach
    void setUp() {
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hash", Role.VENDOR, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", "hash", Role.CUSTOMER, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        validRequest = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );
    }

    private void setAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void shouldCreateVendorSuccessfully() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            return vendor;
        });

        VendorResponse response = vendorService.createVendor(validRequest);

        assertNotNull(response);
        assertEquals(VendorStatus.PENDING_REVIEW, response.status());
        assertEquals("Vendor profile created successfully.", response.message());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void shouldRejectDuplicateVendor() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.createVendor(validRequest);
        });

        assertEquals("Vendor profile already exists", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void shouldRejectNonVendorUser() {
        setAuthentication(customerUser);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            vendorService.createVendor(validRequest);
        });

        assertEquals("Only vendors can create vendor profiles", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void shouldLinkVendorToAuthenticatedUser() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            assertEquals(vendorUser.getId(), vendor.getUser().getId());
            return vendor;
        });

        vendorService.createVendor(validRequest);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void shouldForceStatusToPendingReview() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            assertEquals(VendorStatus.PENDING_REVIEW, vendor.getStatus());
            return vendor;
        });

        vendorService.createVendor(validRequest);

        verify(vendorRepository).save(any(Vendor.class));
    }
}