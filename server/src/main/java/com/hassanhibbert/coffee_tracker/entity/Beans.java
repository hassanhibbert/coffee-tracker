package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "beans")
public class Beans {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // STRING stores "MEDIUM_DARK" in the column. The default (ORDINAL) stores 3, which
    // silently corrupts your data the day someone reorders or inserts an enum constant.
    @Enumerated(EnumType.STRING)
    @Column(name = "roast_level", nullable = false, length = 20)
    private RoastLevel roastLevel;

    @Column(name = "country_origin", nullable = false, length = 60)
    private String countryOrigin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    protected Beans() {}

    public Beans(AppUser owner, String name, RoastLevel roastLevel, String countryOrigin) {
        this.owner = owner;
        this.name = name;
        this.roastLevel = roastLevel;
        this.countryOrigin = countryOrigin;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoastLevel getRoastLevel() {
        return roastLevel;
    }

    public void setRoastLevel(RoastLevel roastLevel) {
        this.roastLevel = roastLevel;
    }

    public String getCountryOrigin() {
        return countryOrigin;
    }

    public void setCountryOrigin(String countryOrigin) {
        this.countryOrigin = countryOrigin;
    }

    public AppUser getOwner() {
        return owner;
    }
}
