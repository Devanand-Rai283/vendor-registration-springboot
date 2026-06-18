package com.streetvendor.rating.service;

import com.streetvendor.rating.dto.CreateRatingRequest;
import com.streetvendor.rating.dto.RatingResponse;

import java.util.UUID;

public interface RatingService {
    RatingResponse createRating(UUID userId, CreateRatingRequest request);
}
