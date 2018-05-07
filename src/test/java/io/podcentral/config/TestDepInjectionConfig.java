package io.podcentral.config;

import static org.picocontainer.Characteristics.*;

import java.util.HashMap;

import org.picocontainer.DefaultPicoContainer;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public class TestDepInjectionConfig {
  public static DefaultPicoContainer defaultConfig() {
    DefaultPicoContainer container = new DefaultPicoContainer(DepInjectionConfig.defaultConfig());

    AmazonDynamoDB client = DepInjectionConfig.getDynamoDbClient(
        String.format("http://localhost:%s", System.getProperty("dynamodb.port")));
    container.as(CACHE).addComponent(client);
    container.as(CACHE).addComponent(new DynamoDB(client));
    container.as(CACHE).addComponent(DepInjectionConfig.getDynamoDbMapper(client, new HashMap<>()));
    return container;
  }
}
