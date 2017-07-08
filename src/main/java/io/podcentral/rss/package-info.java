@XmlJavaTypeAdapters({@XmlJavaTypeAdapter(value = DateAdapter.class, type = Date.class),
    @XmlJavaTypeAdapter(value = BoolAdapter.class, type = Boolean.class)})
@XmlSchema(
    xmlns = {@javax.xml.bind.annotation.XmlNs(prefix = "itunes", namespaceURI = XmlNs.ITUNES),
        @javax.xml.bind.annotation.XmlNs(prefix = "media", namespaceURI = XmlNs.MEDIA),
        @javax.xml.bind.annotation.XmlNs(prefix = "atom", namespaceURI = XmlNs.ATOM)},
    elementFormDefault = javax.xml.bind.annotation.XmlNsForm.UNQUALIFIED)
package io.podcentral.rss;

import java.util.Date;

import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import io.podcentral.xml.BoolAdapter;
import io.podcentral.xml.DateAdapter;
