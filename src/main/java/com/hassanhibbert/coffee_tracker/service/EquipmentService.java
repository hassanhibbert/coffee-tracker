package com.hassanhibbert.coffee_tracker.service;

import com.hassanhibbert.coffee_tracker.dto.EquipmentRequest;
import com.hassanhibbert.coffee_tracker.dto.EquipmentResponse;
import com.hassanhibbert.coffee_tracker.entity.Equipment;
import com.hassanhibbert.coffee_tracker.exception.ResourceInUseException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
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

    public EquipmentService(EquipmentRepository equipmentRepository, BrewsRepository brewsRepository) {
        this.equipmentRepository = equipmentRepository;
        this.brewsRepository = brewsRepository;
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll().stream().map(EquipmentResponse::fromEntity).toList();
    }

    public EquipmentResponse findById(Long id) {
        return EquipmentResponse.fromEntity(getEntity(id));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        Equipment equipment = new Equipment(request.name(), request.method(), request.brand());
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = getEntity(id);
        equipment.setName(request.name());
        equipment.setMethod(request.method());
        equipment.setBrand(request.brand());
        return EquipmentResponse.fromEntity(equipment);
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = getEntity(id);
        if (brewsRepository.existsByEquipmentId(id)) {
            throw new ResourceInUseException("Equipment " + id + " is used by existing brews; delete those brews first");
        }
        equipmentRepository.delete(equipment);
    }

    private Equipment getEntity(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
    }
}
