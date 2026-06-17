package com.streetvendor.discovery.cache;

import java.util.UUID;

public final class CacheKeyGenerator {
    private CacheKeyGenerator() {
    }

    public static String vendorSearchKey(double lat, double lng, double radius) {
        return String.format("%s:%s:%s:%s", CacheConstants.SEARCH_VENDORS, lat, lng, radius);
    }

    public static String vendorMenuKey(UUID vendorId) {
        return String.format("%s:%s", CacheConstants.VENDOR_MENU, vendorId);
    }
}
