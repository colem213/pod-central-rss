package io.podcentral.function;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.reflections.Reflections;
import org.springframework.http.HttpStatus;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.podcentral.config.EnvConfig;
import io.podcentral.config.TableConstants;
import io.podcentral.entity.ChannelUrl;
import io.podcentral.entity.Subscription;
import io.podcentral.model.Error;
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
  HttpResponse<InputStream> mockRsp;
  private static JAXBContext CTX;

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    if (context.getIdentity() == null || context.getIdentity().getIdentityId() == null) {
      return ServerlessOutput.builder().statusCode(HttpStatus.UNAUTHORIZED.value()).build();
    }
    String userId = context.getIdentity().getIdentityId();
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new ISO8601DateFormat());

    try {
      FeedForm form = mapper.readValue(input.getBody(), FeedForm.class);
      Optional<ChannelUrl> url = Optional.of(new ChannelUrl(form.getFeedUrl()));
      log.info("RawUrl={}", form.getFeedUrl());
      log.info("Url={}", url.get().getUrl());

      DynamoDBMapper dbMapper = EnvConfig.getDynamoDbMapper();

      Optional<ChannelUrl> dbUrl = Optional.ofNullable(dbMapper.load(url.get()));
      if (dbUrl.isPresent()) {
        url = dbUrl;
        Subscription sub = new Subscription(userId, url.get().getId(), new Date());

        try {
          dbMapper.save(sub, new DynamoDBSaveExpression().withExpectedEntry("channelId",
              new ExpectedAttributeValue().withExists(false)));
        } catch (ConditionalCheckFailedException e) {
          log.warn("Subscription already exists!\t{}", sub);
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value())
              .body(mapper.writeValueAsString(new Error("SubscriptonAlreadyExistsException",
                  "A subscription to this channel already exists!")))
              .build();
        }

        Channel channel = new Channel(url.get().getId());
        channel = dbMapper.load(channel);
        queryItemsForChannel(dbMapper, channel);
        return ServerlessOutput.builder().statusCode(HttpStatus.CREATED.value())
            .body(mapper.writeValueAsString(channel)).build();
      } else {
        HttpResponse<InputStream> rsp = fetchFeedFromUrl(url.get().getUrl());
        if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
          log.warn("StatusCode={} from url={}", rsp.getStatus(), url.get().getUrl());
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value())
              .body(mapper.writeValueAsString(new Error("UnreachableException",
                  "Unable to get a response from " + url.get().getUrl())))
              .build();
        }

        CTX = CTX == null ? JAXBContext.newInstance(RssFeed.class) : CTX;
        Unmarshaller des = CTX.createUnmarshaller();
        RssFeed feed;
        try {
          feed = (RssFeed) des.unmarshal(rsp.getBody());
        } catch (JAXBException e) {
          log.error("Failed parsing RSS Feed", e);
          return ServerlessOutput.builder().statusCode(HttpStatus.BAD_REQUEST.value())
              .body(mapper.writeValueAsString(new Error("InvalidResponseException",
                  "Unable to parse feed received from " + url.get().getUrl())))
              .build();
        }

        final String channelId = UUID.randomUUID().toString();
        url.get().setId(channelId);
        Subscription sub = new Subscription(userId, channelId, new Date());
        Channel channel = feed.getChannel();
        channel.setId(channelId);
        channel.getItems().stream().forEach(item -> item.setChannelId(channelId));

        List<Object> toSave = new ArrayList<Object>(Arrays.asList(url.get(), sub, channel));
        toSave.addAll(channel.getItems());
        List<FailedBatch> failed = dbMapper.batchSave(toSave);
        failed.forEach(
            batchErr -> log.error("Failed to save item to DynamoDB", batchErr.getException()));
        log.trace(feed);

        return ServerlessOutput.builder().statusCode(HttpStatus.CREATED.value())
            .body(mapper.writeValueAsString(channel)).build();
      }
    } catch (Exception e) {
      log.error("", e);
      return ServerlessOutput.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
          .build();
    }
  }

  void queryItemsForChannel(DynamoDBMapper mapper, Channel channel) {
    Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>(1);
    eav.put(":id", new AttributeValue().withS(channel.getId()));

    DynamoDBQueryExpression<Item> query =
        new DynamoDBQueryExpression<Item>().withIndexName(TableConstants.Item.GSI_CHANNEL_INDEX)
            .withConsistentRead(false).withKeyConditionExpression("channelId = :id")
            .withExpressionAttributeValues(eav).withSelect(Select.ALL_ATTRIBUTES);

    channel.setItems(mapper.query(Item.class, query));
  }

  HttpResponse<InputStream> fetchFeedFromUrl(String url) throws UnirestException, Exception {
    HttpResponse<InputStream> rsp = mockRsp == null ? Unirest.get(url).asBinary() : mockRsp;
    if (log.isDebugEnabled()) {
      String headers = String.join(", ", rsp.getHeaders().entrySet().stream().map(
          entry -> String.format("[%s=%s]", entry.getKey(), String.join(", ", entry.getValue())))
          .collect(Collectors.toList()));
      log.debug("StatusCode={}, Headers={{}}", rsp.getStatus(), headers);
    }
    return rsp;
  }

  public static void main(String[] args) {
    DynamoDBMapper mapper = EnvConfig.getDynamoDbMapper();
    AmazonDynamoDB client = EnvConfig.getDynamoDbClient();

    Reflections reflections = new Reflections("io.podcentral");
    Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DynamoDBTable.class);

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
}
