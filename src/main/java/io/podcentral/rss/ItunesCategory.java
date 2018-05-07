package io.podcentral.rss;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@DynamoDBDocument
@NoArgsConstructor
@RequiredArgsConstructor
@Data
public class ItunesCategory {
  @NonNull
  @XmlAttribute(name = "text")
  private String name;

  @DynamoDBAttribute(attributeName = "subCategory")
  public String getSubCategory() {
    return getSubCategories() == null || getSubCategories().isEmpty() ? null
        : getSubCategories().get(0).getName();
  }

  @XmlPath("itunes:category")
  private List<ItunesCategory> subCategories;
}
