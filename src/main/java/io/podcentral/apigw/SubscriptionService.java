package io.podcentral.apigw;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.*;
import static org.asynchttpclient.Dsl.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.extras.rxjava2.RxHttpClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.podcentral.config.TableConstants.Feed;
import io.podcentral.entity.UserFeed;
import io.podcentral.feed.FeedHandler;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.model.ServerlessOutput.ServerlessOutputBuilder;
import io.podcentral.rss.Channel;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
public class SubscriptionService {

  private ServerlessInput in;
  private ServerlessOutputBuilder out;
  private Context context;
  private ObjectMapper mapper;
  private DynamoDB dynamo;
  private DynamoDBMapper dbMapper;

  public ServerlessOutput subscribe() {
    if (!"post".equalsIgnoreCase(in.getHttpMethod())) {
      return ServerlessOutput.invalidMethod();
    }
    if (context.getIdentity() == null || context.getIdentity().getIdentityId() == null
        || context.getIdentity().getIdentityId().isEmpty()) {
      return ServerlessOutput.unauthorized();
    }

    List<String> feedUrls;
    try {
      feedUrls = mapper.readValue(in.getBody(),
          mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (IOException e) {
      return ServerlessOutput.badRequest("InvalidRequestBodyException",
          "The request body is unable to be processed");
    }

    List<String> feeds = new ArrayList<>();
    Flowable.fromIterable(feedUrls).parallel().runOn(Schedulers.computation())
        .map(url -> SubscriptionService.normalizeUri(new URI(url)).toString()).flatMap(url -> {
          return Flowable.just(url).map(this::findFeedByUri).flatMap(id -> {
            if (id.isPresent())
              return Flowable.just(id.get());
            else
              return Flowable.just(url)
                  .flatMapMaybe(__ -> RxHttpClient.create(asyncHttpClient())
                      .prepare(new RequestBuilder("HEAD").setUrl(url).build()))
                  .filter(rsp -> rsp.getContentType().contains("rss"))
                  .flatMapMaybe(__ -> RxHttpClient.create(asyncHttpClient())
                      .prepare(new RequestBuilder("GET").setUrl(url).build()))
                  .map(rsp -> rsp.getResponseBodyAsStream()).map(FeedHandler::parseRss)
                  .map(rss -> rss.getChannel()).map(this::save).map(feed -> feed.getId());
          });
        }).sequential().blockingSubscribe(feeds::add, log::error);
    return out.body(feeds).build();
  }

  public Optional<String> findFeedByUri(String url) {
    QueryExpressionSpec keyExprSpec =
        new ExpressionSpecBuilder().withKeyCondition(S("feedUrl").eq(url)).buildForQuery();
    QuerySpec qSpec =
        new QuerySpec().withProjectionExpression("id").withExpressionSpec(keyExprSpec);
    ItemCollection<QueryOutcome> results =
        dynamo.getTable(Feed.TABLE_NAME).getIndex(Feed.GSI_URI_INDEX).query(qSpec);
    IteratorSupport<Item, QueryOutcome> it = results.iterator();
    return it.hasNext() ? Optional.ofNullable(it.next().getString("id")) : Optional.empty();
  }

  public Channel save(Channel feed) {
    final String channelId = UUID.randomUUID().toString();
    final String userId = context.getIdentity().getIdentityId();
    UserFeed sub = new UserFeed(userId, channelId);
    feed.setId(channelId);
    feed.getEntries().stream().forEach(entry -> entry.setFeedId(channelId));

    List<Object> toSave = new ArrayList<Object>(Arrays.asList(sub, feed));
    toSave.addAll(feed.getEntries());
    List<FailedBatch> failed = dbMapper.batchSave(toSave);
    failed.forEach(batchErr -> log.error("Failed to save item to DynamoDB. channel={}", channelId,
        batchErr.getException()));
    log.trace(feed);

    return feed;
  }

  public static URI normalizeUri(URI uri) throws URISyntaxException, MalformedURLException {
    if (uri.isOpaque()) {
      throw new IllegalArgumentException("URL must be server-based!");
    }
    uri = uri.normalize();
    int defaultPort = uri.toURL().getDefaultPort();
    int port = uri.getPort() == -1 || uri.getPort() == defaultPort ? -1 : uri.getPort();
    return new URI(uri.getScheme(), null, uri.getHost(), port, uri.getPath(), null, null);
  }

}
