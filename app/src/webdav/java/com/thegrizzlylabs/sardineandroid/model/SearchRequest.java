/*
 * copyright(c) 2014 SAS Institute, Cary NC 27513 Created on Oct 23, 2014
 */
package com.thegrizzlylabs.sardineandroid.model;

import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * <p>Java class for anonymous complex type.</p>
 *
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>
    &lt;element name="searchrequest"&gt;
        &lt;complexType&gt;
            &lt;any processContents="skip" namespace="##other" minOccurs="1" maxOccurs="1" /&gt;
        &lt;/complexType&gt;
    &lt;/element&gt;
 * </pre>
 */
@Root
@Namespace(prefix = "D", reference = "DAV:")
public class SearchRequest
{
	private String language;

	private String query;

	public SearchRequest()
	{
		this.language = "davbasic";
		this.query = "";
	}

	public SearchRequest(String language, String query)
	{
		this.language = language;
		this.query = query;
	}

	public final String getLanguage()
	{
		return language;
	}

	//@XmlTransient
	public void setLanguage(String language)
	{
		this.language = language;
	}

	public final String getQuery()
	{
		return query;
	}

	//@XmlTransient
	public void setQuery(String query)
	{
		this.query = query;
	}

//	@XmlAnyElement
//	public JAXBElement<String> getElement()
//	{
//		return new JAXBElement<String>(new QName("DAV:", language), String.class, query);
//	}
}
