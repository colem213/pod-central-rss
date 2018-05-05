package io.podcentral.apigw;

import java.time.Instant;
import java.util.HashMap;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.picocontainer.DefaultPicoContainer;
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
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;

import io.podcentral.aws.InstantConverter;
import io.podcentral.config.DepInjectionConfig;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceIntTest {
  private static final String PORT = System.getProperty("dynamodb.port");
  private DynamoDBMapper mapper;
  private AmazonDynamoDB client;
  private Reflections reflect;

  @Mock
  Context context;
  @Mock
  CognitoIdentity ident;

  public SubscriptionServiceIntTest() {
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
  public void shouldParseAndSubscribeToRssFeed() {
    Mockito.when(ident.getIdentityId()).thenReturn("test");
    DefaultPicoContainer test = new DefaultPicoContainer(DepInjectionConfig.defaultConfig());
    test.addComponent(SubscriptionService.class);
    test.addComponent(ServerlessInput.builder().httpMethod("post")
        .body("[\"http://audioboom.com/channels/3709182.rss\"]").build());
    test.addComponent(context);
    test.addComponent(DepInjectionConfig.getDynamoDbMapper(test.getComponent(AmazonDynamoDB.class),
        new HashMap<>()));

    SubscriptionService subService = test.getComponent(SubscriptionService.class);

    ServerlessOutput out = subService.subscribe();
  }

  @Before
  public void setUp() {
    Mockito.when(context.getIdentity()).thenReturn(ident);

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
