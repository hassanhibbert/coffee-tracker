package com.hassanhibbert.coffee_tracker.dto.stats;

import com.hassanhibbert.coffee_tracker.entity.BrewMethod;

/** averageRating is a Double (nullable) because AVG over only-unrated brews is SQL NULL. */
public record MethodStat(BrewMethod method, Long brewCount, Double averageRating) {
}
