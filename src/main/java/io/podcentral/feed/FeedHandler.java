package io.podcentral.feed;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import io.podcentral.feed.exception.RssParseException;
import io.podcentral.rss.RssFeed;

public class FeedHandler {
  private static JAXBContext rssJaxb;

  private static JAXBContext getRssJaxbContext() {
    if (rssJaxb == null) {
      synchronized (FeedHandler.class) {
        if (rssJaxb == null) {
          try {
            rssJaxb = JAXBContext.newInstance(RssFeed.class);
          } catch (JAXBException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return rssJaxb;
  }

  public static RssFeed parseRss(InputStream xmlInput) {
    try {
      return getRssJaxbContext().createUnmarshaller()
          .unmarshal(new StreamSource(xmlInput), RssFeed.class).getValue();
    } catch (JAXBException e) {
      throw new RssParseException(e);
    }
  }

}
