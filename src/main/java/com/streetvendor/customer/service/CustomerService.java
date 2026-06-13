package com.streetvendor.customer.service;

import com.streetvendor.customer.entity.Customer;

import java.math.BigDecimal;
import java.util.UUID;

public interface CustomerService {

    Customer createProfile(UUID userId, String fullName, String phone, String address, BigDecimal latitude, BigDecimal longitude);
}
