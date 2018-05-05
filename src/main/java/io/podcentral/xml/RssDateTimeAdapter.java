package io.podcentral.xml;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class RssDateTimeAdapter extends XmlAdapter<String, Instant> {
  @Override
  public String marshal(Instant val) throws Exception {
    return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.of("UTC"))
        .format(val);
  }

  @Override
  public Instant unmarshal(String val) throws Exception {
    return DateTimeFormatter.ofPattern("EEE, dd MMM [yyyy][yy] HH:mm:ss [Z][z]")
        .withZone(ZoneId.of("UTC")).parse(val, Instant::from);
  }
}
