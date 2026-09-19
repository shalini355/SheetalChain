package com.sheetalchain.backend.repository;

import com.sheetalchain.backend.model.Unit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

/**
 * DAO Repository for managing 'Unit' records in DynamoDB "Units" table.
 */
@Repository
public class UnitRepository {

    private static final String TABLE_NAME = "Units";
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public UnitRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Saves or updates a Unit record in DynamoDB.
     */
    public Unit save(Unit unit) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("unitId", AttributeValue.builder().s(unit.getUnitId()).build());
        item.put("farmerId", AttributeValue.builder().s(unit.getFarmerId()).build());
        item.put("farmerName", AttributeValue.builder().s(unit.getFarmerName()).build());
        item.put("location", AttributeValue.builder().s(unit.getLocation()).build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(unit.getCreatedAt())).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());

        return unit;
    }

    /**
     * Retrieves a Unit by its unitId partition key.
     */
    public Optional<Unit> findById(String unitId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("unitId", AttributeValue.builder().s(unitId).build());

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build());

        if (response.hasItem() && !response.item().isEmpty()) {
            return Optional.of(mapToUnit(response.item()));
        }
        return Optional.empty();
    }

    /**
     * Lists all registered units.
     */
    public List<Unit> findAll() {
        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build());

        List<Unit> units = new ArrayList<>();
        if (response.hasItems()) {
            for (Map<String, AttributeValue> item : response.items()) {
                units.add(mapToUnit(item));
            }
        }
        return units;
    }

    /**
     * Helper to convert DynamoDB AttributeValue map into a Unit object.
     */
    private Unit mapToUnit(Map<String, AttributeValue> item) {
        return Unit.builder()
                .unitId(item.get("unitId") != null ? item.get("unitId").s() : null)
                .farmerId(item.get("farmerId") != null ? item.get("farmerId").s() : null)
                .farmerName(item.get("farmerName") != null ? item.get("farmerName").s() : null)
                .location(item.get("location") != null ? item.get("location").s() : null)
                .createdAt(item.get("createdAt") != null ? Long.parseLong(item.get("createdAt").n()) : 0L)
                .build();
    }
}
