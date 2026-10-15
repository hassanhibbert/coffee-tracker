package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BeanRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.EquipmentRequest;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.entity.RoastLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Gives a new account a few beans, a few brewers and two weeks of brews, so the dashboard
 * and stats have something to show on first sign-in. Everything is marked as a sample and
 * belongs to that user alone, so they can edit or delete it like their own data.
 *
 * Goes through the services rather than the repositories: the same validation and grind
 * rules apply, so sample data can never be something the API itself would reject.
 */
@Service
public class SampleDataService {
    private static final String EDITABLE_NOTE = " (Sample brew: edit or delete it anytime.)";

    private final BeansService beansService;
    private final EquipmentService equipmentService;
    private final BrewsService brewsService;

    public SampleDataService(BeansService beansService, EquipmentService equipmentService,
                             BrewsService brewsService) {
        this.beansService = beansService;
        this.equipmentService = equipmentService;
        this.brewsService = brewsService;
    }

    /**
     * Joins the caller's transaction (propagation REQUIRED, the default), so when called from
     * registration, the new user and their sample data are saved together or not at all.
     */
    @Transactional
    public void addTo(Long ownerId) {
        addTo(ownerId, EDITABLE_NOTE);
    }

    /** For the read-only demo account, whose brews can't be edited, so the notes don't say they can. */
    @Transactional
    public void addToReadOnlyDemo(Long ownerId) {
        addTo(ownerId, "");
    }

    private void addTo(Long ownerId, String noteSuffix) {
        long ethiopia = beansService.create(
                new BeanRequest("Yirgacheffe", RoastLevel.LIGHT, "Ethiopia"), ownerId).id();
        long colombia = beansService.create(
                new BeanRequest("Huila", RoastLevel.MEDIUM, "Colombia"), ownerId).id();
        long vietnam = beansService.create(
                new BeanRequest("Da Lat Robusta", RoastLevel.DARK, "Vietnam"), ownerId).id();

        long v60 = equipmentService.create(
                new EquipmentRequest("V60", BrewMethod.POUR_OVER, "Hario"), ownerId).id();
        long aeropress = equipmentService.create(
                new EquipmentRequest("AeroPress", BrewMethod.AEROPRESS, "AeroPress"), ownerId).id();
        long phin = equipmentService.create(
                new EquipmentRequest("Phin filter", BrewMethod.PHIN, null), ownerId).id();

        Instant now = Instant.now();
        brew(ownerId, noteSuffix, ethiopia, v60, GrindSize.MEDIUM, "15", "250", 94, 180, 5, daysAgo(now, 1),
                "Bright and floral, jasmine and lemon.");
        brew(ownerId, noteSuffix, ethiopia, v60, GrindSize.MEDIUM_FINE, "15", "250", 94, 225, 3, daysAgo(now, 3),
                "Too fine: slow drawdown, bitter finish.");
        brew(ownerId, noteSuffix, colombia, v60, GrindSize.MEDIUM, "20", "320", 93, 200, 4, daysAgo(now, 4),
                "Caramel and red apple. Balanced.");
        brew(ownerId, noteSuffix, colombia, aeropress, GrindSize.FINE, "17", "220", 85, 120, 4, daysAgo(now, 6),
                "Inverted method. Chocolatey and clean.");
        brew(ownerId, noteSuffix, ethiopia, aeropress, GrindSize.MEDIUM, "15", "230", 88, 150, 5, daysAgo(now, 8),
                "Lower temperature kept it sweet.");
        brew(ownerId, noteSuffix, vietnam, phin, GrindSize.MEDIUM, "20", "90", 98, 300, 5, daysAgo(now, 9),
                "Over condensed milk. Strong, syrupy, perfect.");
        brew(ownerId, noteSuffix, vietnam, phin, GrindSize.FINE, "20", "90", 98, 420, 2, daysAgo(now, 11),
                "Too fine: the phin choked and dripped forever.");
        brew(ownerId, noteSuffix, colombia, aeropress, GrindSize.MEDIUM, "16", "240", 90, 105, 3, daysAgo(now, 13),
                "A bit thin. Try a longer steep.");
    }

    private void brew(Long ownerId, String noteSuffix, long beanId, long equipmentId, GrindSize grind, String coffeeGrams,
                      String waterGrams, int waterTempC, int brewTimeSeconds, int rating, Instant brewedAt,
                      String notes) {
        brewsService.create(new BrewRequest(beanId, equipmentId, grind, new BigDecimal(coffeeGrams),
                new BigDecimal(waterGrams), waterTempC, brewTimeSeconds, rating, notes + noteSuffix,
                brewedAt), ownerId);
    }

    private static Instant daysAgo(Instant now, int days) {
        return now.minus(Duration.ofDays(days));
    }
}
