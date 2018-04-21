package io.podcentral.rss;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import lombok.Data;

@Data
public class ItunesCategory {
  @XmlAttribute
  private String text;

  public String getSubCategory() {
    return getSubCategories().isEmpty() ? null : getSubCategories().get(0).getText();
  }

  @XmlPath("itunes:category")
  private List<ItunesCategory> subCategories;
}
