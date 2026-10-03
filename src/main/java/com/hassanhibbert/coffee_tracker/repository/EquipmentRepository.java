package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
