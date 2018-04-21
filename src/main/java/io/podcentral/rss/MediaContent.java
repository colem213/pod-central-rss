package io.podcentral.rss;

import javax.xml.bind.annotation.XmlAttribute;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Data;

@DynamoDBDocument
@Data
public class MediaContent {
  @XmlAttribute
  private String url;
  @XmlAttribute
  private String type;
  @XmlAttribute
  private String medium;
  @XmlAttribute
  private String duration;
  @XmlAttribute
  private int length;
  @XmlAttribute
  private String lang;
}
