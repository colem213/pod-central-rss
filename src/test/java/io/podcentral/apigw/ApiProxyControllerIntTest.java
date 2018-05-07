package io.podcentral.apigw;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.extras.rxjava2.RxHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.picocontainer.DefaultPicoContainer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.podcentral.TestUtil;
import io.podcentral.config.TableConstants.Entry;
import io.podcentral.config.TableConstants.Feed;
import io.podcentral.config.TestDepInjectionConfig;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.reactivex.Maybe;

@PrepareForTest({RxHttpClient.class, Dsl.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.xml.*", "com.sun.*", "org.eclipse.persistence.*",
    "*.ssl.*", "*.crypto.*", "io.podcentral.xml.*", "org.asynchttpclient.SslEngineFactory",
    "org.asynchttpclient.netty.*"})
public class ApiProxyControllerIntTest {
  private ObjectMapper mapper;
  private DynamoDBMapper dbMapper;
  private AmazonDynamoDB client;
  private DynamoDB dynamo;
  private Reflections reflect;
  private DefaultPicoContainer container;

  @Mock
  Context context;
  @Mock
  CognitoIdentity ident;
  @Mock
  RxHttpClient httpClient;

  public ApiProxyControllerIntTest() {
    container = TestDepInjectionConfig.defaultConfig();
    mapper = container.getComponent(ObjectMapper.class);
    client = container.getComponent(AmazonDynamoDB.class);
    dbMapper = container.getComponent(DynamoDBMapper.class);
    dynamo = container.getComponent(DynamoDB.class);
    reflect = new Reflections(new ConfigurationBuilder().forPackages("io.podcentral")
        .filterInputsBy(str -> str.endsWith(".class")));
    PowerMockito.mockStatic(RxHttpClient.class);
    PowerMockito.mockStatic(Dsl.class);
  }

  @Test
  public void parseAndSaveRssFeed() throws JsonParseException, JsonMappingException, IOException {
    when(ident.getIdentityId()).thenReturn("test");
    when(RxHttpClient.create(any())).thenReturn(httpClient);
    when(Dsl.asyncHttpClient()).thenReturn(null);

    mockHttpResponse("HEAD", "application/rss+xml");
    mockHttpResponse("GET", "", TestUtil.loadInputStreamFromClasspath("rss/sample-rss.xml"));

    ServerlessInput in = ServerlessInput.builder().pathParameter("proxy", "subscribe")
        .httpMethod("post").body("[\"http://undisclosed-podcast.com\"]").build();

    ServerlessOutput out = new ApiProxyController(container).handleRequest(in, context);

    List<String> ids = mapper.readValue(out.getBody(),
        mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    assertEquals(1, ids.size());
    Optional<Item> rsp = findFeed(ids.get(0));
    assertTrue(rsp.isPresent());
    Item feed = rsp.get();
    assertEquals("ITunes Owner", feed.getString("ownerName"));
    assertEquals("ITunes Author", feed.getString("author"));
    assertEquals("http://example.com/podcasts/everything/AllAboutEverything.jpg",
        feed.getString("imageUrl"));
    assertEquals("http://www.scripting.com/", feed.getString("link"));
    assertNotNull(feed.getString("description"));
    assertEquals("en-us", feed.getString("language"));
    assertNotNull(feed.getString("id"));
    // assertNotNull(feed.getMap("categories"));
    // assertEquals("News & Politics", feed.getMap("categories").get("name"));
    assertEquals("Scripting News", feed.getString("title"));
    assertEquals("2002-09-29T00:00:00Z", feed.getString("pubDate"));
    assertEquals("itunes@owner.email", feed.getString("ownerEmail"));

    rsp = findEntryByFeed(feed.getString("id"));
    assertTrue(rsp.isPresent());
    Item entry = rsp.get();
    assertEquals("http://scriptingnews.userland.com/backissues/2002/09/29#When:6:56:02PM/comments",
        entry.getString("comments"));
    assertEquals("ITunes Entry Author", entry.getString("author"));
    assertEquals("http://scriptingnews.userland.com/", entry.getString("link"));
    assertEquals("Entry Summary", entry.getString("description"));
    assertEquals(1, entry.getInt("closedCaptioned"));
    assertEquals("Scripting News", entry.getString("title"));
    assertEquals("2002-09-30T01:56:02Z", entry.getString("pubDate"));
    assertEquals(1, entry.getInt("explicit"));
    assertEquals("07:04", entry.getString("duration"));
    assertNotNull(entry.getList("media"));
    Map<String, Object> media = (Map<String, Object>) entry.getList("media").get(0);
    assertEquals("http://www.scripting.com/mp3s/weatherReportSuite.mp3", media.get("url"));
    assertEquals("audio/mpeg", media.get("type"));
    assertEquals(new BigDecimal("12216320"), media.get("length"));
  }

  void mockHttpResponse(String httpMethod, String contentType) {
    mockHttpResponse(httpMethod, contentType, null);
  }

  void mockHttpResponse(String httpMethod, String contentType, InputStream is) {
    Response rsp = mock(Response.class);
    when(rsp.getContentType()).thenReturn(contentType);
    if (is != null)
      when(rsp.getResponseBodyAsStream()).thenReturn(is);
    when(httpClient.prepare(argThat(new ArgumentMatcher<Request>() {
      @Override
      public boolean matches(Object arg) {
        if (!(arg instanceof Request))
          return false;
        return ((Request) arg).getMethod().equalsIgnoreCase(httpMethod);
      }
    }))).thenReturn(Maybe.just(rsp));
  }

  public Optional<Item> findFeed(String id) {
    QueryExpressionSpec keyExprSpec =
        new ExpressionSpecBuilder().withKeyCondition(S("id").eq(id)).buildForQuery();
    QuerySpec qSpec = new QuerySpec().withExpressionSpec(keyExprSpec);
    ItemCollection<QueryOutcome> results = dynamo.getTable(Feed.TABLE_NAME).query(qSpec);
    IteratorSupport<Item, QueryOutcome> it = results.iterator();
    return it.hasNext() ? Optional.ofNullable(it.next()) : Optional.empty();
  }

  public Optional<Item> findEntryByFeed(String id) {
    QueryExpressionSpec keyExprSpec =
        new ExpressionSpecBuilder().withKeyCondition(S("feedId").eq(id)).buildForQuery();
    QuerySpec qSpec =
        new QuerySpec().withSelect(Select.ALL_ATTRIBUTES).withExpressionSpec(keyExprSpec);
    ItemCollection<QueryOutcome> results =
        dynamo.getTable(Entry.TABLE_NAME).getIndex(Entry.GSI_FEED_INDEX).query(qSpec);
    IteratorSupport<Item, QueryOutcome> it = results.iterator();
    return it.hasNext() ? Optional.ofNullable(it.next()) : Optional.empty();
  }

  @Before
  public void setUp() {
    when(context.getIdentity()).thenReturn(ident);

    Set<Class<?>> annotated = reflect.getTypesAnnotatedWith(DynamoDBTable.class);

    ProvisionedThroughput thruPut = new ProvisionedThroughput(3L, 3L);
    for (Class<?> table : annotated) {
      CreateTableRequest tableReq = dbMapper.generateCreateTableRequest(table);
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
