package io.podcentral.entity;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.podcentral.config.TableConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDBTable(tableName = TableConstants.Subscription.TABLE_NAME)
public class Subscription {
  @DynamoDBHashKey
  private String userId;
  @DynamoDBRangeKey
  private String channelId;
  @DynamoDBIndexRangeKey(localSecondaryIndexName = TableConstants.Subscription.LSI_DATE_INDEX)
  private Date subDate;
}
