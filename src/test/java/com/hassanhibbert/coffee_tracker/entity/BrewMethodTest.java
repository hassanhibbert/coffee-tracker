package com.hassanhibbert.coffee_tracker.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BrewMethodTest {

    // One test method, many cases: each CSV row runs as its own test in the report.
    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @CsvSource({
            "ESPRESSO,     EXTRA_FINE,    true",
            "ESPRESSO,     FINE,          true",
            "ESPRESSO,     MEDIUM_FINE,   false",
            "ESPRESSO,     COARSE,        false",
            "PHIN,         FINE,          true",
            "PHIN,         MEDIUM,        true",
            "PHIN,         COARSE,        false",
            "FRENCH_PRESS, EXTRA_COARSE,  true",
            "FRENCH_PRESS, MEDIUM,        false",
            "COLD_BREW,    FINE,          false",
    })
    void acceptsOnlyGrindSizesWithinItsRange(BrewMethod method, GrindSize grind, boolean expected) {
        assertThat(method.accepts(grind)).isEqualTo(expected);
    }
}
