package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BeanRequest;
import com.hassanhibbert.coffee_tracker.dto.BeanResponse;
import com.hassanhibbert.coffee_tracker.service.BeansService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/beans")
public class BeansController {
    private final BeansService beansService;

    public BeansController(BeansService beansService) {
        this.beansService = beansService;
    }

    @GetMapping
    public List<BeanResponse> getAllBeans() {
        return beansService.findAll();
    }

    @GetMapping("/{id}")
    public BeanResponse getBean(@PathVariable Long id) {
        return beansService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BeanResponse> createBean(@RequestBody BeanRequest request) {
        BeanResponse created = beansService.create(request);
        // 201 Created + a Location header pointing at the new resource.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BeanResponse updateBean(@PathVariable Long id, @RequestBody BeanRequest request) {
        return beansService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBean(@PathVariable Long id) {
        beansService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
