package com.streetvendor.customer;

import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.customer.service.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void shouldCreateProfileSuccessfully() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            return customer;
        });

        Customer customer = customerService.createProfile(
                userId,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );

        assertNotNull(customer);
        assertEquals(userId, customer.getUserId());
        assertEquals("John Doe", customer.getFullName());
        assertEquals("1234567890", customer.getPhone());
        assertEquals("123 Main St", customer.getAddress());
        assertEquals(new BigDecimal("40.71280000"), customer.getLatitude());
        assertEquals(new BigDecimal("-74.00600000"), customer.getLongitude());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectDuplicateProfile() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            customerService.createProfile(
                    userId,
                    "John Doe",
                    "1234567890",
                    "123 Main St",
                    new BigDecimal("40.71280000"),
                    new BigDecimal("-74.00600000")
            );
        });

        assertEquals("Customer profile already exists for this user", exception.getMessage());
    }

    @Test
    void shouldGenerateUUIDForNewCustomer() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            return customer;
        });

        Customer customer = customerService.createProfile(
                userId,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );

        assertNotNull(customer.getId());
    }

    @Test
    void shouldLinkCustomerToUser() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            return customer;
        });

        Customer customer = customerService.createProfile(
                userId,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );

        assertEquals(userId, customer.getUserId());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            return customer;
        });

        Customer customer = customerService.createProfile(
                userId,
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(customer);
        assertEquals(userId, customer.getUserId());
    }
}
