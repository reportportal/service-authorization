package com.epam.reportportal.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

import static org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityDsl.serviceProvider;

@Configuration
@Order(5)
public class SamlSecurityConfig extends SamlServiceProviderSecurityConfiguration {

    private SamlServerConfiguration serverConfiguration;

    public SamlSecurityConfig(SamlServiceProviderServerBeanConfiguration configuration,
                              SamlServerConfiguration spConfiguration) {
        super(configuration);
        this.serverConfiguration = spConfiguration;

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        super.configure(http);

        http.apply(serviceProvider())
                .configure(serverConfiguration);
    }


}
