package io.podcentral.model;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.podcentral.TableConstants;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@DynamoDBTable(tableName = TableConstants.Subscription.TABLE_NAME)
public class Subscription {
  @DynamoDBHashKey
  private String userId;
  @DynamoDBIndexRangeKey(localSecondaryIndexName = TableConstants.Subscription.LSI_CHANNEL_INDEX)
  private String channelId;
  @DynamoDBRangeKey
  private Date subDate;
}
