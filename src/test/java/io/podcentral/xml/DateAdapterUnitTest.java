package io.podcentral.xml;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

public class DateAdapterUnitTest {
  @Test
  public void shouldParseTwoDigitYear() throws Exception {
    String expectedDateStr = "Mon, 30 Sep 02 11:00:00 GMT";

    Date actualDate = new DateAdapter().unmarshal(expectedDateStr);

    DateTimeFormatter dtf = DateTimeFormat.forPattern("EEE, dd MMM yy HH:mm:ss z").withZoneUTC();
    DateTime dt = dtf.parseDateTime(expectedDateStr);
    assertEquals(dt.toDate(), actualDate);
  }

  @Test
  public void shouldParseFourDigitYear() throws Exception {
    String expectedDateStr = "Mon, 30 Sep 2002 11:00:00 GMT";

    Date actualDate = new DateAdapter().unmarshal(expectedDateStr);

    DateTimeFormatter dtf = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss z").withZoneUTC();
    DateTime dt = dtf.parseDateTime(expectedDateStr);
    assertEquals(dt.toDate(), actualDate);
  }
}
