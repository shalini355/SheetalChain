package com.sheetalchain.backend.repository;

import com.sheetalchain.backend.model.Telemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO Repository for managing 'Telemetry' records in DynamoDB "Telemetry" table.
 */
@Repository
public class TelemetryRepository {

    private static final String TABLE_NAME = "Telemetry";
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public TelemetryRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Saves a Telemetry entry in DynamoDB.
     */
    public Telemetry save(Telemetry telemetry) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("telemetryId", AttributeValue.builder().s(telemetry.getTelemetryId()).build());
        item.put("unitId", AttributeValue.builder().s(telemetry.getUnitId()).build());
        item.put("temperature", AttributeValue.builder().n(String.valueOf(telemetry.getTemperature())).build());
        item.put("humidity", AttributeValue.builder().n(String.valueOf(telemetry.getHumidity())).build());
        item.put("latitude", AttributeValue.builder().n(String.valueOf(telemetry.getLatitude())).build());
        item.put("longitude", AttributeValue.builder().n(String.valueOf(telemetry.getLongitude())).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(telemetry.getTimestamp())).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());

        return telemetry;
    }

    /**
     * Retrieves telemetry readings for a specific unit, sorted by timestamp descending, limited to requested count.
     */
    public List<Telemetry> findLatestByUnitId(String unitId, int limit) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#u", "unitId");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":uVal", AttributeValue.builder().s(unitId).build());

        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("#u = :uVal")
                .expressionAttributeNames(expressionNames)
                .expressionAttributeValues(expressionValues)
                .build());

        List<Telemetry> readings = new ArrayList<>();
        if (response.hasItems()) {
            for (Map<String, AttributeValue> item : response.items()) {
                readings.add(mapToTelemetry(item));
            }
        }

        // Sort descending by timestamp and limit to 20
        return readings.stream()
                .sorted(Comparator.comparingLong(Telemetry::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Helper to convert DynamoDB AttributeValue map into a Telemetry object.
     */
    private Telemetry mapToTelemetry(Map<String, AttributeValue> item) {
        return Telemetry.builder()
                .telemetryId(item.get("telemetryId") != null ? item.get("telemetryId").s() : null)
                .unitId(item.get("unitId") != null ? item.get("unitId").s() : null)
                .temperature(item.get("temperature") != null ? Double.parseDouble(item.get("temperature").n()) : 0.0)
                .humidity(item.get("humidity") != null ? Double.parseDouble(item.get("humidity").n()) : 0.0)
                .latitude(item.get("latitude") != null ? Double.parseDouble(item.get("latitude").n()) : 0.0)
                .longitude(item.get("longitude") != null ? Double.parseDouble(item.get("longitude").n()) : 0.0)
                .timestamp(item.get("timestamp") != null ? Long.parseLong(item.get("timestamp").n()) : 0L)
                .build();
    }
}
