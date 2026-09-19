package com.sheetalchain.backend.controller;

import com.sheetalchain.backend.dto.CreateAgentAlertRequest;
import com.sheetalchain.backend.model.Alert;
import com.sheetalchain.backend.model.Unit;
import com.sheetalchain.backend.security.AuthorizationService;
import com.sheetalchain.backend.security.SecurityInterceptor;
import com.sheetalchain.backend.security.UserContext;
import com.sheetalchain.backend.service.AlertService;
import com.sheetalchain.backend.service.UnitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for retrieving system alerts and receiving agent alerts with Cedar ABAC authorization.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final UnitService unitService;
    private final AuthorizationService authorizationService;

    @Autowired
    public AlertController(AlertService alertService,
                           UnitService unitService,
                           AuthorizationService authorizationService) {
        this.alertService = alertService;
        this.unitService = unitService;
        this.authorizationService = authorizationService;
    }

    /**
     * GET /api/alerts - List all alerts across all units (MANAGER-only).
     * Handled by SecurityInterceptor.
     */
    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    /**
     * POST /api/alerts/agent-flag - Record an alert flagged by the Strands AI Monitoring Agent (MANAGER-only).
     */
    @PostMapping("/agent-flag")
    public ResponseEntity<Alert> createAgentAlert(@Valid @RequestBody CreateAgentAlertRequest request) {
        Alert createdAlert = alertService.createAgentAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAlert);
    }

    /**
     * GET /api/alerts/{unitId} - List alerts for a specific unit.
     * Enforces Cedar ABAC ownership check (resource.farmerId == principal.farmerId).
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<List<Alert>> getAlertsForUnit(@PathVariable String unitId, HttpServletRequest request) {
        UserContext userContext = (UserContext) request.getAttribute(SecurityInterceptor.ATTRIBUTE_USER_CONTEXT);
        Unit unit = unitService.getUnitById(unitId);

        if (!authorizationService.isPermitted(userContext, "view", unit.getFarmerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to view alerts for this unit");
        }

        return ResponseEntity.ok(alertService.getAlertsForUnit(unitId));
    }
}
