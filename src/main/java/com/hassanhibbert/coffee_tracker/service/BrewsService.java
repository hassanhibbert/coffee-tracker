package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.dto.PageResponse;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.exception.BrewRuleViolationException;
import com.hassanhibbert.coffee_tracker.exception.InvalidQueryException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewSpecifications;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.repository.EquipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BrewsService {
    private final BrewsRepository brewsRepository;
    private final BeansRepository beansRepository;
    private final EquipmentRepository equipmentRepository;
    private final AppUserRepository appUserRepository;

    public BrewsService(BrewsRepository brewsRepository, BeansRepository beansRepository,
                        EquipmentRepository equipmentRepository, AppUserRepository appUserRepository) {
        this.brewsRepository = brewsRepository;
        this.beansRepository = beansRepository;
        this.equipmentRepository = equipmentRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Sorting is client input too. Unchecked, ?sort=owner.passwordHash would be accepted, and the
     * order of results would leak information about a column the client can't otherwise see.
     */
    static final Set<String> SORTABLE = Set.of("brewedAt", "rating", "coffeeGrams", "waterGrams", "brewTimeSeconds");

    public PageResponse<BrewResponse> search(Long ownerId, BrewFilter filter, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidQueryException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
            }
        }
        // Tie-breaker on id: rows with equal sort values otherwise have no defined order,
        // and the same brew can appear on two pages (or none) as the user pages through.
        Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Brews> page = brewsRepository.findAll(BrewSpecifications.matching(ownerId, filter), stable);
        return PageResponse.from(page, BrewResponse::fromEntity);
    }

    public BrewResponse findById(Long id, Long ownerId) {
        return BrewResponse.fromEntity(getEntity(id, ownerId));
    }

    @Transactional
    public BrewResponse create(BrewRequest request, Long ownerId) {
        // Looking up bean and equipment *by owner* is the IDOR check: a beanId belonging to
        // another user is "not found", so nobody can attach brews to someone else's beans.
        Beans bean = findBean(request.beanId(), ownerId);
        Equipment equipment = findEquipment(request.equipmentId(), ownerId);
        checkGrindSuitsMethod(request.grindSize(), equipment);
        Instant brewedAt = request.brewedAt() != null ? request.brewedAt() : Instant.now();

        Brews brew = new Brews(appUserRepository.getReferenceById(ownerId), bean, equipment, request.grindSize(),
                request.coffeeGrams(), request.waterGrams(), request.waterTempC(), request.brewTimeSeconds(),
                request.rating(), request.notes(), brewedAt);
        return BrewResponse.fromEntity(brewsRepository.save(brew));
    }

    @Transactional
    public BrewResponse update(Long id, BrewRequest request, Long ownerId) {
        Brews brew = getEntity(id, ownerId);
        Equipment equipment = findEquipment(request.equipmentId(), ownerId);
        checkGrindSuitsMethod(request.grindSize(), equipment);

        brew.setBean(findBean(request.beanId(), ownerId));
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
    public void delete(Long id, Long ownerId) {
        brewsRepository.delete(getEntity(id, ownerId));
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

    private Brews getEntity(Long id, Long ownerId) {
        return brewsRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Brew", id));
    }

    private Beans findBean(Long id, Long ownerId) {
        return beansRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Bean", id));
    }

    private Equipment findEquipment(Long id, Long ownerId) {
        return equipmentRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
    }
}
