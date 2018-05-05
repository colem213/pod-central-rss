package io.podcentral.entity;

import java.time.Instant;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import io.podcentral.aws.InstantConverter;
import io.podcentral.config.TableConstants;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@DynamoDBTable(tableName = TableConstants.UserFeed.TABLE_NAME)
public class UserFeed {
  @DynamoDBHashKey
  private String userId;
  @DynamoDBRangeKey
  private String feedId;
  @DynamoDBTypeConverted(converter = InstantConverter.class)
  @DynamoDBIndexRangeKey(localSecondaryIndexName = TableConstants.UserFeed.LSI_DATE_INDEX)
  private Instant subDate;

  public UserFeed(String userId, String channelId) {
    this.userId = userId;
    this.feedId = channelId;
    this.subDate = Instant.now();
  }
}
