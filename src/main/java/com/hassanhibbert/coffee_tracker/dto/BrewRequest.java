package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.GrindSize;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A record is an immutable data carrier: the compiler generates the constructor,
 * accessors (beanId(), not getBeanId()), equals/hashCode and toString.
 * Ideal for DTOs, but not for JPA entities (Hibernate needs a no-arg constructor
 * and mutable fields).
 */
public record BrewRequest(
        Long beanId,
        Long equipmentId,
        GrindSize grindSize,
        BigDecimal coffeeGrams,
        BigDecimal waterGrams,
        Integer waterTempC,
        Integer brewTimeSeconds,
        Integer rating,
        String notes,
        Instant brewedAt
) {
}
