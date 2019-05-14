package com.epam.reportportal.auth.converter;

import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.ws.model.settings.SamlDetailsResource;

import java.util.function.Function;

public class SamlDetailsConverter {

    public final static Function<SamlProviderDetails, SamlDetailsResource> TO_RESOURCE = details ->
            new SamlDetailsResource()
                    .setEmailAttribute(details.getEmailAttributeId())
                    .setFirstNameAttribute(details.getFirstNameAttributeId())
                    .setLastNameAttribute(details.getLastNameAttributeId())
                    .setFullNameAttribute(details.getFullNameAttributeId())
                    .setIdentityProviderAlias(details.getIdpAlias())
                    .setIdentityProviderMetadataUrl(details.getIdpMetadata())
                    .setIdentityProviderName(details.getIdpName())
                    .setIdentityProviderUrl(details.getIdpUrl())
                    .setEnabled(details.isEnabled());

    public final static Function<SamlDetailsResource, SamlProviderDetails> FROM_RESOURCE = resource ->
            new SamlProviderDetails()
                    .setEmailAttributeId(resource.getEmailAttribute())
                    .setFirstNameAttributeId(resource.getFirstNameAttribute())
                    .setLastNameAttributeId(resource.getLastNameAttribute())
                    .setFullNameAttributeId(resource.getFullNameAttribute())
                    .setIdpAlias(resource.getIdentityProviderAlias())
                    .setIdpMetadata(resource.getIdentityProviderMetadataUrl())
                    .setIdpName(resource.getIdentityProviderName())
                    .setIdpUrl(resource.getIdentityProviderUrl())
                    .setEnabled(resource.isEnabled());
}
