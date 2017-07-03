package io.podcentral.rss;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import lombok.Data;

@Data
public class Channel {
  private String title;
  private String description;
  private String link;
  private Date pubDate;
  private String language;
  @XmlPath("itunes:image/@href")
  private String imageUrl;
  @XmlPath("itunes:category/@text")
  private String category;
  @XmlElement(name = "explicit", namespace = XmlNs.ITUNES)
  private Boolean explicit;
  @XmlElement(name = "item")
  private List<Item> items;
}
