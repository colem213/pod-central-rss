package io.podcentral;

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

  public static DynamoDBMapper getDynamoDbMapper() {
    String endpoint = System.getenv(DYNAMO_ENDPOINT);

    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
    if (endpoint != null) {
      builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
    }
    AmazonDynamoDB client = builder.build();
    DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTableNameResolver(new EnvTableNameResolver()).build();
    return new DynamoDBMapper(client, config);
  }
}
