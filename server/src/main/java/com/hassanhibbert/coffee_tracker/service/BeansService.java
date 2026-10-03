package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.BeanRequest;
import com.hassanhibbert.coffee_tracker.dto.BeanResponse;
import com.hassanhibbert.coffee_tracker.entity.Beans;
import com.hassanhibbert.coffee_tracker.exception.ResourceInUseException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;

    public BeansService(BeansRepository beansRepository, BrewsRepository brewsRepository,
                        AppUserRepository appUserRepository) {
        this.beansRepository = beansRepository;
        this.brewsRepository = brewsRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<BeanResponse> findAll(Long ownerId) {
        return beansRepository.findAllByOwnerIdOrderByNameAsc(ownerId).stream().map(BeanResponse::fromEntity).toList();
    }

    public BeanResponse findById(Long id, Long ownerId) {
        return BeanResponse.fromEntity(getEntity(id, ownerId));
    }

    @Transactional
    public BeanResponse create(BeanRequest request, Long ownerId) {
        // getReferenceById returns a lazy proxy without a SELECT: we only need the id
        // to fill owner_id, not the user's row.
        Beans bean = new Beans(appUserRepository.getReferenceById(ownerId), request.name(), request.roastLevel(), request.countryOrigin());
        return BeanResponse.fromEntity(beansRepository.save(bean));
    }

    @Transactional
    public BeanResponse update(Long id, BeanRequest request, Long ownerId) {
        Beans bean = getEntity(id, ownerId);
        bean.setName(request.name());
        bean.setRoastLevel(request.roastLevel());
        bean.setCountryOrigin(request.countryOrigin());
        // No save() call: bean is "managed" by the open persistence context, so Hibernate
        // detects the changed fields (dirty checking) and issues the UPDATE at commit.
        return BeanResponse.fromEntity(bean);
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        Beans bean = getEntity(id, ownerId);
        if (brewsRepository.existsByBeanId(id)) {
            throw new ResourceInUseException("Bean " + id + " is used by existing brews; delete those brews first");
        }
        beansRepository.delete(bean);
    }

    /** Someone else's bean is reported as 404, not 403: don't confirm that the id exists. */
    private Beans getEntity(Long id, Long ownerId) {
        return beansRepository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ResourceNotFoundException("Bean", id));
    }
}
