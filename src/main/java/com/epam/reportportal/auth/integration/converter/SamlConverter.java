/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.integration.converter;

import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.integration.auth.SamlProvidersResource;
import com.epam.ta.reportportal.ws.model.integration.auth.SamlResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.*;

/**
 * Used for mapping between SAML resource model and entity
 *
 * @author Yevgeniy Svalukhin
 */
public class SamlConverter {

	public static final BiFunction<UpdateAuthRQ, Integration, Integration> UPDATE_FROM_REQUEST = (request, integration) -> {
		integration.setEnabled(request.getEnabled());
		integration.setName(IDP_NAME.getRequiredParameter(request));
		ParameterUtils.setSamlParameters(request, integration);
		return integration;
	};

	public final static Function<Integration, SamlResource> TO_RESOURCE = integration -> {
		SamlResource resource = new SamlResource();
		resource.setId(integration.getId());
		resource.setIdentityProviderName(integration.getName());
		resource.setEnabled(integration.isEnabled());

		EMAIL_ATTRIBUTE.getParameter(integration).ifPresent(resource::setEmailAttribute);
		FIRST_NAME_ATTRIBUTE.getParameter(integration).ifPresent(resource::setFirstNameAttribute);
		LAST_NAME_ATTRIBUTE.getParameter(integration).ifPresent(resource::setLastNameAttribute);
		FULL_NAME_ATTRIBUTE.getParameter(integration).ifPresent(resource::setFullNameAttribute);
		IDP_ALIAS.getParameter(integration).ifPresent(resource::setIdentityProviderAlias);
		IDP_METADATA_URL.getParameter(integration).ifPresent(resource::setIdentityProviderMetadataUrl);
		IDP_URL.getParameter(integration).ifPresent(resource::setIdentityProviderUrl);
		IDP_NAME_ID.getParameter(integration).ifPresent(resource::setIdentityProviderNameId);
		return resource;
	};

	public final static Function<List<Integration>, List<ExternalIdentityProviderConfiguration>> TO_EXTERNAL_PROVIDER_CONFIG = integrations -> {
		List<ExternalIdentityProviderConfiguration> externalProviders = integrations.stream()
				.map(integration -> new ExternalIdentityProviderConfiguration().setAlias(IDP_ALIAS.getParameter(integration).get())
						.setMetadata(IDP_METADATA_URL.getRequiredParameter(integration))
						.setLinktext(integration.getName())
						.setNameId(NameId.fromUrn(IDP_NAME_ID.getParameter(integration).get())))
				.collect(Collectors.toList());
		IntStream.range(0, externalProviders.size()).forEach(value -> externalProviders.get(value).setAssertionConsumerServiceIndex(value));
		return externalProviders;
	};

	public final static Function<List<Integration>, SamlProvidersResource> TO_PROVIDERS_RESOURCE = integrations -> {
		if (CollectionUtils.isEmpty(integrations)) {
			SamlProvidersResource emptyResource = new SamlProvidersResource();
			emptyResource.setProviders(Collections.emptyList());
			return emptyResource;
		}
		SamlProvidersResource resource = new SamlProvidersResource();
		resource.setProviders(integrations.stream().map(TO_RESOURCE).collect(Collectors.toList()));
		return resource;
	};

}
