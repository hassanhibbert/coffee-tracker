package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import com.hassanhibbert.coffee_tracker.entity.Equipment;

public record EquipmentResponse(Long id, String name, BrewMethod method,
                                @Schema(nullable = true) String brand) {

    public static EquipmentResponse fromEntity(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getName(), equipment.getMethod(), equipment.getBrand());
    }
}
