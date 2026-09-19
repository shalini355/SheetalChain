package com.sheetalchain.backend.repository;

import com.sheetalchain.backend.model.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO Repository for managing 'Alert' records in DynamoDB "Alerts" table.
 */
@Repository
public class AlertRepository {

    private static final String TABLE_NAME = "Alerts";
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public AlertRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Saves an Alert entry in DynamoDB.
     */
    public Alert save(Alert alert) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("alertId", AttributeValue.builder().s(alert.getAlertId()).build());
        item.put("unitId", AttributeValue.builder().s(alert.getUnitId()).build());
        item.put("severity", AttributeValue.builder().s(alert.getSeverity()).build());
        item.put("message", AttributeValue.builder().s(alert.getMessage()).build());
        item.put("temperature", AttributeValue.builder().n(String.valueOf(alert.getTemperature())).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(alert.getTimestamp())).build());
        if (alert.getSource() != null) {
            item.put("source", AttributeValue.builder().s(alert.getSource()).build());
        }
        if (alert.getReasoning() != null) {
            item.put("reasoning", AttributeValue.builder().s(alert.getReasoning()).build());
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());

        return alert;
    }

    /**
     * Lists all alerts across all units, sorted by timestamp descending.
     */
    public List<Alert> findAll() {
        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build());

        List<Alert> alerts = new ArrayList<>();
        if (response.hasItems()) {
            for (Map<String, AttributeValue> item : response.items()) {
                alerts.add(mapToAlert(item));
            }
        }

        return alerts.stream()
                .sorted(Comparator.comparingLong(Alert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Lists alerts for a specific unit, sorted by timestamp descending.
     */
    public List<Alert> findByUnitId(String unitId) {
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

        List<Alert> alerts = new ArrayList<>();
        if (response.hasItems()) {
            for (Map<String, AttributeValue> item : response.items()) {
                alerts.add(mapToAlert(item));
            }
        }

        return alerts.stream()
                .sorted(Comparator.comparingLong(Alert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Helper to convert DynamoDB AttributeValue map into an Alert object.
     */
    private Alert mapToAlert(Map<String, AttributeValue> item) {
        return Alert.builder()
                .alertId(item.get("alertId") != null ? item.get("alertId").s() : null)
                .unitId(item.get("unitId") != null ? item.get("unitId").s() : null)
                .severity(item.get("severity") != null ? item.get("severity").s() : null)
                .message(item.get("message") != null ? item.get("message").s() : null)
                .temperature(item.get("temperature") != null ? Double.parseDouble(item.get("temperature").n()) : 0.0)
                .timestamp(item.get("timestamp") != null ? Long.parseLong(item.get("timestamp").n()) : 0L)
                .source(item.get("source") != null ? item.get("source").s() : "BACKEND_RULE")
                .reasoning(item.get("reasoning") != null ? item.get("reasoning").s() : null)
                .build();
    }
}
