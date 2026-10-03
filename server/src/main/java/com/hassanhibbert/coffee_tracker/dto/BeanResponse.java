package com.hassanhibbert.coffee_tracker.dto;

import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.RoastLevel;

public record BeanResponse(Long id, String name, RoastLevel roastLevel, String countryOrigin) {

    public static BeanResponse fromEntity(Beans bean) {
        return new BeanResponse(bean.getId(), bean.getName(), bean.getRoastLevel(), bean.getCountryOrigin());
    }
}
