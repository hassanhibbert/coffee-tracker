package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "brews")
public class Brews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bean_id", nullable = false)
    private Beans bean;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "grind_size", nullable = false, length = 20)
    private GrindSize grindSize;

    // BigDecimal, not double: 0.1 + 0.2 != 0.3 in binary floating point, and these map
    // exactly onto Postgres NUMERIC columns.
    @Column(name = "coffee_grams", nullable = false, precision = 5, scale = 1)
    private BigDecimal coffeeGrams;

    @Column(name = "water_grams", nullable = false, precision = 6, scale = 1)
    private BigDecimal waterGrams;

    // Integer (nullable wrapper), not int: cold brew has no meaningful water temperature,
    // and an int field can't represent "no value" — it would silently become 0.
    @Column(name = "water_temp_c")
    private Integer waterTempC;

    @Column(name = "brew_time_seconds", nullable = false)
    private Integer brewTimeSeconds;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "brewed_at", nullable = false)
    private Instant brewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    protected Brews() {}

    public Brews(AppUser owner, Beans bean, Equipment equipment, GrindSize grindSize, BigDecimal coffeeGrams,
                 BigDecimal waterGrams, Integer waterTempC, Integer brewTimeSeconds,
                 Integer rating, String notes, Instant brewedAt) {
        this.owner = owner;
        this.bean = bean;
        this.equipment = equipment;
        this.grindSize = grindSize;
        this.coffeeGrams = coffeeGrams;
        this.waterGrams = waterGrams;
        this.waterTempC = waterTempC;
        this.brewTimeSeconds = brewTimeSeconds;
        this.rating = rating;
        this.notes = notes;
        this.brewedAt = brewedAt;
    }

    public Long getId() { return id; }

    public Beans getBean() { return bean; }

    public void setBean(Beans bean) { this.bean = bean; }

    public Equipment getEquipment() { return equipment; }

    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public GrindSize getGrindSize() { return grindSize; }

    public void setGrindSize(GrindSize grindSize) { this.grindSize = grindSize; }

    public BigDecimal getCoffeeGrams() { return coffeeGrams; }

    public void setCoffeeGrams(BigDecimal coffeeGrams) { this.coffeeGrams = coffeeGrams; }

    public BigDecimal getWaterGrams() { return waterGrams; }

    public void setWaterGrams(BigDecimal waterGrams) { this.waterGrams = waterGrams; }

    public Integer getWaterTempC() { return waterTempC; }

    public void setWaterTempC(Integer waterTempC) { this.waterTempC = waterTempC; }

    public Integer getBrewTimeSeconds() { return brewTimeSeconds; }

    public void setBrewTimeSeconds(Integer brewTimeSeconds) { this.brewTimeSeconds = brewTimeSeconds; }

    public Integer getRating() { return rating; }

    public void setRating(Integer rating) { this.rating = rating; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public Instant getBrewedAt() { return brewedAt; }

    public void setBrewedAt(Instant brewedAt) { this.brewedAt = brewedAt; }

    public AppUser getOwner() { return owner; }
}
