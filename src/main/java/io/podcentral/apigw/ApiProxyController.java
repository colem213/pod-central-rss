package io.podcentral.apigw;

import javax.xml.bind.JAXBContext;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.podcentral.config.DepInjectionConfig;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ApiProxyController implements RequestHandler<ServerlessInput, ServerlessOutput> {
  private static JAXBContext CTX;
  private final DefaultPicoContainer container;

  public ApiProxyController() {
    this(DepInjectionConfig.defaultConfig());
  }

  public ApiProxyController(DefaultPicoContainer container) {
    this.container = container;
  }

  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    MutablePicoContainer req = new DefaultPicoContainer(container);
    req.addComponent(input);
    req.addComponent(context);
    req.addComponent(DepInjectionConfig.getDynamoDbMapper(req.getComponent(AmazonDynamoDB.class),
        input.getStageVariables()));
    req.addComponent(SubscriptionService.class);
    switch (input.getPathParameters().getOrDefault("proxy", "")) {
      case "subscribe":
        return req.getComponent(SubscriptionService.class).subscribe();
    }
    return ServerlessOutput.notFound();
  }

  // Optional<FeedUrl> transformChannelUrl(String url) throws URISyntaxException, IOException {
  // URI uri = FeedUrl.normalizeUri(url);
  // Matcher match;
  // switch (uri.getHost()) {
  // case "soundcloud.com":
  // String htmlElement =
  // Jsoup.connect(url).get().select("[content^=soundcloud://users:]").first().toString();
  // match = Pattern.compile("soundcloud://users:(\\d+)").matcher(htmlElement);
  // if (match.find()) {
  // return Optional.of(new FeedUrl(String.format(
  // "http://feeds.soundcloud.com/users/soundcloud:users:%s/sounds.rss", match.group(1))));
  // }
  // break;
  // case "itunes.apple.com":
  // match = Pattern.compile("id(\\d+)").matcher(url);
  // if (match.find()) {
  // URL lookupUrl = new URL(String
  // .format("https://itunes.apple.com/lookup?id=%s&entity=podcast", match.group(1)));
  // return Optional.of(new FeedUrl(container.getComponent(ObjectMapper.class)
  // .readTree(lookupUrl).at("/results/0/feedUrl").asText()));
  // }
  // break;
  // default:
  // return Optional.of(new FeedUrl(url));
  // }
  // return Optional.of(new FeedUrl(url));
  // }
}
