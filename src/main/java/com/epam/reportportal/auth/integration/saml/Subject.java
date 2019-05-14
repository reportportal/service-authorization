package com.epam.reportportal.auth.integration.saml;

import java.io.Serializable;

public class Subject implements Serializable {

    private static final long serialVersionUID = 2390092323L;

    private Principal principal;

    public Principal getPrincipal() {
        return principal;
    }

    public Subject setPrincipal(Principal principal) {
        this.principal = principal;
        return this;
    }
}
