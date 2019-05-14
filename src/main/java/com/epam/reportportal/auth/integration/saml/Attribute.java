package com.epam.reportportal.auth.integration.saml;

import org.springframework.security.saml.saml2.attribute.AttributeNameFormat;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Attribute implements Serializable {
    private static final long serialVersionUID = -182983902349882L;

    private String name;
    private String friendlyName;
    private List<Object> values = new LinkedList<>();
    private String nameFormat = AttributeNameFormat.UNSPECIFIED.toString();
    private boolean required;

    public String getName() {
        return name;
    }

    public Attribute setName(String name) {
        this.name = name;
        return this;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public Attribute setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
    }

    public List<Object> getValues() {
        return values;
    }

    public Attribute setValues(List<Object> values) {
        this.values.addAll(values);
        return this;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public Attribute setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public Attribute setRequired(boolean required) {
        this.required = required;
        return this;
    }
}
