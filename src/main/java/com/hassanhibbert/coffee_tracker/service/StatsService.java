package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.stats.StatsResponse;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {
    private static final int TOP_BEANS = 5;

    private final BrewsRepository brewsRepository;

    public StatsService(BrewsRepository brewsRepository) {
        this.brewsRepository = brewsRepository;
    }

    public StatsResponse summary(Long ownerId) {
        return new StatsResponse(
                brewsRepository.countByOwnerId(ownerId),
                brewsRepository.averageRating(ownerId),
                brewsRepository.statsByMethod(ownerId),
                brewsRepository.topBeans(ownerId, Limit.of(TOP_BEANS)));
    }
}
