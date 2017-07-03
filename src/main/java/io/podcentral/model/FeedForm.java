package io.podcentral.model;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class FeedForm {
	private String feedUrl;
}
