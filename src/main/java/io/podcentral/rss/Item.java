package io.podcentral.rss;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.podcentral.config.TableConstants;
import io.podcentral.xml.CsvAdapter;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@DynamoDBTable(tableName = TableConstants.Item.TABLE_NAME)
@Data
public class Item {
  @DynamoDBHashKey
  @DynamoDBAutoGeneratedKey
  private String id;
  @DynamoDBIndexHashKey(globalSecondaryIndexName = TableConstants.Item.GSI_CHANNEL_INDEX)
  private String channelId;
  private String title;
  private String link;
  @XmlPath("itunes:image/@href")
  private String imageUrl;
  private String description;
  @DynamoDBIndexRangeKey(globalSecondaryIndexName = TableConstants.Item.GSI_CHANNEL_INDEX)
  private Date pubDate;
  @XmlElements({@XmlElement(name = "content", namespace = XmlNs.MEDIA),
      @XmlElement(name = "enclosure")})
  private List<MediaContent> media;
  @XmlJavaTypeAdapter(CsvAdapter.class)
  @XmlElement(namespace = XmlNs.MEDIA)
  private List<String> keywords;
}
