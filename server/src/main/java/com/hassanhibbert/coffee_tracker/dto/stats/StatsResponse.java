package com.hassanhibbert.coffee_tracker.dto.stats;

import java.util.List;

public record StatsResponse(long totalBrews, Double averageRating, List<MethodStat> byMethod, List<BeanStat> topBeans) {
}
