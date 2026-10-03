package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Brews;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrewsRepository extends JpaRepository<Brews, Long> {

    // Spring Data derives the query from the method name. @EntityGraph adds a JOIN FETCH
    // for bean and equipment, so mapping N brews to DTOs costs 1 query instead of 1 + 2N.
    @EntityGraph(attributePaths = {"bean", "equipment"})
    List<Brews> findAllByOwnerIdOrderByBrewedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = {"bean", "equipment"})
    Optional<Brews> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByBeanId(Long beanId);

    boolean existsByEquipmentId(Long equipmentId);
}
