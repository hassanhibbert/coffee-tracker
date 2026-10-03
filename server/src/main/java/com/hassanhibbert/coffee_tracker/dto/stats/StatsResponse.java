package com.hassanhibbert.coffee_tracker.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StatsResponse(long totalBrews, @Schema(nullable = true) Double averageRating, List<MethodStat> byMethod, List<BeanStat> topBeans) {
}
