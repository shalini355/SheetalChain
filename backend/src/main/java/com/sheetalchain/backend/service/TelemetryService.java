package com.sheetalchain.backend.service;

import com.sheetalchain.backend.dto.CreateTelemetryRequest;
import com.sheetalchain.backend.model.Alert;
import com.sheetalchain.backend.model.Telemetry;
import com.sheetalchain.backend.repository.AlertRepository;
import com.sheetalchain.backend.repository.TelemetryRepository;
import com.sheetalchain.backend.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service handling telemetry ingestion and threshold monitoring.
 */
@Service
public class TelemetryService {

    private static final double TEMPERATURE_SAFE_THRESHOLD = 8.0;

    private final TelemetryRepository telemetryRepository;
    private final AlertRepository alertRepository;
    private final UnitRepository unitRepository;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository,
                            AlertRepository alertRepository,
                            UnitRepository unitRepository) {
        this.telemetryRepository = telemetryRepository;
        this.alertRepository = alertRepository;
        this.unitRepository = unitRepository;
    }

    /**
     * Ingests a new telemetry sensor reading from a unit.
     * Evaluates temperature safety threshold (> 8.0°C) and generates an Alert if breached.
     */
    public Telemetry ingestTelemetry(CreateTelemetryRequest request) {
        // Optional validation: check if unitId actually exists
        unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found with id: " + request.getUnitId()));

        long currentTime = System.currentTimeMillis();

        Telemetry telemetry = Telemetry.builder()
                .telemetryId(UUID.randomUUID().toString())
                .unitId(request.getUnitId())
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timestamp(currentTime)
                .build();

        Telemetry savedTelemetry = telemetryRepository.save(telemetry);

        // CORE BUSINESS RULE: Check for spoilage risk threshold breach (> 8.0°C)
        if (request.getTemperature() > TEMPERATURE_SAFE_THRESHOLD) {
            Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .unitId(request.getUnitId())
                    .severity("HIGH")
                    .message("Spoilage risk: temperature exceeded safe threshold (" + request.getTemperature() + "°C)")
                    .temperature(request.getTemperature())
                    .timestamp(currentTime)
                    .source("BACKEND_RULE")
                    .build();

            alertRepository.save(alert);
            System.out.println("⚠️ HIGH SEVERITY ALERT TRIGGERED for Unit: " + request.getUnitId() + " (Temp: " + request.getTemperature() + "°C)");
        }

        return savedTelemetry;
    }

    /**
     * Retrieves the latest 20 telemetry readings for a specific unit (sorted descending by timestamp).
     */
    public List<Telemetry> getLatestTelemetryForUnit(String unitId) {
        return telemetryRepository.findLatestByUnitId(unitId, 20);
    }
}
