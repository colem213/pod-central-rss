package io.podcentral.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BoolAdapter extends XmlAdapter<String, Boolean> {
  @Override
  public String marshal(Boolean val) throws Exception {
    if (val == null)
      return null;
    return val ? "Yes" : "No";
  }

  @Override
  public Boolean unmarshal(String val) throws Exception {
    if (val == null)
      return null;
    return "yes".equalsIgnoreCase(val);
  }
}
