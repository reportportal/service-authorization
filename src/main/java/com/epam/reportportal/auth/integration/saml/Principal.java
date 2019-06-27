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

import org.springframework.security.saml.saml2.authentication.SubjectPrincipal;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents principal extracted from SAML response and used for authentication
 *
 * @author Yevgeniy Svalukhin
 */
public class Principal extends SubjectPrincipal<Principal> implements Serializable, java.security.Principal {

    private static final long serialVersionUID = -341083232L;

    private String format;
    private String value;

    public String getFormat() {
        return format;
    }

    public Principal setFormat(String format) {
        this.format = format;
        return this;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Principal setValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public String getName() {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Principal principal = (Principal) o;
        return format.equals(principal.format) &&
                value.equals(principal.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, value);
    }
}
