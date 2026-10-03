package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.exception.BrewRuleViolationException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrewsService {
    private final BrewsRepository brewsRepository;
    private final BeansRepository beansRepository;
    private final EquipmentRepository equipmentRepository;

    public BrewsService(BrewsRepository brewsRepository, BeansRepository beansRepository,
                        EquipmentRepository equipmentRepository) {
        this.brewsRepository = brewsRepository;
        this.beansRepository = beansRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public List<BrewResponse> findAll() {
        return brewsRepository.findAllByOrderByBrewedAtDesc().stream().map(BrewResponse::fromEntity).toList();
    }

    public BrewResponse findById(Long id) {
        return BrewResponse.fromEntity(getEntity(id));
    }

    @Transactional
    public BrewResponse create(BrewRequest request) {
        Beans bean = findBean(request.beanId());
        Equipment equipment = findEquipment(request.equipmentId());
        checkGrindSuitsMethod(request.grindSize(), equipment);
        Instant brewedAt = request.brewedAt() != null ? request.brewedAt() : Instant.now();

        Brews brew = new Brews(bean, equipment, request.grindSize(), request.coffeeGrams(), request.waterGrams(),
                request.waterTempC(), request.brewTimeSeconds(), request.rating(), request.notes(), brewedAt);
        return BrewResponse.fromEntity(brewsRepository.save(brew));
    }

    @Transactional
    public BrewResponse update(Long id, BrewRequest request) {
        Brews brew = getEntity(id);
        brew.setBean(findBean(request.beanId()));
        Equipment equipment = findEquipment(request.equipmentId());
        checkGrindSuitsMethod(request.grindSize(), equipment);
        brew.setEquipment(equipment);
        brew.setGrindSize(request.grindSize());
        brew.setCoffeeGrams(request.coffeeGrams());
        brew.setWaterGrams(request.waterGrams());
        brew.setWaterTempC(request.waterTempC());
        brew.setBrewTimeSeconds(request.brewTimeSeconds());
        brew.setRating(request.rating());
        brew.setNotes(request.notes());
        if (request.brewedAt() != null) {
            brew.setBrewedAt(request.brewedAt());
        }
        return BrewResponse.fromEntity(brew);
    }

    @Transactional
    public void delete(Long id) {
        brewsRepository.delete(getEntity(id));
    }

    /**
     * Cross-entity rule: it depends on the equipment row in the database, so it can't be a
     * field annotation on BrewRequest. It lives here, where the equipment is already loaded.
     */
    private void checkGrindSuitsMethod(GrindSize grindSize, Equipment equipment) {
        BrewMethod method = equipment.getMethod();
        if (!method.accepts(grindSize)) {
            throw new BrewRuleViolationException("Grind size " + grindSize + " does not suit " + method
                    + " (expected " + method.getFinest() + " to " + method.getCoarsest() + ")");
        }
    }

    private Brews getEntity(Long id) {
        return brewsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Brew", id));
    }

    private Beans findBean(Long id) {
        return beansRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bean", id));
    }

    private Equipment findEquipment(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
    }
}
