package io.podcentral.feed;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import io.podcentral.rss.Channel;
import io.podcentral.rss.RssFeed;
import io.podcentral.xml.RssDateTimeAdapter;

public class FeedHandlerUnitTest {
  @Test
  public void shouldParseMandatoryRssChannelFields() throws JAXBException {
    InputStream xmlInput = getClass().getResourceAsStream("/rss/sample-rss.xml");
    RssFeed rss = FeedHandler.parseRss(xmlInput);

    Channel ch = rss.getChannel();
    assertEquals("Scripting News", ch.getTitle());
    assertEquals("http://www.scripting.com/", ch.getLink());
    assertEquals("A weblog about scripting and stuff like that.", ch.getDescription());
  }

  @Test
  public void shouldParseSupportedOptionalRssChannelFields() throws Exception {
    InputStream xmlInput = getClass().getResourceAsStream("/rss/sample-rss.xml");
    RssFeed rss = FeedHandler.parseRss(xmlInput);

    Channel ch = rss.getChannel();
    assertEquals("en-us", ch.getLanguage());
    assertEquals("Copyright 1997-2002 Dave Winer", ch.getCopyright());
    assertEquals(new RssDateTimeAdapter().unmarshal("Sun, 29 Sep 2002 00:00:00 GMT"),
        ch.getPubDate());
    assertEquals(new RssDateTimeAdapter().unmarshal("Mon, 30 Sep 2002 11:00:00 GMT"),
        ch.getLastBuildDate());
    assertEquals("1765", ch.getRssCategory());
    assertEquals(40, (int) ch.getTtl());
    // assertEquals("Scripting News", ch.getSkipHours());
    // assertEquals("Scripting News", ch.getSkipDays());
  }
}
