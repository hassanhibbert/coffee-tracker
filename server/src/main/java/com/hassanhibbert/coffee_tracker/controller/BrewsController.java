package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.dto.PageResponse;
import com.hassanhibbert.coffee_tracker.service.BrewsService;
import com.hassanhibbert.coffee_tracker.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/brews")
public class BrewsController {
    private final BrewsService brewsService;

    public BrewsController(BrewsService brewsService) {
        this.brewsService = brewsService;
    }

    /** e.g. GET /api/v1/brews?method=ESPRESSO&minRating=4&page=0&size=20&sort=rating,desc */
    @GetMapping
    public PageResponse<BrewResponse> searchBrews(@ParameterObject @Valid BrewFilter filter,
                                                  @ParameterObject @PageableDefault(size = 20, sort = "brewedAt",
                                                          direction = Sort.Direction.DESC) Pageable pageable,
                                                  @CurrentUserId Long userId) {
        return brewsService.search(userId, filter, pageable);
    }

    @GetMapping("/{id}")
    public BrewResponse getBrew(@PathVariable Long id, @CurrentUserId Long userId) {
        return brewsService.findById(id, userId);
    }

    @PostMapping
    public ResponseEntity<BrewResponse> createBrew(@Valid @RequestBody BrewRequest request, @CurrentUserId Long userId) {
        BrewResponse created = brewsService.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BrewResponse updateBrew(@PathVariable Long id, @Valid @RequestBody BrewRequest request, @CurrentUserId Long userId) {
        return brewsService.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrew(@PathVariable Long id, @CurrentUserId Long userId) {
        brewsService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
