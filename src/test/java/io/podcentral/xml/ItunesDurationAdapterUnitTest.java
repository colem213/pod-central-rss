package io.podcentral.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.podcentral.feed.exception.UnsupportedItunesDurationFormatException;

public class ItunesDurationAdapterUnitTest {
  @Test(expected = UnsupportedItunesDurationFormatException.class)
  public void shouldThrowFormatException() throws Exception {
    new ItunesDurationAdapter().unmarshal("10:15:3223");
  }

  @Test
  public void ignoreExtraTimeParts() throws Exception {
    assertEquals("10:15:32", new ItunesDurationAdapter().unmarshal("10:15:32:23:24:12"));
  }

  @Test
  public void parseLongHoursFormat() throws Exception {
    String expectedDurationStr = "10:15:32";
    assertEquals(expectedDurationStr, new ItunesDurationAdapter().unmarshal(expectedDurationStr));
  }

  @Test
  public void parseShortHoursFormat() throws Exception {
    String expectedDurationStr = "1:15:32";
    assertEquals(expectedDurationStr, new ItunesDurationAdapter().unmarshal(expectedDurationStr));
  }

  @Test
  public void parseLongMinutesFormat() throws Exception {
    String expectedDurationStr = "15:32";
    assertEquals(expectedDurationStr, new ItunesDurationAdapter().unmarshal(expectedDurationStr));
  }

  @Test
  public void parseShortMinutesFormat() throws Exception {
    String expectedDurationStr = "5:32";
    assertEquals(expectedDurationStr, new ItunesDurationAdapter().unmarshal(expectedDurationStr));
  }

  @Test
  public void parseSecondsFormat() throws Exception {
    String expectedDurationStr = "2534905";
    assertEquals(expectedDurationStr, new ItunesDurationAdapter().unmarshal(expectedDurationStr));
  }

  @Test
  public void hoursWithLeadingZero() throws Exception {
    assertEquals("05:12:11", new ItunesDurationAdapter().unmarshal("05:12:11"));
  }

  @Test
  public void hoursWithNonLeadingZero() throws Exception {
    assertEquals("10:12:11", new ItunesDurationAdapter().unmarshal("10:12:11"));
  }

  @Test
  public void trimHourIfZeroes() throws Exception {
    assertEquals("12:11", new ItunesDurationAdapter().unmarshal("00:12:11"));
  }

  @Test
  public void minutesWithLeadingZero() throws Exception {
    assertEquals("09:11", new ItunesDurationAdapter().unmarshal("09:11"));
  }

  @Test
  public void minutesWithNonLeadingZero() throws Exception {
    assertEquals("40:11", new ItunesDurationAdapter().unmarshal("40:11"));
  }

  @Test
  public void trimHourAndMinutesIfZeroes() throws Exception {
    assertEquals("11", new ItunesDurationAdapter().unmarshal("00:00:11"));
  }

  @Test
  public void trimMinutesIfZeroes() throws Exception {
    assertEquals("11", new ItunesDurationAdapter().unmarshal("00:11"));
  }
}
