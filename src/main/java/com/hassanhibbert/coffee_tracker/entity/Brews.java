package com.hassanhibbert.coffee_tracker.entity;

import jakarta.persistence.*;

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

    @Column(name = "ground_size", nullable = false)
    private String groundSize;


    protected Brews() {}

    public Brews(Beans bean, Equipment equipment, String groundSize) {
        this.bean = bean;
        this.equipment = equipment;
        this.groundSize = groundSize;
    }


    public Long getId() { return id; }

    public Beans getBean() { return bean; }

    public Equipment getEquipment() { return  equipment; }

    public String getGroundSize() { return groundSize; }

    public void setGroundSize(String groundSize) {
        this.groundSize = groundSize;
    }


}
