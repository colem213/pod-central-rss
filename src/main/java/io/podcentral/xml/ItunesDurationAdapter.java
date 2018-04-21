package io.podcentral.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import io.podcentral.feed.exception.UnsupportedItunesDurationFormatException;

public class ItunesDurationAdapter extends XmlAdapter<String, String> {
  @Override
  public String marshal(String val) throws Exception {
    return val;
  }

  @Override
  public String unmarshal(String val) throws Exception {
    if (val == null)
      return null;
    if (val.matches("\\d{1,2}:\\d{2}:\\d{2}(:\\d{2})*")) {
      String[] timeParts = val.split(":");
      if (timeParts[0].replaceAll("^0*", "").length() > 0) {
        return timeParts[0] + ":" + timeParts[1] + ":" + timeParts[2];
      } else {
        return unmarshal(timeParts[1] + ":" + timeParts[2]);
      }
    } else if (val.matches("\\d{1,2}:\\d{2}")) {
      String[] timeParts = val.split(":");
      if (timeParts[0].replaceAll("^0*", "").length() > 0) {
        return timeParts[0] + ":" + timeParts[1];
      } else {
        return timeParts[1];
      }
    } else if (val.matches("\\d+")) {
      return val;
    }
    throw new UnsupportedItunesDurationFormatException(val);
  }
}
