package io.podcentral.model;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.Data;

@DynamoDBTable(tableName = "Subscription")
@Data
public class Subscription {
  @DynamoDBHashKey(attributeName = "userId")
  private String userId;
  @DynamoDBIndexRangeKey(localSecondaryIndexName = "ChannelRangeIndex")
  private String channelId;
  @DynamoDBRangeKey
  private Date subDate;
}
