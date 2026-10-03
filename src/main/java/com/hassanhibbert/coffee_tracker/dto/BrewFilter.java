package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * Optional query-string filters for GET /api/v1/brews, e.g. ?method=ESPRESSO&minRating=4.
 * Spring binds query parameters to the record's constructor; absent parameters are null.
 */
public record BrewFilter(
        Long beanId,
        Long equipmentId,
        BrewMethod method,
        GrindSize grindSize,
        @Min(1) @Max(5) Integer minRating,
        Instant from,
        Instant to
) {
}
