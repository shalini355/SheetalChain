// ============================================================================
// Cedar Policy: Manager Access Control (Full RBAC)
// ============================================================================
// Permits users with role "MANAGER" full access across all actions (view, create,
// override) for all cold storage units, telemetry readings, and alerts in the facility.
// ============================================================================

permit (
    principal in Role::"MANAGER",
    action,
    resource
);
