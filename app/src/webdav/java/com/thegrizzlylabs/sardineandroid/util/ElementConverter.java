package com.thegrizzlylabs.sardineandroid.util;

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

public class ElementConverter {

    public static Element read(InputNode node) throws Exception {
        QName qname = new QName(node.getReference(), node.getName(), node.getPrefix());
        org.w3c.dom.Element element = SardineUtil.createElement(qname);
        element.setTextContent(node.getValue());
        return element;
    }

    public static void write(OutputNode parent, Element domElement) throws Exception {
        OutputNode child = parent.getChild(domElement.getNodeName());
        child.getNamespaces().setReference(domElement.getNamespaceURI(), domElement.getPrefix());
        child.setValue(domElement.getTextContent());
        child.commit();
    }
}
