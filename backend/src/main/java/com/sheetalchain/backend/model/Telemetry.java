package com.sheetalchain.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a sensor reading ingested from a cold storage unit.
 * This is a plain Java domain model (POJO) used to represent DynamoDB records in the "Telemetry" table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Telemetry {

    /**
     * Unique identifier for the telemetry entry (UUID generated at ingestion).
     * Serves as the Partition Key in DynamoDB.
     */
    private String telemetryId;

    /**
     * ID of the unit sending the reading (foreign reference to Unit.unitId).
     */
    private String unitId;

    /**
     * Temperature reading in Celsius (e.g. 4.5°C). Safe threshold <= 8.0°C.
     */
    private double temperature;

    /**
     * Relative humidity percentage (e.g. 85.0%).
     */
    private double humidity;

    /**
     * Latitude coordinate of the unit's GPS location.
     */
    private double latitude;

    /**
     * Longitude coordinate of the unit's GPS location.
     */
    private double longitude;

    /**
     * Epoch timestamp (in milliseconds) when the reading was recorded.
     */
    private long timestamp;
}
