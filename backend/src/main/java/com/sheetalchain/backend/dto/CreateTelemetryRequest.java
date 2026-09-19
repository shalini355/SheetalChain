package com.sheetalchain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for ingesting telemetry reading from a cold storage unit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTelemetryRequest {

    @NotBlank(message = "Unit ID cannot be blank")
    private String unitId;

    @NotNull(message = "Temperature is required")
    private Double temperature;

    @NotNull(message = "Humidity is required")
    private Double humidity;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
