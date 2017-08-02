package io.podcentral.function;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.HttpStatus;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.podcentral.EnvConfig;
import io.podcentral.model.FeedForm;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.model.Subscription;
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
  HttpResponse<InputStream> mockRsp;
  private static JAXBContext CTX;

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    ServerlessOutput output = new ServerlessOutput();
    output.setStatusCode(HttpStatus.CREATED.value());
    ObjectMapper mapper = new ObjectMapper();

    try {
      FeedForm form = mapper.readValue(input.getBody(), FeedForm.class);
      log.info("Url={}", form.getFeedUrl());

      DynamoDBMapper dynDbMapper = EnvConfig.getDynamoDbMapper();

      Optional<Channel> channel = queryForChannelByUrl(dynDbMapper, form.getFeedUrl());
      Optional<Subscription> sub = queryForSubscriptionByChannel(dynDbMapper,
          Optional.ofNullable(context.getIdentity().getIdentityId()), channel);
      queryForItemsByChannel(dynDbMapper, channel);

      if (channel.isPresent()) {
        output.setBody(mapper.writeValueAsString(channel.get()));
        return output;
      }

      HttpResponse<InputStream> rsp = fetchFeedFromUrl(form.getFeedUrl());

      CTX = CTX == null ? JAXBContext.newInstance(RssFeed.class) : CTX;
      Unmarshaller des = CTX.createUnmarshaller();
      RssFeed feed = (RssFeed) des.unmarshal(rsp.getBody());

      dynDbMapper.save(feed.getChannel());
      feed.getChannel().getItems().stream()
          .forEach(item -> item.setChannelId(feed.getChannel().getId()));
      dynDbMapper.batchSave(feed.getChannel().getItems());
      log.trace(feed);

      output.setBody(mapper.writeValueAsString(feed.getChannel()));
    } catch (Exception e) {
      output.setStatusCode(500);
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      output.setBody(sw.toString());
      log.error(output);
    }
    return output;
  }

  Optional<Channel> queryForChannelByUrl(DynamoDBMapper mapper, String url) {
    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>(1);
    eav.put(":feedUrl", new AttributeValue().withS(url));

    DynamoDBQueryExpression<Channel> channelQuery =
        new DynamoDBQueryExpression<Channel>().withIndexName("feedUrl").withConsistentRead(false)
            .withKeyConditionExpression("feedUrl = :feedUrl").withExpressionAttributeValues(eav)
            .withLimit(1).withSelect(Select.ALL_ATTRIBUTES);

    PaginatedList<Channel> results = mapper.query(Channel.class, channelQuery);
    if (!results.isEmpty()) {
      return Optional.of(results.get(0));
    } else {
      return Optional.empty();
    }
  }

  Optional<Subscription> queryForSubscriptionByChannel(DynamoDBMapper mapper,
      Optional<String> identity, Optional<Channel> channel) {
    if (!channel.isPresent() || !identity.isPresent())
      return Optional.empty();

    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>(2);
    eav.put(":userId", new AttributeValue().withS(identity.get()));
    eav.put(":channelId", new AttributeValue().withS(channel.get().getId()));

    DynamoDBQueryExpression<Subscription> query = new DynamoDBQueryExpression<Subscription>()
        .withIndexName("ChannelRangeIndex").withConsistentRead(false)
        .withKeyConditionExpression("userId = :userId and channelId = :channelId")
        .withExpressionAttributeValues(eav);

    PaginatedQueryList<Subscription> results = mapper.query(Subscription.class, query);
    if (!results.isEmpty()) {
      return Optional.of(results.get(0));
    } else {
      return Optional.empty();
    }
  }

  void queryForItemsByChannel(DynamoDBMapper mapper, Optional<Channel> channel) {
    if (!channel.isPresent())
      return;

    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>(1);
    eav.put(":id", new AttributeValue().withS(channel.get().getId()));

    DynamoDBQueryExpression<Item> query =
        new DynamoDBQueryExpression<Item>().withIndexName("itemsByChannel")
            .withConsistentRead(false).withKeyConditionExpression("channelId = :id")
            .withExpressionAttributeValues(eav).withSelect(Select.ALL_ATTRIBUTES);

    PaginatedQueryList<Item> items = mapper.query(Item.class, query);
    channel.get().setItems(items);
  }

  HttpResponse<InputStream> fetchFeedFromUrl(String url) throws UnirestException, Exception {
    HttpResponse<InputStream> rsp = mockRsp == null ? Unirest.get(url).asBinary() : mockRsp;
    if (log.isDebugEnabled()) {
      String headers = String.join(", ", rsp.getHeaders().entrySet().stream().map(
          entry -> String.format("[%s=%s]", entry.getKey(), String.join(", ", entry.getValue())))
          .collect(Collectors.toList()));
      log.debug("Status Code={}, Headers={{}}", rsp.getStatus(), headers);
    }
    if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
      throw new Exception("Failed request: " + url);
    }
    return rsp;
  }

  public static void main(String[] args) {
    DynamoDBMapper mapper = EnvConfig.getDynamoDbMapper();

    String endpoint = System.getenv(EnvConfig.DYNAMO_ENDPOINT);

    AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
    if (endpoint != null) {
      builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
    }
    AmazonDynamoDB client = builder.build();

    CreateTableRequest tableReq = mapper.generateCreateTableRequest(Channel.class);
    ProvisionedThroughput thruPut = new ProvisionedThroughput(3L, 3L);
    tableReq.setProvisionedThroughput(thruPut);
    tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
    client.createTable(tableReq);

    tableReq = mapper.generateCreateTableRequest(Item.class);
    tableReq.setProvisionedThroughput(thruPut);
    tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
    client.createTable(tableReq);

    tableReq = mapper.generateCreateTableRequest(Subscription.class);
    tableReq.setProvisionedThroughput(thruPut);
    client.createTable(tableReq);
  }
}
