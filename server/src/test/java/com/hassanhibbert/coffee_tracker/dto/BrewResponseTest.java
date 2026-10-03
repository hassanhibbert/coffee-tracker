package com.hassanhibbert.coffee_tracker.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BrewResponseTest {

    @Test
    void ratioIsWaterOverCoffeeToOneDecimal() {
        assertThat(BrewResponse.ratio(new BigDecimal("15"), new BigDecimal("250"))).isEqualTo("1:16.7");
        assertThat(BrewResponse.ratio(new BigDecimal("18.0"), new BigDecimal("36.0"))).isEqualTo("1:2.0");
    }
}
