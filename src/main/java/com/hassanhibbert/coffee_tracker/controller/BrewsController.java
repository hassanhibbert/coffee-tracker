package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.service.BrewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/brews")
public class BrewsController {
    private final BrewsService brewsService;

    public BrewsController(BrewsService brewsService) {
        this.brewsService = brewsService;
    }

    @GetMapping
    public List<BrewResponse> getAllBrews() {
        return brewsService.findAll();
    }

    @GetMapping("/{id}")
    public BrewResponse getBrew(@PathVariable Long id) {
        return brewsService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BrewResponse> createBrew(@RequestBody BrewRequest request) {
        BrewResponse created = brewsService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BrewResponse updateBrew(@PathVariable Long id, @RequestBody BrewRequest request) {
        return brewsService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrew(@PathVariable Long id) {
        brewsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
