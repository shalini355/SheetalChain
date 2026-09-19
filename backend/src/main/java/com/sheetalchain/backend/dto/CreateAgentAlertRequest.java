package com.sheetalchain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating an alert flagged by the independent Strands AI Agent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentAlertRequest {

    private String unitId;
    private String severity;
    private String message;
    private double temperature;
    private String reasoning;
}
