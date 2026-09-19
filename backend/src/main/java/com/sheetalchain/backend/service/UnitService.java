package com.sheetalchain.backend.service;

import com.sheetalchain.backend.dto.CreateUnitRequest;
import com.sheetalchain.backend.model.Unit;
import com.sheetalchain.backend.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service handling business logic for registering and retrieving Cold Storage Units.
 */
@Service
public class UnitService {

    private final UnitRepository unitRepository;

    @Autowired
    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    /**
     * Registers a new micro-cold-storage unit, assigning a random UUID and timestamp.
     */
    public Unit registerUnit(CreateUnitRequest request) {
        Unit unit = Unit.builder()
                .unitId(UUID.randomUUID().toString())
                .farmerId(request.getFarmerId())
                .farmerName(request.getFarmerName())
                .location(request.getLocation())
                .createdAt(System.currentTimeMillis())
                .build();

        return unitRepository.save(unit);
    }

    /**
     * Retrieves all registered cold storage units.
     */
    public List<Unit> getAllUnits() {
        return unitRepository.findAll();
    }

    /**
     * Retrieves details for a specific unit by unitId.
     * Throws HTTP 404 NOT FOUND if unit does not exist.
     */
    public Unit getUnitById(String unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found with id: " + unitId));
    }
}
