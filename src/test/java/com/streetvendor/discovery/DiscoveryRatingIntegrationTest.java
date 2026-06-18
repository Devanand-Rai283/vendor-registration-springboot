package com.streetvendor.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.rating.dto.CreateRatingRequest;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class DiscoveryRatingIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customerUser;
    private Customer customer;
    private User vendorUser;
    private Vendor vendor;
    private String customerToken;

    @BeforeEach
    void setUp() {
        customerUser = new User(UUID.randomUUID(), "cust@test.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        customer = new Customer(UUID.randomUUID(), customerUser.getId(), "Customer Name", "12345", "Address", null, null);
        customerRepository.save(customer);

        vendorUser = new User(UUID.randomUUID(), "vend@test.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);
        vendor = new Vendor(UUID.randomUUID(), vendorUser, "Best Tacos");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setLatitude(BigDecimal.valueOf(12.9716));
        vendor.setLongitude(BigDecimal.valueOf(77.5946));
        vendor.setFoodType("Mexican");
        vendor.setAddress("123 Street");
        vendor.setAverageRating(BigDecimal.ZERO);
        vendor.setTotalReviews(0);
        vendorRepository.save(vendor);

        customerToken = jwtService.generateAccessToken(customerUser.getId(), "cust@test.com", "CUSTOMER");
    }

    @Test
    void shouldReflectNewRatingsInDiscoveryResults() throws Exception {
        // Create 3 completed orders
        Order order1 = orderRepository.save(new Order(UUID.randomUUID(), customer, vendor, new BigDecimal("100"), UUID.randomUUID().toString()));
        order1.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order1);

        Order order2 = orderRepository.save(new Order(UUID.randomUUID(), customer, vendor, new BigDecimal("150"), UUID.randomUUID().toString()));
        order2.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order2);

        Order order3 = orderRepository.save(new Order(UUID.randomUUID(), customer, vendor, new BigDecimal("200"), UUID.randomUUID().toString()));
        order3.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order3);

        // Verify initial nearby query returns averageRating = 0
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].averageRating").value(0.0));

        // Submit rating 1: 5 stars
        CreateRatingRequest rating1 = new CreateRatingRequest(order1.getId(), 5, "Excellent");
        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(rating1)))
                .andExpect(status().isCreated());

        // Verify nearby query returns averageRating = 5.0
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].averageRating").value(5.0));

        // Submit rating 2: 4 stars
        CreateRatingRequest rating2 = new CreateRatingRequest(order2.getId(), 4, "Good");
        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(rating2)))
                .andExpect(status().isCreated());

        // Submit rating 3: 4 stars
        CreateRatingRequest rating3 = new CreateRatingRequest(order3.getId(), 4, "Satisfied");
        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(rating3)))
                .andExpect(status().isCreated());

        // Verify nearby query returns averageRating = 4.33
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].averageRating").value(4.33));
    }
}
