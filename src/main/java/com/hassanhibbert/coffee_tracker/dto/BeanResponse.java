package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.Beans;

public class BeanResponse {
    private Long id;
    private String name;
    private String roastLevel;
    private String countryOrigin;

    public BeanResponse(Long id, String name, String roastLevel, String countryOrigin) {
        this.id = id;
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

    public String getRoastLevel() {
        return roastLevel;
    }

    public String getCountryOrigin() {
        return countryOrigin;
    }


    public static  BeanResponse fromEntity(Beans bean) {
        return new BeanResponse(bean.getId(), bean.getName(), bean.getRoastLevel(), bean.getCountryOrigin());
    }
}



