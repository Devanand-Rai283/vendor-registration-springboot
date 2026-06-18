package com.streetvendor.rating.mapper;

import com.streetvendor.rating.dto.RatingResponse;
import com.streetvendor.rating.entity.Rating;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public RatingResponse toResponse(Rating rating) {
        if (rating == null) {
            return null;
        }
        return new RatingResponse(
                rating.getId(),
                rating.getOrder().getId(),
                rating.getCustomer().getId(),
                rating.getVendor().getId(),
                rating.getStars(),
                rating.getReviewText(),
                rating.getCreatedAt()
        );
    }
}
