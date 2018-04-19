package io.podcentral.xml;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

public class DateAdapter extends XmlAdapter<String, Date> {

  public static final String RSS_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
  private final DateTimeFormatter formatter;

  public DateAdapter() {
    super();
    DateTimeParser[] parsers =
        new DateTimeParser[] {DateTimeFormat.forPattern("EEE, dd MMM yy HH:mm:ss z").getParser(),
            DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss z").getParser()};
    formatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter().withZoneUTC();
  }

  @Override
  public String marshal(Date val) throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat(RSS_DATE_FORMAT);
    return sdf.format(val);
  }

  @Override
  public Date unmarshal(String val) throws Exception {
    return formatter.parseDateTime(val).toDate();
  }

}
