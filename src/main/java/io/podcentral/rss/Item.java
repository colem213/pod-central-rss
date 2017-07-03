package io.podcentral.rss;

import java.util.Date;

import lombok.Data;

@Data
public class Item {
	private String title;
	private String link;
	private String description;
	private Date pubDate;
}
