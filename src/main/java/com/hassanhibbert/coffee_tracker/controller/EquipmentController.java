package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.EquipmentRequest;
import com.hassanhibbert.coffee_tracker.dto.EquipmentResponse;
import com.hassanhibbert.coffee_tracker.service.EquipmentService;
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
    public List<EquipmentResponse> getAllEquipment() {
        return equipmentService.findAll();
    }

    @GetMapping("/{id}")
    public EquipmentResponse getEquipment(@PathVariable Long id) {
        return equipmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@RequestBody EquipmentRequest request) {
        EquipmentResponse created = equipmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public EquipmentResponse updateEquipment(@PathVariable Long id, @RequestBody EquipmentRequest request) {
        return equipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
