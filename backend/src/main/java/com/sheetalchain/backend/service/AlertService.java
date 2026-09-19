package com.sheetalchain.backend.service;

import com.sheetalchain.backend.dto.CreateAgentAlertRequest;
import com.sheetalchain.backend.model.Alert;
import com.sheetalchain.backend.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service handling alert retrieval and agent-flagged alert ingestion.
 */
@Service
public class AlertService {

    private final AlertRepository alertRepository;

    @Autowired
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Lists all alerts across all cold storage units (used by global dashboard alert feed).
     */
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    /**
     * Lists alerts for a specific unit (used by farmer-specific view).
     */
    public List<Alert> getAlertsForUnit(String unitId) {
        return alertRepository.findByUnitId(unitId);
    }

    /**
     * Creates an alert flagged by the independent Strands AI Agent.
     */
    public Alert createAgentAlert(CreateAgentAlertRequest request) {
        long currentTime = System.currentTimeMillis();
        Alert alert = Alert.builder()
                .alertId(UUID.randomUUID().toString())
                .unitId(request.getUnitId())
                .severity(request.getSeverity() != null ? request.getSeverity() : "CRITICAL")
                .message(request.getMessage() != null ? request.getMessage() : "AI Agent spoilage risk alert")
                .temperature(request.getTemperature())
                .timestamp(currentTime)
                .source("AI_AGENT")
                .reasoning(request.getReasoning())
                .build();

        Alert savedAlert = alertRepository.save(alert);
        System.out.println("🤖 AI AGENT ALERT LOGGED for Unit: " + request.getUnitId() + " (Severity: " + request.getSeverity() + ")");
        return savedAlert;
    }
}
