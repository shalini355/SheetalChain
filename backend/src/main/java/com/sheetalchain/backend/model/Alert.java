package com.sheetalchain.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a system alert generated when cold storage conditions breach safety thresholds.
 * This is a plain Java domain model (POJO) used to represent DynamoDB records in the "Alerts" table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    /**
     * Unique identifier for the alert (UUID generated when alert is triggered).
     * Serves as the Partition Key in DynamoDB.
     */
    private String alertId;

    /**
     * ID of the cold storage unit that triggered the alert.
     */
    private String unitId;

    /**
     * Severity level of the alert ("LOW", "MEDIUM", or "HIGH").
     */
    private String severity;

    /**
     * Human-readable alert description (e.g. "Spoilage risk: temperature exceeded safe threshold").
     */
    private String message;

    /**
     * Temperature reading (°C) that triggered the alert.
     */
    private double temperature;

    /**
     * Epoch timestamp (in milliseconds) when the alert was created.
     */
    private long timestamp;

    /**
     * Source trigger of the alert ("BACKEND_RULE" or "AI_AGENT").
     */
    private String source;

    /**
     * Agent reasoning explanation text (populated when source is "AI_AGENT").
     */
    private String reasoning;
}
