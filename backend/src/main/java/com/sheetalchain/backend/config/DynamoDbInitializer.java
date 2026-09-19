package com.sheetalchain.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Startup component that ensures required DynamoDB tables ("Units", "Telemetry", "Alerts")
 * exist in LocalStack upon application startup.
 */
@Component
public class DynamoDbInitializer implements CommandLineRunner {

    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public DynamoDbInitializer(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public void run(String... args) {
        createTableIfNotExists("Units", "unitId");
        createTableIfNotExists("Telemetry", "telemetryId");
        createTableIfNotExists("Alerts", "alertId");
    }

    private void createTableIfNotExists(String tableName, String partitionKeyName) {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            System.out.println("✅ DynamoDB table already exists: " + tableName);
        } catch (ResourceNotFoundException e) {
            System.out.println("⚡ Creating DynamoDB table: " + tableName + " with partition key: " + partitionKeyName);
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(partitionKeyName)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName(partitionKeyName)
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            System.out.println("✅ Created DynamoDB table: " + tableName);
        } catch (DynamoDbException e) {
            System.err.println("❌ Error ensuring table " + tableName + " exists: " + e.getMessage());
        }
    }
}
