package io.podcentral.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ItunesExplicitAdapter extends XmlAdapter<String, Boolean> {
  @Override
  public String marshal(Boolean val) throws Exception {
    if (val == null)
      return null;
    return val ? "true" : "false";
  }

  @Override
  public Boolean unmarshal(String val) throws Exception {
    boolean isExplicit = "yes".equalsIgnoreCase(val) || "explicit".equalsIgnoreCase(val)
        || "true".equalsIgnoreCase(val);
    boolean isNotExplicit = "clean".equalsIgnoreCase(val) || "no".equalsIgnoreCase(val)
        || "false".equalsIgnoreCase(val);
    if (isExplicit)
      return true;
    else if (isNotExplicit)
      return false;
    else
      return null;
  }
}
