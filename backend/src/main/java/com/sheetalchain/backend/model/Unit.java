package com.sheetalchain.backend.model;

/**
 * Represents a Micro-Cold-Storage Unit registered by a farmer.
 * This is a plain Java domain model (POJO) used to represent DynamoDB records in the "Units" table.
 */
public class Unit {

    /**
     * Unique identifier for the cold storage unit (UUID generated at creation).
     * Serves as the Partition Key in DynamoDB.
     */
    private String unitId;

    /**
     * ID of the farmer owning/using this unit (e.g. "FARMER-101").
     */
    private String farmerId;

    /**
     * Full name of the farmer (e.g. "Ramesh Kumar").
     */
    private String farmerName;

    /**
     * Location or district of the cold storage (e.g. "Nashik, Maharashtra").
     */
    private String location;

    /**
     * Epoch timestamp (in milliseconds) when the unit was registered.
     */
    private long createdAt;

    public String getUnitId() {
        return unitId;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getLocation() {
        return location;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Unit unit = new Unit();

        public Builder unitId(String unitId) {
            unit.unitId = unitId;
            return this;
        }

        public Builder farmerId(String farmerId) {
            unit.farmerId = farmerId;
            return this;
        }

        public Builder farmerName(String farmerName) {
            unit.farmerName = farmerName;
            return this;
        }

        public Builder location(String location) {
            unit.location = location;
            return this;
        }

        public Builder createdAt(long createdAt) {
            unit.createdAt = createdAt;
            return this;
        }

        public Unit build() {
            return unit;
        }
    }
}
