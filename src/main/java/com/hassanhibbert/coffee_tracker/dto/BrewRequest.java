package com.hassanhibbert.coffee_tracker.dto;

public class BrewRequest {
    private Long beanId;
    private Long equipmentId;
    private String groundSize;

    public Long getBeanId() {
        return beanId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getGroundSize(){
        return groundSize;
    }

    public void setGroundSize(String groundSize) {
        this.groundSize = groundSize;
    }

    public void setBeanId(Long beanId) {
        this.beanId = beanId;
    }

    public void setEquipmentId(Long equipmentId){
        this.equipmentId  = equipmentId;
    }
}
