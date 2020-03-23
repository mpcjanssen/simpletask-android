package com.thegrizzlylabs.sardineandroid.util;

import com.thegrizzlylabs.sardineandroid.model.Prop;
import com.thegrizzlylabs.sardineandroid.model.Property;
import com.thegrizzlylabs.sardineandroid.model.Resourcetype;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Basic utility code. I borrowed some code from the webdavlib for
 * parsing dates.
 *
 * @author jonstevens
 */
public final class SardineUtil {
    private SardineUtil() {
    }

    private final static String[] SUPPORTED_DATE_FORMATS = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "EEE MMM dd HH:mm:ss zzz yyyy",
            "EEEEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMMM d HH:mm:ss yyyy"};

    /**
     * Default namespace prefix
     */
    public static final String CUSTOM_NAMESPACE_PREFIX = "s";

    /**
     * Default namespace URI
     */
    public static final String CUSTOM_NAMESPACE_URI = "SAR:";

    /**
     * Default namespace prefix
     */
    public static final String DEFAULT_NAMESPACE_PREFIX = "D";

    /**
     * Default namespace URI
     */
    public static final String DEFAULT_NAMESPACE_URI = "DAV:";

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Date formats using for Date parsing.
     */
    private static final List<ThreadLocal<SimpleDateFormat>> DATETIME_FORMATS;

    static {
        List<ThreadLocal<SimpleDateFormat>> l = new ArrayList<>(SUPPORTED_DATE_FORMATS.length);
        for (int i = 0; i < SUPPORTED_DATE_FORMATS.length; i++) {
            l.add(new ThreadLocal<SimpleDateFormat>());
        }
        DATETIME_FORMATS = Collections.unmodifiableList(l);
    }

    /**
     * Loops over all the possible date formats and tries to find the right one.
     *
     * @param value ISO date string
     * @return Null if there is a parsing failure
     */
    public static Date parseDate(String value) {
        if (value == null) {
            return null;
        }
        Date date = null;
        for (int i = 0; i < DATETIME_FORMATS.size(); i++) {
            ThreadLocal<SimpleDateFormat> format = DATETIME_FORMATS.get(i);
            SimpleDateFormat sdf = format.get();
            if (sdf == null) {
                sdf = new SimpleDateFormat(SUPPORTED_DATE_FORMATS[i], Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                format.set(sdf);
            }
            try {
                date = sdf.parse(value);
                break;
            } catch (ParseException e) {
                // We loop through this until we found a valid one.
            }
        }
        return date;
    }

    private static Serializer getSerializer() throws Exception {
        Format format = new Format("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        Registry registry = new Registry();
        Strategy strategy = new RegistryStrategy(registry);
        Serializer serializer = new Persister(strategy, format);

        registry.bind(Prop.class, new EntityWithAnyElementConverter<>(serializer, Prop.class));
        registry.bind(Resourcetype.class, new EntityWithAnyElementConverter<>(serializer, Resourcetype.class));
        registry.bind(Property.class, Property.PropertyConverter.class);

        return serializer;
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(Class<? extends T> type, InputStream in) throws IOException {
        try {
            return getSerializer().read(type, in);
        } catch (SAXException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            // Server does not return any valid WebDAV XML that matches our JAXB context
            throw new IOException("Not a valid DAV response", e);
        }
    }

    /**
     * @param jaxbElement An object from the model
     * @return The XML string for the WebDAV request
     * @throws RuntimeException When there is a JAXB error
     */
    public static String toXml(Object jaxbElement) {
        StringWriter writer = new StringWriter();
        try {
            getSerializer().write(jaxbElement, writer);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return writer.toString();
    }

    /**
     * @return New XML document from the default document builder factory.
     */
    private static Document createDocument() {
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return builder.newDocument();
    }

    /** */
    public static Map<QName, String> toQName(Map<String, String> setProps) {
        if (setProps == null) {
            return Collections.emptyMap();
        }
        Map<QName, String> result = new HashMap<>(setProps.size());
        for (Map.Entry<String, String> entry : setProps.entrySet()) {
            result.put(createQNameWithCustomNamespace(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /** */
    public static List<QName> toQName(List<String> removeProps) {
        if (removeProps == null) {
            return Collections.emptyList();
        }
        List<QName> result = new ArrayList<>(removeProps.size());
        for (String entry : removeProps) {
            result.add(createQNameWithCustomNamespace(entry));
        }
        return result;
    }

    public static QName toQName(Element element) {
        String namespace = element.getNamespaceURI();
        if (namespace == null) {
            return new QName(SardineUtil.DEFAULT_NAMESPACE_URI,
                    element.getLocalName(),
                    SardineUtil.DEFAULT_NAMESPACE_PREFIX);
        } else if (element.getPrefix() == null) {
            return new QName(element.getNamespaceURI(),
                    element.getLocalName());
        } else {
            return new QName(element.getNamespaceURI(),
                    element.getLocalName(),
                    element.getPrefix());
        }

    }

    /**
     * @param key Local element name.
     */
    public static QName createQNameWithCustomNamespace(String key) {
        return new QName(CUSTOM_NAMESPACE_URI, key, CUSTOM_NAMESPACE_PREFIX);
    }

    /**
     * @param key Local element name.
     */
    public static QName createQNameWithDefaultNamespace(String key) {
        return new QName(DEFAULT_NAMESPACE_URI, key, DEFAULT_NAMESPACE_PREFIX);
    }

    /**
     * @param key Fully qualified element name.
     */
    public static Element createElement(QName key) {
        return createDocument().createElementNS(key.getNamespaceURI(), key.getPrefix() + ":" + key.getLocalPart());
    }

    /**
     * @param key Fully qualified element name.
     */
    public static Element createElement(Element parent, QName key) {
        return parent.getOwnerDocument().createElementNS(key.getNamespaceURI(), key.getPrefix() + ":" + key.getLocalPart());
    }
   
    /**
     *  @return standard UTF8 charset on any version of Android
     */
    public static Charset standardUTF8() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            return StandardCharsets.UTF_8;
        } else {
            return Charset.forName("UTF-8");
        }
    }
}
