package com.epam.reportportal.auth.integration.saml;

import org.springframework.security.saml.saml2.authentication.SubjectPrincipal;

import java.io.Serializable;

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
}
