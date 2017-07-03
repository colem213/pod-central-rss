@XmlJavaTypeAdapters({
	@XmlJavaTypeAdapter(value=DateAdapter.class, type=Date.class)
})
package io.podcentral.rss;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import io.podcentral.xml.DateAdapter;