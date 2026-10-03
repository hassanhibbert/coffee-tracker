package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "brew_method", nullable = false, length = 20)
    private BrewMethod method;

    // Optional: a Vietnamese phin from a street market often has no brand.
    @Column(name = "brand", length = 60)
    private String brand;

    protected Equipment() {}

    public Equipment(String name, BrewMethod method, String brand) {
        this.name = name;
        this.method = method;
        this.brand = brand;
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

    public BrewMethod getMethod() {
        return method;
    }

    public void setMethod(BrewMethod method) {
        this.method = method;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
