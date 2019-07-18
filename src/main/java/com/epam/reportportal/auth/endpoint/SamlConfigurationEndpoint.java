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
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.event.SamlProvidersReloadEvent;
import com.epam.reportportal.auth.integration.converter.SamlDetailsConverter;
import com.epam.ta.reportportal.dao.SamlProviderDetailsRepository;
import com.epam.ta.reportportal.entity.saml.SamlProviderDetails;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.integration.auth.SamlDetailsResource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProvider;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.integration.converter.SamlDetailsConverter.TO_RESOURCE;
import static com.epam.ta.reportportal.commons.Predicates.isPresent;
import static com.epam.ta.reportportal.commons.Predicates.not;
import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static java.util.Optional.ofNullable;

/**
 * This class is responsible for provisioning REST API for managing of SAML identity providers' settings
 *
 * @author Yevgeniy Svalukhin
 */
@Controller
@RequestMapping("/settings/saml")
public class SamlConfigurationEndpoint {

	private final ApplicationEventPublisher eventPublisher;
	private SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning;
	private SamlProviderDetailsRepository repository;

	public SamlConfigurationEndpoint(ApplicationEventPublisher eventPublisher,
			SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning, SamlProviderDetailsRepository repository) {
		this.eventPublisher = eventPublisher;
		this.serviceProviderProvisioning = serviceProviderProvisioning;
		this.repository = repository;
	}

	@GetMapping("/{providerName}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Transactional(readOnly = true)
	public SamlDetailsResource getSamlSettingsByName(@PathVariable String providerName) {
		SamlProviderDetails samlProviderDetails = repository.findByIdpName(providerName)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, providerName));
		return TO_RESOURCE.apply(samlProviderDetails);
	}

	@GetMapping
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Transactional(readOnly = true)
	public List<SamlDetailsResource> getAllSamlSettings() {
		return repository.findAll().stream().map(TO_RESOURCE).collect(Collectors.toList());
	}

	@DeleteMapping("/{providerId}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Transactional
	public OperationCompletionRS deleteSamlSetting(@PathVariable Long providerId) {
		SamlProviderDetails samlProviderDetails = repository.findById(providerId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, providerId));
		repository.delete(samlProviderDetails);
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(repository.findAll()));
		return new OperationCompletionRS("Auth settings '" + samlProviderDetails.getIdpName() + "' were successfully removed");
	}

	@PostMapping
	@ResponseBody
	@ResponseStatus
	@Transactional
	public SamlDetailsResource createSamlSettings(@RequestBody @Validated SamlDetailsResource samlDetailsResource) {
		expect(repository.findByIdpName(samlDetailsResource.getIdentityProviderName()),
				not(isPresent())
		).verify(ErrorType.INTEGRATION_ALREADY_EXISTS, samlDetailsResource.getIdentityProviderName());
		validate(samlDetailsResource);
		SamlProviderDetails samlProviderDetails = populateProviderDetails(samlDetailsResource);
		repository.save(samlProviderDetails);
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(repository.findAll()));
		return TO_RESOURCE.apply(samlProviderDetails);
	}

	@PutMapping("/{providerId}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Transactional
	public SamlDetailsResource updateSamlSettings(@PathVariable Long providerId,
			@RequestBody @Validated SamlDetailsResource samlDetailsResource) {
		SamlProviderDetails samlProviderDetails = repository.findById(providerId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, providerId));

		validate(samlDetailsResource);

		ofNullable(samlDetailsResource.getIdentityProviderName()).ifPresent(samlProviderDetails::setIdpName);
		ofNullable(samlDetailsResource.getIdentityProviderMetadataUrl()).ifPresent(samlProviderDetails::setIdpMetadata);
		ofNullable(samlDetailsResource.getIdentityProviderNameId()).ifPresent(samlProviderDetails::setIdpNameId);
		ofNullable(samlDetailsResource.getIdentityProviderUrl()).ifPresent(samlProviderDetails::setIdpUrl);
		ofNullable(samlDetailsResource.getIdentityProviderAlias()).ifPresent(samlProviderDetails::setIdpAlias);
		ofNullable(samlDetailsResource.getEmailAttribute()).ifPresent(samlProviderDetails::setEmailAttributeId);
		ofNullable(samlDetailsResource.getFullNameAttribute()).ifPresent(samlProviderDetails::setFullNameAttributeId);
		ofNullable(samlDetailsResource.getFirstNameAttribute()).ifPresent(samlProviderDetails::setFirstNameAttributeId);
		ofNullable(samlDetailsResource.getLastNameAttribute()).ifPresent(samlProviderDetails::setLastNameAttributeId);

		populateProviderDetails(samlProviderDetails);

		repository.save(samlProviderDetails);
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(repository.findAll()));

		return TO_RESOURCE.apply(samlProviderDetails);

	}

	private SamlProviderDetails populateProviderDetails(SamlDetailsResource samlDetails) {
		SamlProviderDetails providerDetails = SamlDetailsConverter.FROM_RESOURCE.apply(samlDetails);
		populateProviderDetails(providerDetails);
		return providerDetails;
	}

	private void populateProviderDetails(SamlProviderDetails providerDetails) {
		ExternalIdentityProviderConfiguration externalConfiguration = new ExternalIdentityProviderConfiguration().setMetadata(
				providerDetails.getIdpMetadata());
		IdentityProviderMetadata remoteProvider = serviceProviderProvisioning.getHostedProvider().getRemoteProvider(externalConfiguration);
		providerDetails.setIdpUrl(remoteProvider.getEntityId());
		providerDetails.setIdpAlias(remoteProvider.getEntityAlias());

		NameId nameId = ofNullable(remoteProvider.getDefaultNameId()).orElseGet(() -> {
			Optional<NameId> first = remoteProvider.getProviders()
					.stream()
					.filter(IdentityProvider.class::isInstance)
					.map(IdentityProvider.class::cast)
					.flatMap(v -> v.getNameIds().stream())
					.filter(Objects::nonNull)
					.findFirst();
			return first.orElseThrow(() -> new ReportPortalException(ErrorType.BAD_UPDATE_PREFERENCE_REQUEST,
					"Provider does not contain information about identification mapping"
			));
		});
		providerDetails.setIdpNameId(nameId.toString());
	}

	private void validate(SamlDetailsResource samlDetails) {
		if (StringUtils.isEmpty(samlDetails.getFullNameAttribute()) && (StringUtils.isEmpty(samlDetails.getLastNameAttribute())
				|| StringUtils.isEmpty(samlDetails.getFirstNameAttribute()))) {
			throw new ReportPortalException(ErrorType.BAD_UPDATE_PREFERENCE_REQUEST,
					"Fields Full name or combination of Last name and First name are empty"
			);
		}
	}

}
