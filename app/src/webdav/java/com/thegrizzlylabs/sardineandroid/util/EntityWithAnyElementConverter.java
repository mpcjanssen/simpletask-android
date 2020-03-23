package com.thegrizzlylabs.sardineandroid.util;

import com.thegrizzlylabs.sardineandroid.model.EntityWithAnyElement;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityWithAnyElementConverter<T extends EntityWithAnyElement> implements Converter<T> {

    private Class<T> entityClass;
    private Serializer serializer;

    public EntityWithAnyElementConverter(Serializer serializer, Class<T> entityClass) {
        this.serializer = serializer;
        this.entityClass = entityClass;
    }

    private Map<String, Field> getEntityFields() {
        Map<String, Field> elementsFields = new HashMap<>();
        for (Field field : entityClass.getDeclaredFields()) {
            Element fieldAnnotation = field.getAnnotation(Element.class);
            if (fieldAnnotation != null) {
                String name = fieldAnnotation.name().equals("") ? field.getName() : fieldAnnotation.name();
                elementsFields.put(name, field);
            }
        }
        return elementsFields;
    }

    private Method getSetterForField(Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String capitalizedFieldName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
        return entityClass.getMethod("set" + capitalizedFieldName, field.getType());
    }

    private Method getGetterForField(Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String capitalizedFieldName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
        return entityClass.getMethod("get" + capitalizedFieldName);
    }

    @Override
    public T read(InputNode node) throws Exception {
        Map<String, Field> entityFields = getEntityFields();
        T entity = entityClass.newInstance();
        List<org.w3c.dom.Element> anyElements = entity.getAny();
        InputNode childNode;
        while((childNode = node.getNext()) != null) {
            if (entityFields.containsKey(childNode.getName())) {
                Field field = entityFields.get(childNode.getName());
                getSetterForField(field).invoke(entity, serializer.read(field.getType(), childNode));
            } else if (childNode.getPrefix() != null && !childNode.getPrefix().isEmpty()) {
                org.w3c.dom.Element element = ElementConverter.read(childNode);
                anyElements.add(element);
            } else {
                // Probably a WebDAV field we don't support yet
                skipChildrenOfNode(childNode);
            }
        }
        return entity;
    }

    private void skipChildrenOfNode(InputNode node) throws Exception {
        while(node.getNext() != null) {
            // Do nothing
        }
    }

    @Override
    public void write(OutputNode node, T entity) throws Exception {
        for(org.w3c.dom.Element domElement : entity.getAny()) {
            ElementConverter.write(node, domElement);
        }
        Map<String, Field> entityFields = getEntityFields();
        for (String fieldName : entityFields.keySet()) {
            Field field = entityFields.get(fieldName);
            Object value = getGetterForField(field).invoke(entity);
            if (value == null) {
                continue;
            }
            if (value instanceof String) {
                OutputNode childNode = node.getChild(fieldName);
                childNode.setReference(SardineUtil.DEFAULT_NAMESPACE_URI);
                childNode.setValue((String)value);
            } else {
                serializer.write(value, node);
            }
        }
    }
}
