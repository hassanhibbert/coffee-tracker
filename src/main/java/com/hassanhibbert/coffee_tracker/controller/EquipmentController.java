package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.EquipmentRequest;
import com.hassanhibbert.coffee_tracker.dto.EquipmentResponse;
import com.hassanhibbert.coffee_tracker.service.EquipmentService;
import com.hassanhibbert.coffee_tracker.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {
    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentResponse> getAllEquipment(@CurrentUserId Long userId) {
        return equipmentService.findAll(userId);
    }

    @GetMapping("/{id}")
    public EquipmentResponse getEquipment(@PathVariable Long id, @CurrentUserId Long userId) {
        return equipmentService.findById(id, userId);
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request, @CurrentUserId Long userId) {
        EquipmentResponse created = equipmentService.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public EquipmentResponse updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request, @CurrentUserId Long userId) {
        return equipmentService.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id, @CurrentUserId Long userId) {
        equipmentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
