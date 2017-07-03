package io.podcentral.rss;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "rss")
public class RssFeed {
  private Channel channel;
}
