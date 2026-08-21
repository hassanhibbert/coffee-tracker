package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;



@Entity
@Table(name = "beans")
public class Beans {
    protected Beans() {}

    public Beans(String name, String roastLevel, String countryOrigin) {
        this.name = name;
        this.roastLevel = roastLevel;
        this.countryOrigin = countryOrigin;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "roast_level", nullable = false)
    private String roastLevel;

    @Column(name = "country_origin", nullable = false)
    private String countryOrigin;


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoastLevel() {
        return roastLevel;
    }

    public void setRoastLevel(String roastLevel) {
        this.roastLevel = roastLevel;
    }

    public String getCountryOrigin() {
        return countryOrigin;
    }

    public void setCountryOrigin(String countryOrigin) {
        this.countryOrigin = countryOrigin;
    }


}
