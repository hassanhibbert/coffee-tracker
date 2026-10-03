package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A record is an immutable data carrier: the compiler generates the constructor,
 * accessors (beanId(), not getBeanId()), equals/hashCode and toString.
 * Ideal for DTOs, but not for JPA entities (Hibernate needs a no-arg constructor
 * and mutable fields).
 *
 * These constraints mirror the CHECK constraints in V1: the database is the backstop,
 * this layer produces the readable 400 response.
 */
public record BrewRequest(
        @NotNull @Positive Long beanId,
        @NotNull @Positive Long equipmentId,
        @NotNull GrindSize grindSize,
        @NotNull @DecimalMin("0.1") @Digits(integer = 4, fraction = 1) BigDecimal coffeeGrams,
        @NotNull @DecimalMin("0.1") @Digits(integer = 5, fraction = 1) BigDecimal waterGrams,
        @Min(0) @Max(100) Integer waterTempC,
        @NotNull @Positive Integer brewTimeSeconds,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String notes,
        @PastOrPresent Instant brewedAt
) {
}
