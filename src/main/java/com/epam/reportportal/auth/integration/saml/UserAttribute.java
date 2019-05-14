package com.epam.reportportal.auth.integration.saml;

public enum UserAttribute {
    FIRST_NAME("FirstName"), LAST_NAME("LastName"), EMAIL("Email");

    private String value;

    UserAttribute(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

