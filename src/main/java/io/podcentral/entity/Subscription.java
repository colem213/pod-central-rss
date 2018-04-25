package io.podcentral.entity;

import java.time.Instant;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;

import io.podcentral.config.TableConstants;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@DynamoDBTable(tableName = TableConstants.Subscription.TABLE_NAME)
public class Subscription {
  @DynamoDBHashKey
  private String userId;
  @DynamoDBRangeKey
  private String channelId;
  @DynamoDBTyped(DynamoDBAttributeType.S)
  @DynamoDBIndexRangeKey(localSecondaryIndexName = TableConstants.Subscription.LSI_DATE_INDEX)
  private Instant subDate;

  public Subscription(String userId, String channelId) {
    this.userId = userId;
    this.channelId = channelId;
    this.subDate = Instant.now();
  }
}
