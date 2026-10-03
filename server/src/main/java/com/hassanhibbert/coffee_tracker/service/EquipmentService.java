package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.EquipmentRequest;
import com.hassanhibbert.coffee_tracker.dto.EquipmentResponse;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.exception.ResourceInUseException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final BrewsRepository brewsRepository;
    private final AppUserRepository appUserRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, BrewsRepository brewsRepository,
                            AppUserRepository appUserRepository) {
        this.equipmentRepository = equipmentRepository;
        this.brewsRepository = brewsRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<EquipmentResponse> findAll(Long ownerId) {
        return equipmentRepository.findAllByOwnerIdOrderByNameAsc(ownerId).stream().map(EquipmentResponse::fromEntity).toList();
    }

    public EquipmentResponse findById(Long id, Long ownerId) {
        return EquipmentResponse.fromEntity(getEntity(id, ownerId));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request, Long ownerId) {
        Equipment equipment = new Equipment(appUserRepository.getReferenceById(ownerId), request.name(), request.method(), request.brand());
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request, Long ownerId) {
        Equipment equipment = getEntity(id, ownerId);
        equipment.setName(request.name());
        equipment.setMethod(request.method());
        equipment.setBrand(request.brand());
        return EquipmentResponse.fromEntity(equipment);
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        Equipment equipment = getEntity(id, ownerId);
        if (brewsRepository.existsByEquipmentId(id)) {
            throw new ResourceInUseException("Equipment " + id + " is used by existing brews; delete those brews first");
        }
        equipmentRepository.delete(equipment);
    }

    private Equipment getEntity(Long id, Long ownerId) {
        return equipmentRepository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
    }
}
