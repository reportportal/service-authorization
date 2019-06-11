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
package com.epam.reportportal.auth.event;

import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import org.springframework.context.ApplicationListener;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handles SAML settings changes event and reload configuration of IDP in service provider configuration
 *
 * @author Yevgeniy Svalukhin
 */
@Component
public class SamlProvidersReloadEventHandler implements ApplicationListener<SamlProvidersReloadEvent> {

    private SamlServerConfiguration samlConfiguration;

    public SamlProvidersReloadEventHandler(SamlServerConfiguration spConfiguration) {
        this.samlConfiguration = spConfiguration;
    }

    @Override
    public void onApplicationEvent(SamlProvidersReloadEvent event) {
        Map<String, SamlProviderDetails> details = event.getDetails();

        LocalServiceProviderConfiguration serviceProvider = samlConfiguration.getServiceProvider();
        List<ExternalIdentityProviderConfiguration> updatedProviders = details.values().stream()
                .map(detail -> new ExternalIdentityProviderConfiguration()
                        .setAlias(detail.getIdpAlias())
                        .setMetadata(detail.getIdpMetadata())
                        .setLinktext(detail.getIdpName())
                        .setNameId(NameId.fromUrn(detail.getIdpNameId())))
                .collect(Collectors.toList());
        IntStream.range(0, updatedProviders.size())
                .forEach(value -> updatedProviders.get(value).setAssertionConsumerServiceIndex(value));

        serviceProvider.getProviders().clear();
        serviceProvider.getProviders().addAll(updatedProviders);
    }
}
