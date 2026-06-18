package com.streetvendor.rating.controller;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.rating.dto.CreateRatingRequest;
import com.streetvendor.rating.dto.RatingResponse;
import com.streetvendor.rating.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "Ratings", description = "Endpoints for vendor rating and review system")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @Operation(summary = "Submit a rating and review for a completed order")
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRatingRequest request) {

        if (user == null) {
            throw new UnauthorizedException("User must be authenticated");
        }

        RatingResponse response = ratingService.createRating(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
