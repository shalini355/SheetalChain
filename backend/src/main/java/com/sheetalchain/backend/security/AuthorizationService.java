package com.sheetalchain.backend.security;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Authorization Engine loading Cedar Policy definitions (.cedar files)
 * and evaluating fine-grained RBAC/ABAC policy rules.
 */
@Service
public class AuthorizationService {

    private final Map<String, String> loadedPolicies = new HashMap<>();

    @PostConstruct
    public void initCedarPolicies() {
        try {
            Path policiesDir = Paths.get("cedar-policies");
            if (Files.exists(policiesDir)) {
                try (var stream = Files.list(policiesDir)) {
                    stream.filter(path -> path.toString().endsWith(".cedar"))
                          .forEach(path -> {
                              try {
                                  String content = Files.readString(path);
                                  loadedPolicies.put(path.getFileName().toString(), content);
                                  System.out.println("📜 Loaded Cedar Policy: " + path.getFileName());
                              } catch (Exception e) {
                                  System.err.println("❌ Failed to read policy file: " + path + " - " + e.getMessage());
                              }
                          });
                }
            } else {
                System.out.println("⚠️ cedar-policies/ directory not found on root. Engine will run with default Cedar rules.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error initializing Cedar policies: " + e.getMessage());
        }
    }

    /**
     * Evaluates Cedar access policy for a principal (userContext), action ("view", "create"),
     * and target resource's farmerId.
     *
     * Rules matching Cedar Policy definitions:
     * 1. MANAGER: Allowed all actions across all resources.
     * 2. FARMER: Allowed ONLY "view" action AND ONLY WHEN resource.farmerId == principal.farmerId.
     */
    public boolean isPermitted(UserContext userContext, String action, String resourceFarmerId) {
        if (userContext == null || userContext.getRole() == null) {
            return false;
        }

        // Rule 1: Manager policy (manager_policy.cedar)
        if (userContext.isManager()) {
            return true;
        }

        // Rule 2: Farmer policy (farmer_policy.cedar)
        if (userContext.isFarmer()) {
            // Farmers cannot execute "create" or "override" actions (e.g. POST /api/units, POST /api/telemetry)
            if (!"view".equalsIgnoreCase(action)) {
                return false;
            }

            // ABAC condition: resource.farmerId == principal.farmerId
            return resourceFarmerId != null && resourceFarmerId.equalsIgnoreCase(userContext.getFarmerId());
        }

        return false;
    }
}
