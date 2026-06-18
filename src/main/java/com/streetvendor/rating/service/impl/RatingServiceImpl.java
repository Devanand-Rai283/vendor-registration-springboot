package com.streetvendor.rating.service.impl;

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
import com.streetvendor.rating.service.RatingService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final RatingMapper ratingMapper;
    private final VendorRepository vendorRepository;

    public RatingServiceImpl(RatingRepository ratingRepository,
                             OrderRepository orderRepository,
                             CustomerRepository customerRepository,
                             RatingMapper ratingMapper,
                             VendorRepository vendorRepository) {
        this.ratingRepository = ratingRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.ratingMapper = ratingMapper;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional
    public RatingResponse createRating(UUID userId, CreateRatingRequest request) {
        // Validate stars range early
        if (request.stars() < 1 || request.stars() > 5) {
            throw new IllegalArgumentException("Stars rating must be between 1 and 5");
        }

        // 1. Customer profile must exist
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Fetch order
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 3. Customer must own the order
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Customer does not own this order");
        }

        // 4. Order status must be COMPLETED
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Order status is not COMPLETED");
        }

        // 5. One review per order only (duplicate check)
        if (ratingRepository.existsByOrderId(request.orderId())) {
            throw new ConflictException("A rating/review already exists for this order");
        }

        // Construct rating
        Rating rating = new Rating(
                UUID.randomUUID(),
                order,
                customer,
                order.getVendor(),
                request.stars(),
                request.reviewText()
        );

        Rating savedRating = ratingRepository.save(rating);

        // Update vendor rating aggregations
        Vendor vendor = order.getVendor();
        BigDecimal currentAverage = vendor.getAverageRating() != null ? vendor.getAverageRating() : BigDecimal.ZERO;
        int currentTotal = vendor.getTotalReviews() != null ? vendor.getTotalReviews() : 0;

        BigDecimal newStars = BigDecimal.valueOf(request.stars());
        BigDecimal currentTotalBD = BigDecimal.valueOf(currentTotal);
        BigDecimal newTotalBD = BigDecimal.valueOf(currentTotal + 1);

        BigDecimal currentSum = currentAverage.multiply(currentTotalBD);
        BigDecimal newAverageHighPrecision = currentSum.add(newStars).divide(newTotalBD, 6, RoundingMode.HALF_UP);
        BigDecimal finalAverage = newAverageHighPrecision.setScale(2, RoundingMode.HALF_UP);

        vendor.setTotalReviews(currentTotal + 1);
        vendor.setAverageRating(finalAverage);
        vendorRepository.save(vendor);

        return ratingMapper.toResponse(savedRating);
    }
}

