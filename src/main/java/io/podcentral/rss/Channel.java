package io.podcentral.rss;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.podcentral.TableConstants;
import lombok.Data;

@DynamoDBTable(tableName = TableConstants.Channel.TABLE_NAME)
@Data
public class Channel {
  @DynamoDBHashKey
  @DynamoDBAutoGeneratedKey
  private String id;
  private String title;
  private String description;
  private String link;
  @DynamoDBIndexHashKey(globalSecondaryIndexName = TableConstants.Channel.GSI_URL_INDEX)
  @DynamoDBIndexRangeKey(localSecondaryIndexName = TableConstants.Channel.LSI_URL_INDEX)
  @XmlPath("atom:link[@rel='self']/@href")
  private String feedUrl;
  @DynamoDBRangeKey
  private Date pubDate;
  private String language;
  @XmlPath("itunes:image/@href")
  private String imageUrl;
  @XmlPath("itunes:category/@text")
  private String category;
  @XmlElement(name = "explicit", namespace = XmlNs.ITUNES)
  private Boolean explicit;
  @DynamoDBIgnore
  @XmlElement(name = "item")
  private List<Item> items;
}
