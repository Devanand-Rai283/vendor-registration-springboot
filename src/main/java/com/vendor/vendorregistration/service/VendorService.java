package com.vendor.vendorregistration.service;

import com.vendor.vendorregistration.entity.Vendor;
import java.util.List;

public interface VendorService {

    Vendor createVendor(Vendor vendor);

    List<Vendor> getAllVendors();

    Vendor getVendorById(Long id);

    Vendor updateVendor(Long id, Vendor vendor);

    void deleteVendor(Long id);
}


