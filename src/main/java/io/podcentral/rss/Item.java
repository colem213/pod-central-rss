package io.podcentral.rss;

import java.time.Instant;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import io.podcentral.aws.InstantConverter;
import io.podcentral.config.TableConstants;
import io.podcentral.xml.CsvAdapter;
import io.podcentral.xml.ItunesDurationAdapter;
import io.podcentral.xml.ItunesExplicitAdapter;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@DynamoDBTable(tableName = TableConstants.Entry.TABLE_NAME)
@Data
public class Item {
  @DynamoDBHashKey
  @DynamoDBAutoGeneratedKey
  private String id;
  @DynamoDBIndexHashKey(globalSecondaryIndexName = TableConstants.Entry.GSI_FEED_INDEX)
  private String feedId;
  private String title;
  private String link;
  private String description;
  private String author;
  private String category;
  private String comments;
  private MediaContent enclosure;
  private String guid;
  @XmlPath("source/@url/text()")
  private String sourceUrl;
  @DynamoDBTypeConverted(converter = InstantConverter.class)
  @DynamoDBIndexRangeKey(globalSecondaryIndexName = TableConstants.Entry.GSI_FEED_INDEX)
  private Instant pubDate;

  @XmlPath("itunes:author/text()")
  private String itunesAuthor;
  @XmlPath("itunes:block/text()")
  private Boolean itunesBlock;
  @XmlPath("itunes:image/@href")
  private String itunesImageUrl;
  @XmlJavaTypeAdapter(ItunesDurationAdapter.class)
  @XmlPath("itunes:duration/text()")
  private String itunesDuration;
  @XmlJavaTypeAdapter(ItunesExplicitAdapter.class)
  @XmlElement(name = "explicit", namespace = XmlNs.ITUNES)
  private Boolean itunesExplicit;
  @XmlElement(name = "isClosedCaptioned", namespace = XmlNs.ITUNES)
  private Boolean itunesIsClosedCaptioned;
  @XmlElement(name = "order", namespace = XmlNs.ITUNES)
  private Integer itunesOrder;
  @XmlElement(name = "subtitle", namespace = XmlNs.ITUNES)
  private String itunesSubtitle;
  @XmlElement(name = "summary", namespace = XmlNs.ITUNES)
  private String itunesSummary;
  @XmlElement(name = "content", namespace = XmlNs.MEDIA)
  private List<MediaContent> media;
  @XmlJavaTypeAdapter(CsvAdapter.class)
  @XmlElement(namespace = XmlNs.MEDIA)
  private List<String> keywords;
}
