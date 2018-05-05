package io.podcentral.xml;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class RssDateTimeAdapterUnitTest {
  DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("EEE, dd MMM [yyyy][yy] HH:mm:ss [Z][z]").withZone(ZoneId.of("UTC"));

  @Test
  public void shouldParseTwoDigitYear() throws Exception {
    String expectedDateStr = "Mon, 30 Sep 02 11:00:00 GMT";

    Instant actualDate = new RssDateTimeAdapter().unmarshal(expectedDateStr);

    Instant expectedDate = formatter.parse(expectedDateStr, Instant::from);
    assertEquals(expectedDate, actualDate);
  }

  @Test
  public void shouldParseFourDigitYear() throws Exception {
    String expectedDateStr = "Mon, 30 Sep 2002 11:00:00 GMT";

    Instant actualDate = new RssDateTimeAdapter().unmarshal(expectedDateStr);

    Instant expectedDate = formatter.parse(expectedDateStr, Instant::from);
    assertEquals(expectedDate, actualDate);
  }

  @Test
  public void shouldFormatRssDate() throws Exception {
    String expectedDateStr = "Mon, 30 Sep 2002 11:00:00 GMT";
    Instant expectedDate = formatter.parse(expectedDateStr, Instant::from);

    String actualDateStr = new RssDateTimeAdapter().marshal(expectedDate);

    assertEquals(expectedDateStr.replaceAll("GMT", "UTC"), actualDateStr);
  }

  @Test
  public void shouldFormatRssDateWithHourOffset() throws Exception {
    String expectedDateStr = "Thu, 03 May 2018 21:58:26 +0000";
    Instant actualDate = new RssDateTimeAdapter().unmarshal(expectedDateStr);

    Instant expectedDate = formatter.parse(expectedDateStr, Instant::from);
    assertEquals(expectedDate, actualDate);
  }
}
