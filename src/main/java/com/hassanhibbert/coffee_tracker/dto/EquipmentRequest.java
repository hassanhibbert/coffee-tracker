package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.BrewMethod;

public record EquipmentRequest(String name, BrewMethod method, String brand) {
}
