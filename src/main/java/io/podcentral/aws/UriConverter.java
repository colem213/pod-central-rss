package io.podcentral.aws;

import java.net.URI;
import java.net.URISyntaxException;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public class UriConverter implements DynamoDBTypeConverter<String, URI> {

  @Override
  public String convert(URI uri) {
    return uri.toString();
  }

  @Override
  public URI unconvert(String string) {
    try {
      return new URI(string);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
