package com.sheetalchain.backend.controller;

import com.sheetalchain.backend.dto.CreateTelemetryRequest;
import com.sheetalchain.backend.model.Telemetry;
import com.sheetalchain.backend.model.Unit;
import com.sheetalchain.backend.security.AuthorizationService;
import com.sheetalchain.backend.security.SecurityInterceptor;
import com.sheetalchain.backend.security.UserContext;
import com.sheetalchain.backend.service.TelemetryService;
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
 * REST Controller for ingesting and retrieving telemetry readings with Cedar ABAC security.
 */
@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final UnitService unitService;
    private final AuthorizationService authorizationService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService,
                               UnitService unitService,
                               AuthorizationService authorizationService) {
        this.telemetryService = telemetryService;
        this.unitService = unitService;
        this.authorizationService = authorizationService;
    }

    /**
     * POST /api/telemetry - Ingest a sensor telemetry reading. (MANAGER-only)
     */
    @PostMapping
    public ResponseEntity<Telemetry> ingestTelemetry(@Valid @RequestBody CreateTelemetryRequest request) {
        Telemetry telemetry = telemetryService.ingestTelemetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetry);
    }

    /**
     * GET /api/telemetry/{unitId} - Get latest 20 telemetry readings for a specific unit.
     * Enforces Cedar ABAC ownership check (resource.farmerId == principal.farmerId).
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<List<Telemetry>> getLatestTelemetryForUnit(@PathVariable String unitId, HttpServletRequest request) {
        UserContext userContext = (UserContext) request.getAttribute(SecurityInterceptor.ATTRIBUTE_USER_CONTEXT);
        Unit unit = unitService.getUnitById(unitId);

        if (!authorizationService.isPermitted(userContext, "view", unit.getFarmerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to view telemetry for this unit");
        }

        return ResponseEntity.ok(telemetryService.getLatestTelemetryForUnit(unitId));
    }
}
