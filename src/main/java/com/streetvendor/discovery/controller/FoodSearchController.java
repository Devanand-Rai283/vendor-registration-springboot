package com.streetvendor.discovery.controller;

import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.discovery.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Food Search", description = "Search for food items across approved vendors")
public class FoodSearchController {

    private final DiscoveryService discoveryService;

    public FoodSearchController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Operation(
        summary = "Search food items across approved vendors.",
        description = """
            Searches menu items by keyword.
            Supports optional filtering by food type and dietary tag.
            Returns only APPROVED vendors with available menu items.
            Supports pagination.
            No authentication required — this endpoint is publicly accessible.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of matching food items. Response includes content (array of FoodSearchResponseDto), totalElements, totalPages, number, and size."),
        @ApiResponse(responseCode = "400", description = """
            Bad Request — errors generated through the global exception handling mechanism.
            Possible causes:
            <ul>
              <li>blank keyword</li>
              <li>whitespace keyword</li>
              <li>invalid page size</li>
              <li>page size greater than 100</li>
            </ul>
            """)
    })
    @GetMapping("/search")
    public ResponseEntity<Page<FoodSearchResponseDto>> searchFoods(
            @Parameter(description = "Food item keyword used for searching", required = true, example = "paneer")
            @RequestParam String keyword,
            @Parameter(description = "Restrict results to vendors serving a specific food type", example = "VEGETARIAN")
            @RequestParam(required = false) String foodType,
            @Parameter(description = "Restrict results to menu items matching a dietary preference", example = "VEGAN")
            @RequestParam(required = false) String dietaryTag,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of results per page. Maximum is 100.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<FoodSearchResponseDto> results = discoveryService.searchFoods(keyword, foodType, dietaryTag, page, size);
        return ResponseEntity.ok(results);
    }
}
