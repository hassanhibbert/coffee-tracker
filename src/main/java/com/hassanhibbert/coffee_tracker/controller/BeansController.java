package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beans")
public class BeansController {
    private final BeansRepository beansRepository;

    public BeansController(BeansRepository beansRepository) {
        this.beansRepository = beansRepository;
    }

    @GetMapping
    public List<Beans> getAllBeans() {
        return beansRepository.findAll();
    }

    @PostMapping
    public Beans createBean(@RequestBody Beans bean) {
        return beansRepository.save(bean);
    }

}
