package com.streetvendor.vendor.entity;

import com.streetvendor.common.audit.AuditableEntity;
import com.streetvendor.vendor.enums.VendorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "vendors",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vendor_user", columnNames = "user_id")
    }
)
public class Vendor extends AuditableEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private com.streetvendor.auth.entity.User user;

    @Column(nullable = false)
    private String businessName;

    private String ownerName;

    private String phone;

    private String foodType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private Integer totalReviews;

    protected Vendor() {
    }

    public Vendor(UUID id, com.streetvendor.auth.entity.User user, String businessName) {
        this.id = id;
        this.user = user;
        this.businessName = businessName;
        this.status = VendorStatus.PENDING_REVIEW;
        this.averageRating = BigDecimal.ZERO;
        this.totalReviews = 0;
    }

    public UUID getId() {
        return id;
    }

    public com.streetvendor.auth.entity.User getUser() {
        return user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }
}