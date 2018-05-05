package io.podcentral.config;

import static org.picocontainer.Characteristics.CACHE;

import java.util.Map;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import io.podcentral.model.ServerlessOutput.ServerlessOutputBuilder;

public class DepInjectionConfig {
  public static final String DYNAMO_ENDPOINT = "DYNAMO_ENDPOINT";

  public static DefaultPicoContainer defaultConfig() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new ISO8601DateFormat());

    DefaultPicoContainer container = new DefaultPicoContainer();
    container.as(CACHE).addComponent(PicoContainer.class, container);
    container.as(CACHE).addComponent(mapper);
    AmazonDynamoDB client = getDynamoDbClient();
    container.as(CACHE).addComponent(client);
    container.as(CACHE).addComponent(new DynamoDB(client));
    container.addComponent(ServerlessOutputBuilder.class);

    return container;
  }

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
    return builder.build();
  }

  public static DynamoDBMapper getDynamoDbMapper(AmazonDynamoDB client,
      Map<String, String> stageVariables) {
    DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTableNameResolver(new EnvTableNameResolver(stageVariables)).build();
    return new DynamoDBMapper(client, config);
  }
}
