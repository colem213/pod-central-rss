package io.podcentral.xml;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DateAdapter extends XmlAdapter<String, Date> {

	private static final String RSS_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

	@Override
	public String marshal(Date val) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat(RSS_DATE_FORMAT);
		return sdf.format(val);
	}

	@Override
	public Date unmarshal(String val) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat(RSS_DATE_FORMAT);
		return sdf.parse(val);
	}

}
