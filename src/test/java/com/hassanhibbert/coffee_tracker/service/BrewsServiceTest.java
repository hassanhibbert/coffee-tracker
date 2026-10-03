package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.Brews;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.entity.RoastLevel;
import com.hassanhibbert.coffee_tracker.exception.BrewRuleViolationException;
import com.hassanhibbert.coffee_tracker.exception.InvalidQueryException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no database, milliseconds to run. Repositories are
 * mocked because what's under test here is the service's *decisions*, not SQL.
 * The SQL itself is tested against real Postgres in BrewsRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
class BrewsServiceTest {
    private static final Long OWNER = 1L;

    @Mock BrewsRepository brewsRepository;
    @Mock BeansRepository beansRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock AppUserRepository appUserRepository;
    @InjectMocks BrewsService brewsService;

    private final AppUser owner = new AppUser("alice", "{noop}x");
    private Beans bean;

    @BeforeEach
    void setUp() {
        bean = new Beans(owner, "Kenya AA", RoastLevel.LIGHT, "Kenya");
    }

    @Test
    void create_rejectsCoarseGrindForEspresso() {
        Equipment espresso = new Equipment(owner, "Gaggia", BrewMethod.ESPRESSO, "Gaggia");
        when(beansRepository.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.of(bean));
        when(equipmentRepository.findByIdAndOwnerId(20L, OWNER)).thenReturn(Optional.of(espresso));

        assertThatThrownBy(() -> brewsService.create(request(GrindSize.COARSE, null), OWNER))
                .isInstanceOf(BrewRuleViolationException.class)
                .hasMessageContaining("COARSE does not suit ESPRESSO");
        verify(brewsRepository, never()).save(any());
    }

    @Test
    void create_withBeanOfAnotherUser_isNotFoundAndSavesNothing() {
        // The repository is asked "bean 10 owned by user 1?" and says no: that's the IDOR guard.
        when(beansRepository.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brewsService.create(request(GrindSize.FINE, null), OWNER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Bean with id 10 was not found");
        verify(brewsRepository, never()).save(any());
    }

    @Test
    void create_defaultsBrewedAtToNowAndComputesRatio() {
        Equipment phin = new Equipment(owner, "Phin", BrewMethod.PHIN, null);
        when(beansRepository.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.of(bean));
        when(equipmentRepository.findByIdAndOwnerId(20L, OWNER)).thenReturn(Optional.of(phin));
        when(appUserRepository.getReferenceById(OWNER)).thenReturn(owner);
        when(brewsRepository.save(any(Brews.class))).thenAnswer(call -> call.getArgument(0));

        Instant before = Instant.now();
        BrewResponse response = brewsService.create(request(GrindSize.FINE, null), OWNER);

        assertThat(response.brewedAt()).isBetween(before, Instant.now());
        assertThat(response.ratio()).isEqualTo("1:5.0");
        assertThat(response.equipment().method()).isEqualTo(BrewMethod.PHIN);
    }

    @Test
    void search_rejectsSortOnUnlistedProperty() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("owner.passwordHash"));

        assertThatThrownBy(() -> brewsService.search(OWNER, emptyFilter(), pageable))
                .isInstanceOf(InvalidQueryException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_appendsIdTieBreakerToRequestedSort() {
        when(brewsRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        brewsService.search(OWNER, emptyFilter(), PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "rating")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(brewsRepository).findAll(any(Specification.class), captor.capture());
        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2);
        assertThat(used.getSort().toList()).containsExactly(
                Sort.Order.desc("rating"), Sort.Order.desc("id"));
    }

    private static BrewRequest request(GrindSize grind, Instant brewedAt) {
        return new BrewRequest(10L, 20L, grind, new BigDecimal("20"), new BigDecimal("100"),
                96, 300, 4, null, brewedAt);
    }

    private static BrewFilter emptyFilter() {
        return new BrewFilter(null, null, null, null, null, null, null);
    }
}
