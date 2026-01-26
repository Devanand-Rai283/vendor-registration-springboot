package com.vendor.vendorregistration.service.impl;

import com.vendor.vendorregistration.entity.Vendor;
import com.vendor.vendorregistration.exception.VendorNotFoundException;
import com.vendor.vendorregistration.repository.VendorRepository;
import com.vendor.vendorregistration.service.VendorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    public VendorServiceImpl(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public Vendor createVendor(Vendor vendor) {
        String code;
        do {
            code = "VND-" + UUID.randomUUID().toString().substring(0, 8);
        } while (vendorRepository.existsByVendorCode(code));

        vendor.setVendorCode(code);
        return vendorRepository.save(vendor);
    }

    @Override
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    @Override
    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() ->
                        new VendorNotFoundException("Vendor not found with id: " + id));
    }

    @Override
    public Vendor updateVendor(Long id, Vendor vendor) {
        Vendor existing = getVendorById(id);

        existing.setName(vendor.getName());
        existing.setAge(vendor.getAge());
        existing.setGender(vendor.getGender());
        existing.setEducation(vendor.getEducation());
        existing.setContactNumber(vendor.getContactNumber());
        existing.setEmail(vendor.getEmail());
        existing.setStallLocation(vendor.getStallLocation());
        existing.setFoodType(vendor.getFoodType());
        existing.setHygieneCertified(vendor.getHygieneCertified());

        return vendorRepository.save(existing);
    }

    @Override
    public void deleteVendor(Long id) {
        Vendor vendor = getVendorById(id);
        vendorRepository.delete(vendor);
    }
}

