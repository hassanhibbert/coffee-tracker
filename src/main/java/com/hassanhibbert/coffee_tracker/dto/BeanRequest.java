package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.RoastLevel;

public record BeanRequest(String name, RoastLevel roastLevel, String countryOrigin) {
}
