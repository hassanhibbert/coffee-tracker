package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BeanRequest;
import com.hassanhibbert.coffee_tracker.dto.BeanResponse;
import com.hassanhibbert.coffee_tracker.service.BeansService;
import com.hassanhibbert.coffee_tracker.security.CurrentUserId;
import jakarta.validation.Valid;
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
    public List<BeanResponse> getAllBeans(@CurrentUserId Long userId) {
        return beansService.findAll(userId);
    }

    @GetMapping("/{id}")
    public BeanResponse getBean(@PathVariable Long id, @CurrentUserId Long userId) {
        return beansService.findById(id, userId);
    }

    @PostMapping
    public ResponseEntity<BeanResponse> createBean(@Valid @RequestBody BeanRequest request, @CurrentUserId Long userId) {
        BeanResponse created = beansService.create(request, userId);
        // 201 Created + a Location header pointing at the new resource.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BeanResponse updateBean(@PathVariable Long id, @Valid @RequestBody BeanRequest request, @CurrentUserId Long userId) {
        return beansService.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBean(@PathVariable Long id, @CurrentUserId Long userId) {
        beansService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
