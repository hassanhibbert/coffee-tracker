package com.hassanhibbert.coffee_tracker.entity;

/**
 * Each brew method carries the range of grind sizes that make sense for it.
 * Java enums are full classes: they can have fields, constructors and methods.
 */
public enum BrewMethod {
    ESPRESSO(GrindSize.EXTRA_FINE, GrindSize.FINE),
    MOKA_POT(GrindSize.FINE, GrindSize.MEDIUM_FINE),
    AEROPRESS(GrindSize.FINE, GrindSize.MEDIUM),
    PHIN(GrindSize.FINE, GrindSize.MEDIUM),
    POUR_OVER(GrindSize.MEDIUM_FINE, GrindSize.MEDIUM_COARSE),
    FRENCH_PRESS(GrindSize.MEDIUM_COARSE, GrindSize.EXTRA_COARSE),
    COLD_BREW(GrindSize.COARSE, GrindSize.EXTRA_COARSE);

    private final GrindSize finest;
    private final GrindSize coarsest;

    BrewMethod(GrindSize finest, GrindSize coarsest) {
        this.finest = finest;
        this.coarsest = coarsest;
    }

    public boolean accepts(GrindSize grindSize) {
        return grindSize.compareTo(finest) >= 0 && grindSize.compareTo(coarsest) <= 0;
    }

    public GrindSize getFinest() {
        return finest;
    }

    public GrindSize getCoarsest() {
        return coarsest;
    }
}
