package com.hassanhibbert.coffee_tracker.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

public record BeanStat(Long beanId, String name, Long brewCount, @Schema(nullable = true) Double averageRating) {
}
