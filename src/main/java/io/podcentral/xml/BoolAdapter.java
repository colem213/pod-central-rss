package io.podcentral.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BoolAdapter extends XmlAdapter<String, Boolean> {
  @Override
  public String marshal(Boolean val) throws Exception {
    return val ? "yes" : "no";
  }

  @Override
  public Boolean unmarshal(String val) throws Exception {
    return "yes".equalsIgnoreCase(val);
  }
}
