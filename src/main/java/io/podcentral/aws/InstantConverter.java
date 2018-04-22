package io.podcentral.aws;

import java.time.Instant;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public class InstantConverter implements DynamoDBTypeConverter<String, Instant> {
  @Override
  public String convert(Instant instant) {
    return instant.toString();
  }

  @Override
  public Instant unconvert(String text) {
    return Instant.parse(text);
  }
}
