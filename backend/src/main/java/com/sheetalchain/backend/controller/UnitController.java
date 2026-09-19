package com.sheetalchain.backend.controller;

import com.sheetalchain.backend.dto.CreateUnitRequest;
import com.sheetalchain.backend.model.Unit;
import com.sheetalchain.backend.security.AuthorizationService;
import com.sheetalchain.backend.security.SecurityInterceptor;
import com.sheetalchain.backend.security.UserContext;
import com.sheetalchain.backend.service.UnitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing Micro-Cold-Storage Units with Cedar authorization.
 */
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService unitService;
    private final AuthorizationService authorizationService;

    @Autowired
    public UnitController(UnitService unitService, AuthorizationService authorizationService) {
        this.unitService = unitService;
        this.authorizationService = authorizationService;
    }

    /**
     * POST /api/units - Register a new cold storage unit. (MANAGER-only)
     */
    @PostMapping
    public ResponseEntity<Unit> createUnit(@Valid @RequestBody CreateUnitRequest request) {
        Unit createdUnit = unitService.registerUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    /**
     * GET /api/units - List units.
     * MANAGER sees all units. FARMER sees only units registered to their farmerId.
     */
    @GetMapping
    public ResponseEntity<List<Unit>> getAllUnits(HttpServletRequest request) {
        UserContext userContext = (UserContext) request.getAttribute(SecurityInterceptor.ATTRIBUTE_USER_CONTEXT);
        List<Unit> allUnits = unitService.getAllUnits();

        if (userContext != null && userContext.isFarmer()) {
            List<Unit> farmerUnits = allUnits.stream()
                    .filter(u -> authorizationService.isPermitted(userContext, "view", u.getFarmerId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(farmerUnits);
        }

        return ResponseEntity.ok(allUnits);
    }

    /**
     * GET /api/units/{unitId} - Get details of a specific unit by unitId.
     * Validates Cedar ABAC ownership policy (resource.farmerId == principal.farmerId).
     */
    @GetMapping("/{unitId}")
    public ResponseEntity<Unit> getUnitById(@PathVariable String unitId, HttpServletRequest request) {
        UserContext userContext = (UserContext) request.getAttribute(SecurityInterceptor.ATTRIBUTE_USER_CONTEXT);
        Unit unit = unitService.getUnitById(unitId);

        if (!authorizationService.isPermitted(userContext, "view", unit.getFarmerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to view this cold storage unit");
        }

        return ResponseEntity.ok(unit);
    }
}
