package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a WHERE clause from whichever filters are present. Each Specification is a lambda
 * (root, query, criteriaBuilder) -> Predicate, and the values become bind parameters,
 * so user input never becomes part of the SQL text.
 */
public final class BrewSpecifications {

    private BrewSpecifications() {}

    public static Specification<Brews> matching(Long ownerId, BrewFilter filter) {
        List<Specification<Brews>> specs = new ArrayList<>();
        // Always first, never optional: this is what keeps users inside their own data.
        specs.add((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));

        if (filter.beanId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("bean").get("id"), filter.beanId()));
        }
        if (filter.equipmentId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("equipment").get("id"), filter.equipmentId()));
        }
        if (filter.method() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("equipment").get("method"), filter.method()));
        }
        if (filter.grindSize() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("grindSize"), filter.grindSize()));
        }
        if (filter.minRating() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), filter.minRating()));
        }
        if (filter.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("brewedAt"), filter.from()));
        }
        if (filter.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("brewedAt"), filter.to()));
        }
        return Specification.allOf(specs);
    }
}
