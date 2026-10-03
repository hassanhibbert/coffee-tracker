package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record BrewResponse(
        Long id,
        BeanSummary bean,
        EquipmentSummary equipment,
        GrindSize grindSize,
        BigDecimal coffeeGrams,
        BigDecimal waterGrams,
        String ratio,
        @Schema(nullable = true) Integer waterTempC,
        Integer brewTimeSeconds,
        @Schema(nullable = true) Integer rating,
        @Schema(nullable = true) String notes,
        Instant brewedAt
) {
    public record BeanSummary(Long id, String name) {}

    public record EquipmentSummary(Long id, String name, BrewMethod method) {}

    /**
     * Touches the lazy bean/equipment associations, so it must be called while the
     * Hibernate session is still open, i.e. inside a @Transactional service method.
     */
    public static BrewResponse fromEntity(Brews brew) {
        return new BrewResponse(
                brew.getId(),
                new BeanSummary(brew.getBean().getId(), brew.getBean().getName()),
                new EquipmentSummary(brew.getEquipment().getId(), brew.getEquipment().getName(),
                        brew.getEquipment().getMethod()),
                brew.getGrindSize(),
                brew.getCoffeeGrams(),
                brew.getWaterGrams(),
                ratio(brew.getCoffeeGrams(), brew.getWaterGrams()),
                brew.getWaterTempC(),
                brew.getBrewTimeSeconds(),
                brew.getRating(),
                brew.getNotes(),
                brew.getBrewedAt());
    }

    /** Coffee-to-water ratio in the usual "1:16.0" notation. */
    static String ratio(BigDecimal coffeeGrams, BigDecimal waterGrams) {
        return "1:" + waterGrams.divide(coffeeGrams, 1, RoundingMode.HALF_UP).toPlainString();
    }
}
