package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Brews;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrewsRepository extends JpaRepository<Brews, Long> {

    // Spring Data derives the query from the method name. @EntityGraph adds a JOIN FETCH
    // for bean and equipment, so mapping N brews to DTOs costs 1 query instead of 1 + 2N.
    @EntityGraph(attributePaths = {"bean", "equipment"})
    List<Brews> findAllByOrderByBrewedAtDesc();

    boolean existsByBeanId(Long beanId);

    boolean existsByEquipmentId(Long equipmentId);
}
