package io.podcentral.function;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.reflections.Reflections;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.podcentral.config.EnvConfig;
import io.podcentral.entity.ChannelUrl;
import io.podcentral.entity.Subscription;
import io.podcentral.model.Error;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.rss.Channel;
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
  private ObjectMapper mapper;
  private static final ServerlessOutput NOT_FOUND =
      ServerlessOutput.builder().statusCode(HttpStatus.SC_NOT_FOUND).build();
  private static final ServerlessOutput INVALID_METHOD =
      ServerlessOutput.builder().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED).build();
  private static final ServerlessOutput UNAUTHORIZED =
      ServerlessOutput.builder().statusCode(HttpStatus.SC_UNAUTHORIZED).build();

  public RssFeedHandler() {
    mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new ISO8601DateFormat());
  }

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    try {
      log.info(mapper.writeValueAsString(input));
    } catch (JsonProcessingException e) {
    }
    switch (input.getPathParameters().getOrDefault("proxy", "")) {
      case "subscribe":
        return subscribe(input, context);
    }
    return NOT_FOUND;
  }

  ServerlessOutput subscribe(ServerlessInput input, Context context) {
    if (!"post".equalsIgnoreCase(input.getHttpMethod())) {
      return INVALID_METHOD;
    }
    if (context.getIdentity() == null || context.getIdentity().getIdentityId() == null
        || context.getIdentity().getIdentityId().isEmpty()) {
      return UNAUTHORIZED;
    }
    String userId = context.getIdentity().getIdentityId();

    try {
      String feedUrl;
      try {
        feedUrl = mapper.readTree(input.getBody()).at("/feedUrl").asText();
      } catch (IOException e) {
        return ServerlessOutput.builder().statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(mapper.writeValueAsString(new Error("InvalidRequestBodyException",
                "The request body is unable to be processed!")))
            .build();
      }
      Optional<ChannelUrl> url = transformChannelUrl(feedUrl);
      log.info("RawUrl={}", feedUrl);
      log.info("Url={}", url.get().getUrl());

      DynamoDBMapper dbMapper = EnvConfig.getDynamoDbMapper(input.getStageVariables());

      ObjectNode channelIdNode = mapper.createObjectNode();
      Optional<ChannelUrl> dbUrl = Optional.ofNullable(dbMapper.load(url.get()));
      if (dbUrl.isPresent()) {
        url = dbUrl;
        Subscription sub = new Subscription(userId, url.get().getId(), new Date());

        try {
          dbMapper.save(sub, new DynamoDBSaveExpression().withExpectedEntry("channelId",
              new ExpectedAttributeValue().withExists(false)));
        } catch (ConditionalCheckFailedException e) {
          log.warn("Subscription already exists!\t{}", sub);
          return ServerlessOutput.builder().statusCode(HttpStatus.SC_BAD_REQUEST)
              .body(mapper.writeValueAsString(new Error("SubscriptonAlreadyExistsException",
                  "A subscription to this channel already exists!")))
              .build();
        }

        channelIdNode.put("channelId", url.get().getId());
        return ServerlessOutput.builder().statusCode(HttpStatus.SC_CREATED)
            .body(mapper.writeValueAsString(channelIdNode)).build();
      } else {
        HttpResponse<InputStream> rsp = fetchFeedFromUrl(url.get().getUrl());
        if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
          log.warn("StatusCode={} from url={}", rsp.getStatus(), url.get().getUrl());
          return ServerlessOutput.builder().statusCode(HttpStatus.SC_BAD_REQUEST)
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
          return ServerlessOutput.builder().statusCode(HttpStatus.SC_BAD_REQUEST)
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
        failed.forEach(batchErr -> log.error("Failed to save item to DynamoDB. channel={}",
            channelId, batchErr.getException()));
        log.trace(feed);


        channelIdNode.put("channelId", channelId);
        return ServerlessOutput.builder().statusCode(HttpStatus.SC_CREATED)
            .body(mapper.writeValueAsString(channelIdNode)).build();
      }
    } catch (Exception e) {
      log.error("", e);
      return ServerlessOutput.builder().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }
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

  Optional<ChannelUrl> transformChannelUrl(String url) throws URISyntaxException, IOException {
    URI uri = ChannelUrl.normalizeUri(url);
    Matcher match;
    switch (uri.getHost()) {
      case "soundcloud.com":
        String htmlElement =
            Jsoup.connect(url).get().select("[content^=soundcloud://users:]").first().toString();
        match = Pattern.compile("soundcloud://users:(\\d+)").matcher(htmlElement);
        if (match.find()) {
          return Optional.of(new ChannelUrl(String.format(
              "http://feeds.soundcloud.com/users/soundcloud:users:%s/sounds.rss", match.group(1))));
        }
        break;
      case "itunes.apple.com":
        match = Pattern.compile("id(\\d+)").matcher(url);
        if (match.find()) {
          URL lookupUrl = new URL(String
              .format("https://itunes.apple.com/lookup?id=%s&entity=podcast", match.group(1)));
          return Optional
              .of(new ChannelUrl(mapper.readTree(lookupUrl).at("/results/0/feedUrl").asText()));
        }
        break;
      default:
        return Optional.of(new ChannelUrl(url));
    }
    return Optional.of(new ChannelUrl(url));
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
