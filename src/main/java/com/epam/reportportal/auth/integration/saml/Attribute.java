/*
 * Copyright 2019 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.integration.saml;

import org.springframework.security.saml.saml2.attribute.AttributeNameFormat;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Represents attributes extracted from SAML response message
 *
 * @author Yevgeniy Svalukhin
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return required == attribute.required &&
                Objects.equals(name, attribute.name) &&
                Objects.equals(nameFormat, attribute.nameFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nameFormat, required);
    }
}
