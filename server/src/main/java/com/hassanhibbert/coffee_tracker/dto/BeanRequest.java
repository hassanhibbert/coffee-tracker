package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.RoastLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BeanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull RoastLevel roastLevel,
        @NotBlank @Size(max = 60) String countryOrigin
) {
}
