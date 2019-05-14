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

        List<ExternalIdentityProviderConfiguration> existProviders = serviceProvider.getProviders();
        serviceProvider.setProviders(updatedProviders);

        existProviders.clear();
    }
}
