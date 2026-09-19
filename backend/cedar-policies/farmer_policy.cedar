// ============================================================================
// Cedar Policy: Farmer Access Control (ABAC & RBAC)
// ============================================================================
// Permits users with role "FARMER" to view cold storage units, telemetry readings,
// and alerts ONLY IF the target resource belongs to their assigned farmer ID.
// ============================================================================

permit (
    principal in Role::"FARMER",
    action == Action::"view",
    resource
)
when {
    resource.farmerId == principal.farmerId
};
