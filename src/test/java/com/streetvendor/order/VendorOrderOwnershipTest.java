package com.streetvendor.order;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.controller.VendorOrderController;
import com.streetvendor.order.service.OrderHistoryService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
public class VendorOrderOwnershipTest extends AbstractSecurityTest {

    @MockitoBean
    private VendorService vendorService;

    @MockitoBean
    private OrderHistoryService orderHistoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String createToken(String email, Role role, UUID userId) {
        User user = new User(userId, email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE);
        userRepository.save(user);
        return jwtService.generateAccessToken(userId, email, role.name());
    }

    @Test
    void vendorCanAccessTheirOwnOrder() throws Exception {
        UUID vendorId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String token = createToken("vendor@test.com", Role.VENDOR, userId);

        Vendor mockVendor = mock(Vendor.class);
        when(mockVendor.getId()).thenReturn(vendorId);

        when(vendorService.getVendorByUserId(userId)).thenReturn(mockVendor);

        mockMvc.perform(get("/api/vendors/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(orderHistoryService).getVendorOrderDetail(orderId, vendorId);
    }
    
    // Note: The actual "cannot access other's order" test would test OrderHistoryServiceImpl behavior
    // because the controller just passes orderId and vendorId to the service. The service uses
    // orderRepository.findByIdAndVendorId(orderId, vendorId). If it returns empty, it throws
    // ResourceNotFoundException. Since the controller passes the authenticated vendorId, there's no way
    // to pass someone else's vendorId from the controller layer unless there's a flaw.
    // Testing the integration ensures the right vendorId is passed to the service.
}
