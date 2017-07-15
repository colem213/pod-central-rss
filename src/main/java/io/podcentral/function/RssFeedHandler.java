package io.podcentral.function;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.ConversionSchemas;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import io.podcentral.model.FeedForm;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.rss.Channel;
import io.podcentral.rss.Item;
import io.podcentral.rss.RssFeed;
import lombok.extern.log4j.Log4j2;

/**
 * Lambda function that triggered by the API Gateway event "POST /". It reads all the query
 * parameters as the metadata for this article and stores them to a DynamoDB table. It reads the
 * payload as the content of the article and stores it to a S3 bucket.
 */
@Log4j2
public class RssFeedHandler implements RequestHandler<ServerlessInput, ServerlessOutput> {
  HttpResponse<InputStream> rsp;
  private static JAXBContext CTX;

  public static final String DYNAMO_ENDPOINT = "DYNAMO_ENDPOINT";

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    ServerlessOutput output = new ServerlessOutput();
    ObjectMapper mapper = new ObjectMapper();

    try {
      FeedForm form = mapper.readValue(input.getBody(), FeedForm.class);
      log.info("Url={}", form.getFeedUrl());

      rsp = rsp == null ? Unirest.get(form.getFeedUrl()).asBinary() : rsp;
      if (log.isDebugEnabled()) {
        String headers = String.join(", ", rsp.getHeaders().entrySet().stream().map(
            entry -> String.format("[%s=%s]", entry.getKey(), String.join(", ", entry.getValue())))
            .collect(Collectors.toList()));
        log.debug("Status Code={}, Headers={{}}", rsp.getStatus(), headers);
      }
      if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
        throw new Exception("Failed request: " + form.getFeedUrl());
      }

      CTX = CTX == null ? JAXBContext.newInstance(RssFeed.class) : CTX;
      Unmarshaller des = CTX.createUnmarshaller();
      RssFeed feed = (RssFeed) des.unmarshal(rsp.getBody());

      String endpoint = System.getenv(DYNAMO_ENDPOINT);

      AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
      if (endpoint != null) {
        builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
      }
      AmazonDynamoDB client = builder.build();

      DynamoDBMapper dynDbMapper = new DynamoDBMapper(client);
      DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
          .withSaveBehavior(SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES)
          .withConversionSchema(ConversionSchemas.V2).build();
      dynDbMapper.save(feed.getChannel(), config);
      feed.getChannel().getItems().stream()
          .forEach(item -> item.setChannelId(feed.getChannel().getId()));
      dynDbMapper.batchSave(feed.getChannel().getItems());

      log.trace(feed);

      output.setStatusCode(200);
    } catch (Exception e) {
      output.setStatusCode(500);
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      output.setBody(sw.toString());
      log.error(output);
    } finally {
      rsp = null;
    }
    return output;
  }

  public static void main(String[] args) {
    String endpoint = System.getenv(DYNAMO_ENDPOINT);

    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
    if (endpoint != null) {
      builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
    }
    AmazonDynamoDB client = builder.build();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    CreateTableRequest tableReq = mapper.generateCreateTableRequest(Channel.class);
    ProvisionedThroughput thruPut = new ProvisionedThroughput(3L, 3L);
    tableReq.setProvisionedThroughput(thruPut);
    tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
    client.createTable(tableReq);

    tableReq = mapper.generateCreateTableRequest(Item.class);
    tableReq.setProvisionedThroughput(thruPut);
    tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
    client.createTable(tableReq);
  }
}
