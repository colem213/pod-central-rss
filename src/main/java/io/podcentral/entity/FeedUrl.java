package io.podcentral.entity;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.podcentral.config.TableConstants;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@DynamoDBTable(tableName = TableConstants.ChannelUrl.TABLE_NAME)
public class FeedUrl {
  @DynamoDBHashKey
  private String url;
  @DynamoDBIndexHashKey(globalSecondaryIndexName = TableConstants.ChannelUrl.GSI_CHANNEL_INDEX)
  private String id;

  public FeedUrl(String url) throws MalformedURLException, URISyntaxException {
    setUrl(url);
  }

  public FeedUrl(String url, String feedId) throws MalformedURLException, URISyntaxException {
    setUrl(url);
    this.id = feedId;
  }

  public void setUrl(String url) throws MalformedURLException, URISyntaxException {
    this.url = FeedUrl.normalizeUrl(url);
  }

  public static URI normalizeUri(String url) throws URISyntaxException, MalformedURLException {
    URI uri = new URI(url);
    if (uri.isOpaque()) {
      throw new IllegalArgumentException("URL must be server-based!");
    }
    uri = uri.normalize();
    int defaultPort = uri.toURL().getDefaultPort();
    int port = uri.getPort() == -1 || uri.getPort() == defaultPort ? -1 : uri.getPort();
    return new URI(uri.getScheme(), null, uri.getHost(), port, uri.getPath(), null, null);
  }

  public static String normalizeUrl(String url) throws URISyntaxException, MalformedURLException {
    return normalizeUri(url).toString();
  }
}
