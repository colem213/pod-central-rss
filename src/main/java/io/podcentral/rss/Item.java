package io.podcentral.rss;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import io.podcentral.xml.CsvAdapter;
import lombok.Data;

@Data
public class Item {
  private String title;
  private String link;
  @XmlPath("itunes:image/@href")
  private String imageUrl;
  private String description;
  private Date pubDate;
  @XmlPath("media:content[@medium='audio']/@url")
  private List<String> mediaUrls;
  @XmlPath("media:content[@medium='image']/@url")
  private List<String> imageUrls;
  @XmlJavaTypeAdapter(CsvAdapter.class)
  @XmlElement(namespace = XmlNs.MEDIA)
  private List<String> keywords;
}
