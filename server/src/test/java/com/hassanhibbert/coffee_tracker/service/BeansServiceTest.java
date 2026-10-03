package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.entity.RoastLevel;
import com.hassanhibbert.coffee_tracker.exception.ResourceInUseException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeansServiceTest {
    @Mock BeansRepository beansRepository;
    @Mock BrewsRepository brewsRepository;
    @Mock AppUserRepository appUserRepository;
    @InjectMocks BeansService beansService;

    @Test
    void delete_refusesWhileBrewsReferenceTheBean() {
        Beans bean = new Beans(new AppUser("alice", "{noop}x"), "Kenya AA", RoastLevel.LIGHT, "Kenya");
        when(beansRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(bean));
        when(brewsRepository.existsByBeanId(5L)).thenReturn(true);

        assertThatThrownBy(() -> beansService.delete(5L, 1L)).isInstanceOf(ResourceInUseException.class);
        verify(beansRepository, never()).delete(any());
    }

    @Test
    void findById_ofAnotherUsersBean_isNotFound() {
        when(beansRepository.findByIdAndOwnerId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beansService.findById(5L, 2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
