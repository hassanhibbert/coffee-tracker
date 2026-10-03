package com.hassanhibbert.coffee_tracker.entity;

/**
 * Declared from finest to coarsest. The declaration order matters: enums compare by
 * their ordinal (position), so {@code FINE.compareTo(COARSE) < 0}. BrewMethod relies on that.
 */
public enum GrindSize {
    EXTRA_FINE,
    FINE,
    MEDIUM_FINE,
    MEDIUM,
    MEDIUM_COARSE,
    COARSE,
    EXTRA_COARSE
}
