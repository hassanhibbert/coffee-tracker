package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.entity.Brews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrewsRepository extends JpaRepository<Brews, Long> {
}
