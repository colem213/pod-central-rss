package io.podcentral.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class StringAdapter extends XmlAdapter<String, String> {
  @Override
  public String marshal(String val) throws Exception {
    return val;
  }

  @Override
  public String unmarshal(String val) throws Exception {
    if (val == null)
      return null;
    return val.trim();
  }
}
