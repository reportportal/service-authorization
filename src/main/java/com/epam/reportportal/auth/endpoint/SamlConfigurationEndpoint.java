package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.converter.SamlDetailsConverter;
import com.epam.reportportal.auth.event.SamlProvidersReloadEvent;
import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.settings.SamlDetailsResource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProvider;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings/{profileId}/saml")
public class SamlConfigurationEndpoint {

    private final ServerSettingsRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning;

    public SamlConfigurationEndpoint(ServerSettingsRepository repository,
                                     ApplicationEventPublisher eventPublisher,
                                     SamlProviderProvisioning<ServiceProviderService> samlServiceProviderProvisioning) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.serviceProviderProvisioning = samlServiceProviderProvisioning;
    }

    @GetMapping(value = "/")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, SamlDetailsResource> getSamlSettings(@PathVariable String profileId) {

        ServerSettings settings = repository.findOne(profileId);
        BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);

        return Optional.ofNullable(settings.getSamlProviderDetails())
                .map(details -> details.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> SamlDetailsConverter.TO_RESOURCE.apply(e.getValue()))))
                .orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND));
    }

    @GetMapping(value = "/{providerId}")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public SamlDetailsResource getSamlProviderSettings(@PathVariable String profileId,
                                                       @PathVariable String providerId) {
        ServerSettings settings = repository.findOne(profileId);
        BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);

        return Optional.ofNullable(settings.getSamlProviderDetails())
                .flatMap(details -> Optional.ofNullable(details.get(providerId)))
                .map(SamlDetailsConverter.TO_RESOURCE)
                .orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, providerId));
    }

    @RequestMapping(value = "/{providerId}", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, SamlDetailsResource> updateSamlSettings(@PathVariable String profileId,
                                                                @PathVariable("providerId") String samlProviderName,
                                                                @RequestBody @Validated SamlDetailsResource samlDetails) {

        validate(samlDetails);
        SamlProviderDetails providerDetails = populateProviderDetails(samlDetails);

        ServerSettings settings = repository.findOne(profileId);
        BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);

        Map<String, SamlProviderDetails> serverDetails = Optional.ofNullable(settings.getSamlProviderDetails())
                .orElse(new HashMap<>());
        serverDetails.put(samlProviderName, providerDetails);

        settings.setSamlProviderDetails(serverDetails);
        repository.save(settings);

        eventPublisher.publishEvent(new SamlProvidersReloadEvent(serverDetails));

        return serverDetails.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> SamlDetailsConverter.TO_RESOURCE.apply(e.getValue())));
    }

    private SamlProviderDetails populateProviderDetails(SamlDetailsResource samlDetails) {
        SamlProviderDetails providerDetails = SamlDetailsConverter.FROM_RESOURCE.apply(samlDetails);

        ExternalIdentityProviderConfiguration externalConfiguration = new ExternalIdentityProviderConfiguration()
                .setMetadata(samlDetails.getIdentityProviderMetadataUrl());
        IdentityProviderMetadata remoteProvider = serviceProviderProvisioning.getHostedProvider().getRemoteProvider(externalConfiguration);
        providerDetails.setIdpUrl(remoteProvider.getEntityId());
        providerDetails.setIdpAlias(remoteProvider.getEntityAlias());

        NameId nameId = Optional.ofNullable(remoteProvider.getDefaultNameId()).orElseGet(() -> {
            Optional<NameId> first = remoteProvider.getProviders().stream()
                    .filter(IdentityProvider.class::isInstance)
                    .map(IdentityProvider.class::cast)
                    .flatMap(v -> v.getNameIds().stream())
                    .filter(Objects::nonNull)
                    .findFirst();
            return first.orElseThrow(() -> new ReportPortalException(ErrorType.BAD_UPDATE_PREFERENCE_REQUEST, "Provider does not contain information about identification mapping"));
        });
        providerDetails.setIdpNameId(nameId.toString());

        return providerDetails;
    }

    private void validate(SamlDetailsResource samlDetails) {
        if (StringUtils.isEmpty(samlDetails.getFullNameAttribute())
                && (StringUtils.isEmpty(samlDetails.getLastNameAttribute())
                        || StringUtils.isEmpty(samlDetails.getFirstNameAttribute()))) {
            throw new ReportPortalException(ErrorType.BAD_UPDATE_PREFERENCE_REQUEST, "Fields Full name or combination of Last name and First name are empty");
        }
    }

}
