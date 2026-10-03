package com.hassanhibbert.coffee_tracker.repository;

import com.hassanhibbert.coffee_tracker.TestcontainersConfiguration;
import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.dto.stats.BeanStat;
import com.hassanhibbert.coffee_tracker.dto.stats.MethodStat;
import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.entity.RoastLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository slice against real Postgres (Testcontainers), with the schema built by the
 * real Flyway migrations. This is where SQL bugs live: a mocked repository would happily
 * "return" results that the actual query could never produce.
 *
 * Each test runs in a transaction that is rolled back afterwards, so tests don't leak data.
 * Skipped (not failed) on machines without Docker; CI always has Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class BrewsRepositoryTest {

    @Autowired AppUserRepository users;
    @Autowired BeansRepository beans;
    @Autowired EquipmentRepository equipment;
    @Autowired BrewsRepository brews;

    private AppUser alice;
    private AppUser bob;
    private Beans kenya;
    private Beans robusta;
    private Equipment v60;
    private Equipment phin;

    @BeforeEach
    void seed() {
        alice = users.save(new AppUser("alice", "{noop}x"));
        bob = users.save(new AppUser("bob", "{noop}x"));
        kenya = beans.save(new Beans(alice, "Kenya AA", RoastLevel.LIGHT, "Kenya"));
        robusta = beans.save(new Beans(alice, "Dak Lak Robusta", RoastLevel.DARK, "Vietnam"));
        v60 = equipment.save(new Equipment(alice, "V60", BrewMethod.POUR_OVER, "Hario"));
        phin = equipment.save(new Equipment(alice, "Phin", BrewMethod.PHIN, null));

        brew(alice, kenya, v60, GrindSize.MEDIUM, 3, "2026-09-01T08:00:00Z");
        brew(alice, kenya, v60, GrindSize.MEDIUM, 5, "2026-09-02T08:00:00Z");
        brew(alice, robusta, phin, GrindSize.FINE, 4, "2026-09-03T08:00:00Z");
        brew(alice, robusta, phin, GrindSize.FINE, null, "2026-09-04T08:00:00Z");

        Beans bobsBean = beans.save(new Beans(bob, "Bob's Brazil", RoastLevel.MEDIUM, "Brazil"));
        Equipment bobsPress = equipment.save(new Equipment(bob, "Press", BrewMethod.FRENCH_PRESS, null));
        brew(bob, bobsBean, bobsPress, GrindSize.COARSE, 5, "2026-09-05T08:00:00Z");
    }

    @Test
    void specification_neverReturnsAnotherUsersBrews() {
        Page<Brews> page = brews.findAll(BrewSpecifications.matching(alice.getId(), noFilter()), PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).allSatisfy(b -> assertThat(b.getOwner().getId()).isEqualTo(alice.getId()));
    }

    @Test
    void specification_combinesFiltersWithAnd() {
        BrewFilter filter = new BrewFilter(null, null, BrewMethod.POUR_OVER, null, 4, null, null);

        List<Brews> result = brews.findAll(BrewSpecifications.matching(alice.getId(), filter),
                PageRequest.of(0, 50)).getContent();

        assertThat(result).singleElement().satisfies(b -> assertThat(b.getRating()).isEqualTo(5));
    }

    @Test
    void specification_dateRangeIsInclusiveFromExclusiveTo() {
        BrewFilter filter = new BrewFilter(null, null, null, null, null,
                Instant.parse("2026-09-02T08:00:00Z"), Instant.parse("2026-09-04T08:00:00Z"));

        List<Brews> result = brews.findAll(BrewSpecifications.matching(alice.getId(), filter),
                PageRequest.of(0, 50, Sort.by("brewedAt"))).getContent();

        assertThat(result).extracting(Brews::getBrewedAt).containsExactly(
                Instant.parse("2026-09-02T08:00:00Z"), Instant.parse("2026-09-03T08:00:00Z"));
    }

    @Test
    void statsByMethod_groupsCountsAndAveragesIgnoringUnratedBrews() {
        List<MethodStat> stats = brews.statsByMethod(alice.getId());

        // AVG skips NULLs: the phin average is 4.0 from one rated brew, not 2.0 over two.
        assertThat(stats).containsExactlyInAnyOrder(
                new MethodStat(BrewMethod.POUR_OVER, 2L, 4.0),
                new MethodStat(BrewMethod.PHIN, 2L, 4.0));
    }

    @Test
    void topBeans_ordersByAverageRatingThenCount() {
        List<BeanStat> top = brews.topBeans(alice.getId(), Limit.of(5));

        assertThat(top).extracting(BeanStat::name).containsExactly("Kenya AA", "Dak Lak Robusta");
        assertThat(top.get(1).averageRating()).isEqualTo(4.0);
    }

    @Test
    void checkConstraint_isTheBackstopWhenValidationIsBypassed() {
        // Validation lives on BrewRequest; going straight to the repository skips it.
        // The V1 CHECK (water_grams > 0) still refuses the row.
        Brews invalid = new Brews(alice, kenya, v60, GrindSize.MEDIUM, new BigDecimal("15"),
                new BigDecimal("-1"), 94, 180, null, null, Instant.now());

        assertThatThrownBy(() -> brews.saveAndFlush(invalid)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void brew(AppUser owner, Beans bean, Equipment eq, GrindSize grind, Integer rating, String at) {
        brews.save(new Brews(owner, bean, eq, grind, new BigDecimal("15"), new BigDecimal("250"),
                94, 180, rating, null, Instant.parse(at)));
    }

    private static BrewFilter noFilter() {
        return new BrewFilter(null, null, null, null, null, null, null);
    }
}
