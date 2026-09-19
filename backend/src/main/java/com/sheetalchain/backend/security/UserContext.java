package com.sheetalchain.backend.security;

/**
 * Encapsulates the authenticated principal identity extracted from HTTP headers:
 * - X-User-Role: "FARMER" or "MANAGER"
 * - X-Farmer-Id: Farmer ID (e.g. "FARMER-101")
 */
public class UserContext {

    private final String role;
    private final String farmerId;

    public UserContext(String role, String farmerId) {
        this.role = role;
        this.farmerId = farmerId;
    }

    public String getRole() {
        return role;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }

    public boolean isFarmer() {
        return "FARMER".equalsIgnoreCase(role);
    }
}
