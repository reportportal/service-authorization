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
package com.epam.reportportal.auth.converter;

import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.ws.model.settings.SamlDetailsResource;

import java.util.function.Function;

/**
 * Used for mapping between SAML resource model and entity
 *
 * @author Yevgeniy Svalukhin
 */
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
