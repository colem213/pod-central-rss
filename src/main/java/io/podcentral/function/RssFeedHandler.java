package io.podcentral.function;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.HttpStatus;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.podcentral.EnvConfig;
import io.podcentral.TableConstants;
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
    if (context.getIdentity() == null || context.getIdentity().getIdentityId() == null) {
      return ServerlessOutput.builder().statusCode(HttpStatus.UNAUTHORIZED.value()).build();
    }
    String userId = context.getIdentity().getIdentityId();
    ObjectMapper mapper = new ObjectMapper();

    try {
      FeedForm form = mapper.readValue(input.getBody(), FeedForm.class);
      log.info("Url={}", form.getFeedUrl());

      DynamoDBMapper dbMapper = EnvConfig.getDynamoDbMapper();

      Optional<Channel> channel = queryForChannelByUrl(dbMapper, form.getFeedUrl());
      if (channel.isPresent()) {
        Subscription sub = new Subscription(userId, channel.get().getId(), new Date());

        try {
          saveSubscriptionIfNotExists(dbMapper, sub);
        } catch (ConditionalCheckFailedException e) {
          log.warn("Subscription already exists!\t{}", sub);
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value()).build();
        }

        queryForItemsByChannel(dbMapper, channel);
        return ServerlessOutput.builder().statusCode(HttpStatus.CREATED.value())
            .body(mapper.writeValueAsString(channel.get())).build();
      } else {
        HttpResponse<InputStream> rsp = fetchFeedFromUrl(form.getFeedUrl());
        if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value()).build();
        }

        CTX = CTX == null ? JAXBContext.newInstance(RssFeed.class) : CTX;
        Unmarshaller des = CTX.createUnmarshaller();
        Optional<RssFeed> feed = Optional.empty();
        try {
          feed = Optional.of((RssFeed) des.unmarshal(rsp.getBody()));
        } catch (JAXBException e) {
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value()).build();
        }

        channel = Optional.of(feed.get().getChannel());
        channel.get().setFeedUrl(form.getFeedUrl());
        if (saveChannelIfNotExists(dbMapper, channel.get())) {
          final String channelId = channel.get().getId();
          channel.get().getItems().stream().forEach(item -> item.setChannelId(channelId));
          dbMapper.batchSave(channel.get().getItems());
        }
        log.trace(feed);

        Subscription sub = new Subscription(userId, channel.get().getId(), new Date());
        try {
          saveSubscriptionIfNotExists(dbMapper, sub);
          return ServerlessOutput.builder().statusCode(HttpStatus.CREATED.value())
              .body(mapper.writeValueAsString(channel.get())).build();
        } catch (ConditionalCheckFailedException e) {
          log.error("Error saving subscription!\t{}", sub);
          return ServerlessOutput.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .build();
        }
      }
    } catch (Exception e) {
      log.error(e);
      return ServerlessOutput.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
          .build();
    }
  }

  Optional<Channel> queryForChannelByUrl(DynamoDBMapper mapper, String url) {
    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>(1);
    eav.put(":url", new AttributeValue().withS(url));

    DynamoDBQueryExpression<Channel> channelQuery =
        new DynamoDBQueryExpression<Channel>().withIndexName(TableConstants.Channel.GSI_URL_INDEX)
            .withConsistentRead(false).withKeyConditionExpression("feedUrl = :url")
            .withExpressionAttributeValues(eav).withLimit(1).withSelect(Select.ALL_ATTRIBUTES);

    PaginatedList<Channel> results = mapper.query(Channel.class, channelQuery);
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
        new DynamoDBQueryExpression<Item>().withIndexName(TableConstants.Item.GSI_CHANNEL_INDEX)
            .withConsistentRead(false).withKeyConditionExpression("channelId = :id")
            .withExpressionAttributeValues(eav).withSelect(Select.ALL_ATTRIBUTES);

    PaginatedQueryList<Item> items = mapper.query(Item.class, query);
    channel.get().setItems(items);
  }

  boolean saveChannelIfNotExists(DynamoDBMapper mapper, Channel channel) {
    DynamoDBSaveExpression saveExpr = new DynamoDBSaveExpression().withExpectedEntry("feedUrl",
        new ExpectedAttributeValue(new AttributeValue(channel.getFeedUrl())).withExists(false));

    try {
      mapper.save(channel, saveExpr);
      return true;
    } catch (ConditionalCheckFailedException e) {
      log.warn("Channel already exists!\t{}", channel);
      return false;
    }

  }

  void saveSubscriptionIfNotExists(DynamoDBMapper mapper, Subscription sub)
      throws ConditionalCheckFailedException {
    DynamoDBSaveExpression saveExpr = new DynamoDBSaveExpression().withExpectedEntry("channelId",
        new ExpectedAttributeValue(new AttributeValue(sub.getChannelId())).withExists(false));

    mapper.save(sub, saveExpr);
  }

  HttpResponse<InputStream> fetchFeedFromUrl(String url) throws UnirestException, Exception {
    HttpResponse<InputStream> rsp = mockRsp == null ? Unirest.get(url).asBinary() : mockRsp;
    if (log.isDebugEnabled()) {
      String headers = String.join(", ", rsp.getHeaders().entrySet().stream().map(
          entry -> String.format("[%s=%s]", entry.getKey(), String.join(", ", entry.getValue())))
          .collect(Collectors.toList()));
      log.debug("Status Code={}, Headers={{}}", rsp.getStatus(), headers);
    }
    return rsp;
  }

  public static void main(String[] args) {
    DynamoDBMapper mapper = EnvConfig.getDynamoDbMapper();
    AmazonDynamoDB client = EnvConfig.getDynamoDbClient();

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
