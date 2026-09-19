package com.sheetalchain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for registering a new cold storage unit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnitRequest {

    @NotBlank(message = "Farmer ID cannot be blank")
    private String farmerId;

    @NotBlank(message = "Farmer Name cannot be blank")
    private String farmerName;

    @NotBlank(message = "Location cannot be blank")
    private String location;
}
