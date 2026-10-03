package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.repository.EquipmentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brews")
public class BrewsController {

    private final BrewsRepository brewsRepository;
    private final BeansRepository beansRepository;
    private final EquipmentRepository equipmentRepository;

    public BrewsController(BrewsRepository brewsRepository, BeansRepository beansRepository, EquipmentRepository equipmentRepository) {
        this.brewsRepository = brewsRepository;
        this.beansRepository = beansRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @GetMapping
    public List<Brews> getAllBrews() {
        return brewsRepository.findAll();
    }

    @PostMapping
    public  Brews createBrew(@RequestBody BrewRequest request) {
        Beans bean = beansRepository.findById(request.getBeanId()).orElseThrow(() -> new RuntimeException("Bean not found"));

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElseThrow(()-> new RuntimeException("Equipment not found"));

        Brews brew = new Brews(bean, equipment, request.getGroundSize());

        return brewsRepository.save(brew);
    }
}
