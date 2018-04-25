package io.podcentral.apigw;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpStatus;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;

import io.podcentral.entity.ChannelUrl;
import io.podcentral.entity.Subscription;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.model.ServerlessOutput.ServerlessOutputBuilder;
import io.podcentral.rss.Channel;
import io.podcentral.rss.RssFeed;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
public class SubscriptionController {

  private ServerlessInput in;
  private ServerlessOutputBuilder out;
  private Context context;
  private ObjectMapper mapper;
  private DynamoDBMapper dbMapper;

  public ServerlessOutput post() {
    if (!"post".equalsIgnoreCase(in.getHttpMethod())) {
      return ServerlessOutput.invalidMethod();
    }
    if (context.getIdentity() == null || context.getIdentity().getIdentityId() == null
        || context.getIdentity().getIdentityId().isEmpty()) {
      return ServerlessOutput.unauthorized();
    }
    String userId = context.getIdentity().getIdentityId();

    String feedUrl;
    try {
      feedUrl = mapper.readTree(in.getBody()).at("/feedUrl").asText();
    } catch (IOException e) {
      return ServerlessOutput.badRequest("InvalidRequestBodyException",
          "The request body is unable to be processed");
    }
    Optional<ChannelUrl> url = null;// = transformChannelUrl(feedUrl);
    log.info("RawUrl={}", feedUrl);
    log.info("Url={}", url.get().getUrl());

    ObjectNode channelIdNode = mapper.createObjectNode();
    Optional<ChannelUrl> dbUrl = Optional.ofNullable(dbMapper.load(url.get()));
    if (dbUrl.isPresent()) {
      url = dbUrl;
      Subscription sub = new Subscription(userId, url.get().getId());

      try {
        dbMapper.save(sub, new DynamoDBSaveExpression().withExpectedEntry("channelId",
            new ExpectedAttributeValue().withExists(false)));
      } catch (ConditionalCheckFailedException e) {
        log.warn("Subscription already exists!\t{}", sub);
        return ServerlessOutput.badRequest("SubscriptonAlreadyExistsException",
            "A subscription to this channel already exists!");
      }

      channelIdNode.put("channelId", url.get().getId());
      return out.statusCode(HttpStatus.SC_CREATED).body(channelIdNode).build();
    } else {
      HttpResponse<InputStream> rsp = null;// = fetchFeedFromUrl(url.get().getUrl());
      if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
        log.warn("StatusCode={} from url={}", rsp.getStatus(), url.get().getUrl());
        return ServerlessOutput.badRequest("UnreachableException",
            "Unable to get a response from " + url.get().getUrl());
      }

      // CTX = CTX == null ? JAXBContext.newInstance(RssFeed.class) : CTX;
      Unmarshaller des = null;// = CTX.createUnmarshaller();
      RssFeed feed;
      try {
        feed = (RssFeed) des.unmarshal(rsp.getBody());
      } catch (JAXBException e) {
        log.error("Failed parsing RSS Feed", e);
        return ServerlessOutput.badRequest("InvalidResponseException",
            "Unable to parse feed received from " + url.get().getUrl());
      }

      final String channelId = UUID.randomUUID().toString();
      url.get().setId(channelId);
      Subscription sub = new Subscription(userId, channelId);
      Channel channel = feed.getChannel();
      channel.setId(channelId);
      channel.getItems().stream().forEach(item -> item.setChannelId(channelId));

      List<Object> toSave = new ArrayList<Object>(Arrays.asList(url.get(), sub, channel));
      toSave.addAll(channel.getItems());
      List<FailedBatch> failed = dbMapper.batchSave(toSave);
      failed.forEach(batchErr -> log.error("Failed to save item to DynamoDB. channel={}", channelId,
          batchErr.getException()));
      log.trace(feed);


      channelIdNode.put("channelId", channelId);
      return out.statusCode(HttpStatus.SC_CREATED).body(channelIdNode).build();
    }
  }

}
