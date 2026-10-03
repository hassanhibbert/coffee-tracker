package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.dto.stats.BeanStat;
import com.hassanhibbert.coffee_tracker.dto.stats.MethodStat;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrewsRepository extends JpaRepository<Brews, Long>, JpaSpecificationExecutor<Brews> {

    // Redeclared only to attach @EntityGraph: fetching bean and equipment in the same query
    // means mapping a page of N brews to DTOs costs 1 query instead of 1 + 2N.
    @Override
    @EntityGraph(attributePaths = {"bean", "equipment"})
    Page<Brews> findAll(Specification<Brews> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"bean", "equipment"})
    Optional<Brews> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    // JPQL works on entities and fields (Brews, b.owner.id), not tables and columns.
    // ":ownerId" is a named bind parameter, never string-concatenated: no SQL injection.
    @Query("select avg(b.rating) from Brews b where b.owner.id = :ownerId")
    Double averageRating(@Param("ownerId") Long ownerId);

    // "select new ..." is a constructor expression: the database does the grouping and
    // Hibernate builds each record directly, without loading a single Brews entity.
    @Query("""
            select new com.hassanhibbert.coffee_tracker.dto.stats.MethodStat(e.method, count(b), avg(b.rating))
            from Brews b join b.equipment e
            where b.owner.id = :ownerId
            group by e.method
            order by count(b) desc
            """)
    List<MethodStat> statsByMethod(@Param("ownerId") Long ownerId);

    @Query("""
            select new com.hassanhibbert.coffee_tracker.dto.stats.BeanStat(bn.id, bn.name, count(b), avg(b.rating))
            from Brews b join b.bean bn
            where b.owner.id = :ownerId
            group by bn.id, bn.name
            order by avg(b.rating) desc nulls last, count(b) desc
            """)
    List<BeanStat> topBeans(@Param("ownerId") Long ownerId, Limit limit);

    boolean existsByBeanId(Long beanId);

    boolean existsByEquipmentId(Long equipmentId);
}
