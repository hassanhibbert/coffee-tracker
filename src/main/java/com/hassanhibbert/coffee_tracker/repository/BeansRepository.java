package com.hassanhibbert.coffee_tracker.repository;


import com.hassanhibbert.coffee_tracker.entity.Beans;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeansRepository extends JpaRepository<Beans, Long> {
}