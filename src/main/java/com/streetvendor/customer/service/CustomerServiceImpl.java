package com.streetvendor.customer.service;

import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer createProfile(UUID userId, String fullName, String phone, String address, BigDecimal latitude, BigDecimal longitude) {
        if (customerRepository.existsByUserId(userId)) {
            throw new ConflictException("Customer profile already exists for this user");
        }

        Customer customer = new Customer(
                UUID.randomUUID(),
                userId,
                fullName,
                phone,
                address,
                latitude,
                longitude
        );

        return customerRepository.save(customer);
    }
}
