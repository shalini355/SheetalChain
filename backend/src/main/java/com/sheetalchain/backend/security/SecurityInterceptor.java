package com.sheetalchain.backend.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring HandlerInterceptor extracting identity headers (X-User-Role, X-Farmer-Id)
 * and enforcing high-level role authorization.
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private static final String HEADER_ROLE = "X-User-Role";
    private static final String HEADER_FARMER_ID = "X-Farmer-Id";
    public static final String ATTRIBUTE_USER_CONTEXT = "userContext";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = request.getHeader(HEADER_ROLE);
        String farmerId = request.getHeader(HEADER_FARMER_ID);

        UserContext userContext = new UserContext(
            role != null ? role.trim().toUpperCase() : null,
            farmerId != null ? farmerId.trim() : null
        );

        // Store UserContext in request scope for controller access
        request.setAttribute(ATTRIBUTE_USER_CONTEXT, userContext);

        if (!userContext.isManager() && !userContext.isFarmer()) {
            return sendUnauthorized(response, "Authentication required: X-User-Role must be FARMER or MANAGER");
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. POST /api/units is MANAGER-only
        if ("POST".equalsIgnoreCase(method) && uri.equalsIgnoreCase("/api/units")) {
            if (!userContext.isManager()) {
                return sendForbidden(response, "Access Denied: Unit registration is restricted to MANAGER role");
            }
        }

        // 2. POST /api/telemetry is MANAGER-only (IoT / Manager ingestion)
        if ("POST".equalsIgnoreCase(method) && uri.equalsIgnoreCase("/api/telemetry")) {
            if (!userContext.isManager()) {
                return sendForbidden(response, "Access Denied: Telemetry submission is restricted to MANAGER role");
            }
        }

        // 3. GET /api/alerts (global alert feed) is MANAGER-only
        if ("GET".equalsIgnoreCase(method) && uri.equalsIgnoreCase("/api/alerts")) {
            if (!userContext.isManager()) {
                return sendForbidden(response, "Access Denied: Farmers must view unit-specific alerts via /api/alerts/{unitId}");
            }
        }

        // 4. POST /api/alerts/agent-flag is MANAGER-only
        if ("POST".equalsIgnoreCase(method) && uri.equalsIgnoreCase("/api/alerts/agent-flag")) {
            if (!userContext.isManager()) {
                return sendForbidden(response, "Access Denied: AI agent alert creation is restricted to MANAGER role");
            }
        }

        return true;
    }

    private boolean sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", 403);
        errorBody.put("error", "Forbidden");
        errorBody.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        return false;
    }

    private boolean sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", 401);
        errorBody.put("error", "Unauthorized");
        errorBody.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        return false;
    }
}
