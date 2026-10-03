package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BeanRequest;
import com.hassanhibbert.coffee_tracker.dto.BeanResponse;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.exception.ResourceInUseException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.BeansRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Class-level readOnly transaction for every method; write methods override it.
 * readOnly lets Hibernate skip dirty checking on flush, a small but free win for reads.
 */
@Service
@Transactional(readOnly = true)
public class BeansService {
    private final BeansRepository beansRepository;
    private final BrewsRepository brewsRepository;

    public BeansService(BeansRepository beansRepository, BrewsRepository brewsRepository) {
        this.beansRepository = beansRepository;
        this.brewsRepository = brewsRepository;
    }

    public List<BeanResponse> findAll() {
        return beansRepository.findAll().stream().map(BeanResponse::fromEntity).toList();
    }

    public BeanResponse findById(Long id) {
        return BeanResponse.fromEntity(getEntity(id));
    }

    @Transactional
    public BeanResponse create(BeanRequest request) {
        Beans bean = new Beans(request.name(), request.roastLevel(), request.countryOrigin());
        return BeanResponse.fromEntity(beansRepository.save(bean));
    }

    @Transactional
    public BeanResponse update(Long id, BeanRequest request) {
        Beans bean = getEntity(id);
        bean.setName(request.name());
        bean.setRoastLevel(request.roastLevel());
        bean.setCountryOrigin(request.countryOrigin());
        // No save() call: bean is "managed" by the open persistence context, so Hibernate
        // detects the changed fields (dirty checking) and issues the UPDATE at commit.
        return BeanResponse.fromEntity(bean);
    }

    @Transactional
    public void delete(Long id) {
        Beans bean = getEntity(id);
        if (brewsRepository.existsByBeanId(id)) {
            throw new ResourceInUseException("Bean " + id + " is used by existing brews; delete those brews first");
        }
        beansRepository.delete(bean);
    }

    private Beans getEntity(Long id) {
        return beansRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bean", id));
    }
}
