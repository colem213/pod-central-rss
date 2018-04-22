package io.podcentral;

import java.time.Instant;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverterFactory;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

import io.podcentral.aws.InstantConverter;

public class FeedHandlerIntTest {
  private static final String PORT = System.getProperty("dynamodb.port");
  private DynamoDBMapper mapper;
  private AmazonDynamoDB client;
  private Reflections reflect;

  public FeedHandlerIntTest() {
    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
    builder.setEndpointConfiguration(
        new EndpointConfiguration(String.format("http://localhost:%s", PORT), ""));
    client = builder.build();
    DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBTypeConverterFactory.standard().override()
            .with(String.class, Instant.class, new InstantConverter()).build())
        .build();
    mapper = new DynamoDBMapper(client, config);
    reflect = new Reflections(new ConfigurationBuilder().forPackages("io.podcentral")
        .filterInputsBy(str -> str.endsWith(".class")));
  }

  @Test
  public void worksWithAwsDynamoDb() {
    ListTablesResult list = client.listTables();
    for (String name : list.getTableNames()) {
      System.out.println("table found: " + name);
    }
  }

  @Before
  public void setUp() {
    Set<Class<?>> annotated = reflect.getTypesAnnotatedWith(DynamoDBTable.class);

    ProvisionedThroughput thruPut = new ProvisionedThroughput(3L, 3L);
    for (Class<?> table : annotated) {
      CreateTableRequest tableReq = mapper.generateCreateTableRequest(table);
      tableReq.setProvisionedThroughput(thruPut);
      if (tableReq.getGlobalSecondaryIndexes() != null) {
        tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
      }
      client.createTable(tableReq);
    }
  }

  @After
  public void tearDown() {
    Set<Class<?>> annotated = reflect.getTypesAnnotatedWith(DynamoDBTable.class);

    for (Class<?> tableClass : annotated) {
      DynamoDBTable table = tableClass.getAnnotation(DynamoDBTable.class);
      client.deleteTable(table.tableName());
    }
  }
}