package io.podcentral.config;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnvConfig {
  public static final String DYNAMO_ENDPOINT = "DYNAMO_ENDPOINT";

  public static AmazonDynamoDB getDynamoDbClient() {
    String endpoint = System.getenv(DYNAMO_ENDPOINT);
    String region = System.getenv("AWS_DEFAULT_REGION");

    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
    if (endpoint != null) {
      builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
    } else {
      endpoint = String.format("https://dynamodb.%s.amazonaws.com", region);
      builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, region));
    }
    AmazonDynamoDB client = builder.build();
    return client;
  }

  public static DynamoDBMapper getDynamoDbMapper() {
    AmazonDynamoDB client = getDynamoDbClient();
    DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTableNameResolver(new EnvTableNameResolver()).build();
    return new DynamoDBMapper(client, config);
  }
}
