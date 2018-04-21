package io.podcentral.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import io.podcentral.rss.Channel;
import io.podcentral.rss.Item;
import io.podcentral.rss.MediaContent;
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
    assertEquals("http://chexed.com/images/chexed.gif", ch.getRssImageUrl());
    assertEquals("Chexed.com", ch.getRssImageTitle());
    assertEquals("http://www.chexed.com/", ch.getRssImageLink());
    // assertEquals("Scripting News", ch.getSkipHours());
    // assertEquals("Scripting News", ch.getSkipDays());
  }

  @Test
  public void shouldParseSupportedRssItemFields() throws Exception {
    InputStream xmlInput = getClass().getResourceAsStream("/rss/sample-rss.xml");
    RssFeed rss = FeedHandler.parseRss(xmlInput);

    Item item = rss.getChannel().getItems().get(0);
    MediaContent enclosure = item.getEnclosure();
    assertEquals("Scripting News", item.getTitle());
    assertEquals("http://scriptingnews.userland.com/", item.getLink());
    assertTrue(item.getDescription() != null);
    assertEquals("Dave Winer", item.getAuthor());
    assertEquals("Tech", item.getCategory());
    assertEquals("http://scriptingnews.userland.com/backissues/2002/09/29#When:6:56:02PM/comments",
        item.getComments());
    assertEquals("http://www.scripting.com/mp3s/weatherReportSuite.mp3", enclosure.getUrl());
    assertEquals(12216320, enclosure.getLength());
    assertEquals("audio/mpeg", enclosure.getType());
    assertEquals("http://scriptingnews.userland.com/backissues/2002/09/29#When:6:56:02PM",
        item.getGuid());
    assertEquals(new RssDateTimeAdapter().unmarshal("Mon, 30 Sep 2002 01:56:02 GMT"),
        item.getPubDate());
    assertEquals("http://scriptingnews.userland.com/.rss", item.getSourceUrl());
  }
}
