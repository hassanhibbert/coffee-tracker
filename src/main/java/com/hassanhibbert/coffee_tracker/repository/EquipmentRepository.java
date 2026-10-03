package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Equipment> findByIdAndOwnerId(Long id, Long ownerId);
}
