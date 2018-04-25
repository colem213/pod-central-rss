package io.podcentral.apigw;

import static org.picocontainer.Characteristics.CACHE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;

import org.jsoup.Jsoup;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.picocontainer.PicoContainer;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.podcentral.config.EnvTableNameResolver;
import io.podcentral.entity.ChannelUrl;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.model.ServerlessOutput.ServerlessOutputBuilder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ApiProxyController implements RequestHandler<ServerlessInput, ServerlessOutput> {
  HttpResponse<InputStream> mockRsp;
  private static JAXBContext CTX;
  private final MutablePicoContainer container;

  public static final String DYNAMO_ENDPOINT = "DYNAMO_ENDPOINT";

  public ApiProxyController() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new ISO8601DateFormat());

    container = new PicoBuilder().withConstructorInjection().build();
    container.as(CACHE).addComponent(PicoContainer.class, container);
    container.as(CACHE).addComponent(mapper);
    container.as(CACHE).addComponent(getDynamoDbClient());
    container.addComponent(ServerlessOutputBuilder.class);
  }

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    MutablePicoContainer req = new PicoBuilder(container).withConstructorInjection().build();
    req.addComponent(input);
    req.addComponent(context);
    req.addComponent(getDynamoDbMapper(input.getStageVariables()));
    req.addComponent(SubscriptionController.class);
    switch (input.getPathParameters().getOrDefault("proxy", "")) {
      case "subscribe":
        return req.getComponent(SubscriptionController.class).post();
    }
    return ServerlessOutput.notFound();
  }

  public AmazonDynamoDB getDynamoDbClient() {
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

  public DynamoDBMapper getDynamoDbMapper(Map<String, String> stageVariables) {
    AmazonDynamoDB client = container.getComponent(AmazonDynamoDB.class);
    DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTableNameResolver(new EnvTableNameResolver(stageVariables)).build();
    return new DynamoDBMapper(client, config);
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
          return Optional.of(new ChannelUrl(container.getComponent(ObjectMapper.class)
              .readTree(lookupUrl).at("/results/0/feedUrl").asText()));
        }
        break;
      default:
        return Optional.of(new ChannelUrl(url));
    }
    return Optional.of(new ChannelUrl(url));
  }

  // public static void main(String[] args) {
  // DynamoDBMapper mapper = EnvConfig.getDynamoDbMapper();
  // AmazonDynamoDB client = EnvConfig.getDynamoDbClient();
  //
  // Reflections reflections = new Reflections("io.podcentral");
  // Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DynamoDBTable.class);
  //
  // ProvisionedThroughput thruPut = new ProvisionedThroughput(3L, 3L);
  // for (Class<?> table : annotated) {
  // CreateTableRequest tableReq = mapper.generateCreateTableRequest(table);
  // tableReq.setProvisionedThroughput(thruPut);
  // if (tableReq.getGlobalSecondaryIndexes() != null) {
  // tableReq.getGlobalSecondaryIndexes().forEach(idx -> idx.setProvisionedThroughput(thruPut));
  // }
  // client.createTable(tableReq);
  // }
  // }
}
