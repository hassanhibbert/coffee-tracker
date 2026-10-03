package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Beans;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Every lookup is scoped by owner. findById(id) alone would let any logged-in user read
 * anyone's bean just by guessing ids (IDOR), so services never call it.
 */
public interface BeansRepository extends JpaRepository<Beans, Long> {

    List<Beans> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Beans> findByIdAndOwnerId(Long id, Long ownerId);
}
