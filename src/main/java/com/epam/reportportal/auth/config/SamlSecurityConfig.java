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
package com.epam.reportportal.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

import static org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityDsl.serviceProvider;

/**
 * Configures security settings for SAML integration
 *
 * @author Yevgeniy Svalukhin
 */
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
