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
public class ChannelUrl {
  @DynamoDBHashKey
  private String url;
  @DynamoDBIndexHashKey(globalSecondaryIndexName = TableConstants.ChannelUrl.GSI_CHANNEL_INDEX)
  private String id;

  public ChannelUrl(String url) throws MalformedURLException, URISyntaxException {
    setUrl(url);
  }

  public ChannelUrl(String url, String channelId) throws MalformedURLException, URISyntaxException {
    setUrl(url);
    this.id = channelId;
  }

  public void setUrl(String url) throws MalformedURLException, URISyntaxException {
    this.url = ChannelUrl.normalizeUrl(url);
  }

  public static String normalizeUrl(String url) throws URISyntaxException, MalformedURLException {
    URI uri = new URI(url);
    if (uri.isOpaque()) {
      throw new IllegalArgumentException("URL must be server-based!");
    }
    uri = uri.normalize();
    int defaultPort = uri.toURL().getDefaultPort();
    int port = uri.getPort() == -1 || uri.getPort() == defaultPort ? -1 : uri.getPort();
    uri = new URI(uri.getScheme(), null, uri.getHost(), port, uri.getPath(), null, null);
    return uri.toString();
  }
}