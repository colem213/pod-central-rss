package io.podcentral.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class CsvAdapter extends XmlAdapter<String, List<String>> {
  @Override
  public String marshal(List<String> val) throws Exception {
    return String.join(", ", val);
  }

  @Override
  public List<String> unmarshal(String val) throws Exception {
    return new ArrayList<String>(Arrays.asList(val.split(" *, *"))).stream()
        .filter(str -> !str.isEmpty()).collect(Collectors.toList());
  }
}
