package io.podcentral.rss;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import lombok.Data;

@Data
public class Channel {
	private String title;
	private String description;
	private String link;
	private Date pubDate;
	private String language;
	private List<Item> items;

	@XmlElement(name = "item")
	public List<Item> getItems() {
		return items;
	}
}
