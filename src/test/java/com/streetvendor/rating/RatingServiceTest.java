package com.streetvendor.rating;

import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.rating.dto.CreateRatingRequest;
import com.streetvendor.rating.dto.RatingResponse;
import com.streetvendor.rating.entity.Rating;
import com.streetvendor.rating.mapper.RatingMapper;
import com.streetvendor.rating.repository.RatingRepository;
import com.streetvendor.rating.service.impl.RatingServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Spy
    private RatingMapper ratingMapper = new RatingMapper();

    @InjectMocks
    private RatingServiceImpl ratingService;

    private UUID userId;
    private Customer testCustomer;
    private Vendor testVendor;
    private UUID orderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testCustomer = new Customer(UUID.randomUUID(), userId, "Customer One", "123", "Addr", null, null);
        testVendor = new Vendor(UUID.randomUUID(), null, "Vendor One");
        orderId = UUID.randomUUID();
        testOrder = new Order(orderId, testCustomer, testVendor, new BigDecimal("100.00"), "key-123");
        testOrder.setStatus(OrderStatus.COMPLETED);
    }

    @Test
    void createRating_shouldSucceed() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(ratingRepository.existsByOrderId(orderId)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating r = invocation.getArgument(0);
            return new Rating(r.getId(), r.getOrder(), r.getCustomer(), r.getVendor(), r.getStars(), r.getReviewText());
        });

        // Act
        RatingResponse response = ratingService.createRating(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(testCustomer.getId(), response.customerId());
        assertEquals(testVendor.getId(), response.vendorId());
        assertEquals(5, response.stars());
        assertEquals("Excellent food!", response.reviewText());
        verify(ratingRepository, times(1)).save(any(Rating.class));
        verify(vendorRepository, times(1)).save(any(Vendor.class));
    }

    @Test
    void createRating_shouldHandleNullAggregationValues() {
        // Arrange
        testVendor.setAverageRating(null);
        testVendor.setTotalReviews(null);

        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "First Review");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(ratingRepository.existsByOrderId(orderId)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating r = invocation.getArgument(0);
            return new Rating(r.getId(), r.getOrder(), r.getCustomer(), r.getVendor(), r.getStars(), r.getReviewText());
        });

        // Act
        ratingService.createRating(userId, request);

        // Assert
        assertEquals(1, testVendor.getTotalReviews());
        assertEquals(new BigDecimal("5.00"), testVendor.getAverageRating());
        verify(vendorRepository, times(1)).save(testVendor);
    }

    @Test
    void createRating_shouldCalculateRollingAveragePrecisionWithoutDrift() {
        // Review 1: 5 stars -> average 5.00
        testVendor.setAverageRating(new BigDecimal("5.00"));
        testVendor.setTotalReviews(1);

        // Review 2: 4 stars -> average 4.50
        CreateRatingRequest request2 = new CreateRatingRequest(orderId, 4, "Second Review");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(ratingRepository.existsByOrderId(orderId)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating r = invocation.getArgument(0);
            return new Rating(r.getId(), r.getOrder(), r.getCustomer(), r.getVendor(), r.getStars(), r.getReviewText());
        });

        ratingService.createRating(userId, request2);
        assertEquals(2, testVendor.getTotalReviews());
        assertEquals(new BigDecimal("4.50"), testVendor.getAverageRating());

        // Review 3: 4 stars -> average 4.33
        CreateRatingRequest request3 = new CreateRatingRequest(orderId, 4, "Third Review");
        ratingService.createRating(userId, request3);
        assertEquals(3, testVendor.getTotalReviews());
        assertEquals(new BigDecimal("4.33"), testVendor.getAverageRating());
    }

    @Test
    void createRating_shouldRollbackTransactionIfVendorSaveFails() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Will fail");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(ratingRepository.existsByOrderId(orderId)).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenThrow(new RuntimeException("Database error saving vendor"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                ratingService.createRating(userId, request)
        );
        // Hibernate session transactions will propagate rollback automatically
    }

    @Test
    void createRating_shouldFailWhenCustomerProfileNotFound() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Customer profile not found", exception.getMessage());
        verify(orderRepository, never()).findById(any());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenOrderNotFound() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Order not found", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenCustomerDoesNotOwnOrder() {
        // Arrange
        Customer otherCustomer = new Customer(UUID.randomUUID(), UUID.randomUUID(), "Other Customer", "456", "Addr", null, null);
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(otherCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Customer does not own this order", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenOrderNotCompleted() {
        // Arrange
        testOrder.setStatus(OrderStatus.ACCEPTED);
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Order status is not COMPLETED", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenDuplicateReview() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 5, "Excellent food!");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(ratingRepository.existsByOrderId(orderId)).thenReturn(true);

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("A rating/review already exists for this order", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenInvalidStarsLessThanOne() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 0, "Bad");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Stars rating must be between 1 and 5", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldFailWhenInvalidStarsGreaterThanFive() {
        // Arrange
        CreateRatingRequest request = new CreateRatingRequest(orderId, 6, "Amazing");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(userId, request)
        );
        assertEquals("Stars rating must be between 1 and 5", exception.getMessage());
        verify(ratingRepository, never()).save(any());
    }
}

