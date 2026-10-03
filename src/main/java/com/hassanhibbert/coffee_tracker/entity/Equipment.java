package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "brand", nullable = false)
    private  String brand;


    protected Equipment() {}

    public Equipment(String name, String type, String brand) {
        this.name = name;
        this.type = type;
        this.brand = brand;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getName() {
        return name;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setBrand(String brand){
        this.brand = brand;
    }

    public void setName(String name){
        this.name = name;
    }
}
