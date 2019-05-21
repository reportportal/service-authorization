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

import com.epam.reportportal.auth.util.CertificationUtil;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import org.opensaml.saml.saml2.core.NameID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.config.NetworkConfiguration;
import org.springframework.security.saml.provider.config.RotatingKeys;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.NameId;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Base64.getEncoder;

/**
 * SAML service provider configuration based on application settings
 *
 * @author Yevgeniy Svalukhin
 */
@Configuration
public class SamlServiceProviderConfiguration {

    @Value("${rp.auth.saml.base-path}")
    private String basePath;

    @Value("${rp.auth.saml.key-alias}")
    private String keyAlias;

    @Value("${rp.auth.saml.key-password}")
    private String keyPassword;

    @Value("${rp.auth.saml.key-store}")
    private String keyStore;

    @Value("${rp.auth.saml.key-store-password}")
    private String keyStorePassword;

    private ServerSettingsRepository serverSettingsRepository;

    public SamlServiceProviderConfiguration(ServerSettingsRepository serverSettingsRepository) {
        this.serverSettingsRepository = serverSettingsRepository;
    }

    @Bean(name = "spConfiguration")
    public SamlServerConfiguration samlServerConfiguration() {
        return new SamlServerConfiguration().setServiceProvider(serviceProviderConfiguration())
                .setNetwork(networkConfiguration());
    }

    private NetworkConfiguration networkConfiguration() {
        return new NetworkConfiguration().setConnectTimeout(5000)
                .setReadTimeout(10000);

    }

    private LocalServiceProviderConfiguration serviceProviderConfiguration() {
        LocalServiceProviderConfiguration serviceProviderConfiguration = new LocalServiceProviderConfiguration();
        serviceProviderConfiguration.setSignRequests(true)
                .setWantAssertionsSigned(true)
                .setEntityId("report.portal.sp.id")
                .setAlias("report-portal-sp")
                .setSignMetadata(true)
                .setSingleLogoutEnabled(true)
                .setNameIds(Arrays.asList(NameID.EMAIL, NameID.PERSISTENT, NameID.UNSPECIFIED))
                .setKeys(rotatingKeys())
                .setProviders(providers())
                .setPrefix("saml/sp")
                .setBasePath(basePath);
        return serviceProviderConfiguration;
    }

    private List<ExternalIdentityProviderConfiguration> providers() {
        ServerSettings settings = serverSettingsRepository.findOne("default");

        if (Objects.isNull(settings.getSamlProviderDetails())) {
            return Collections.emptyList();
        }

        List<ExternalIdentityProviderConfiguration> externalProviders = settings.getSamlProviderDetails().values().stream()
                .map(details -> new ExternalIdentityProviderConfiguration()
                        .setAlias(details.getIdpAlias())
                        .setMetadata(details.getIdpMetadata())
                        .setLinktext(details.getIdpName())
                        .setNameId(NameId.fromUrn(details.getIdpNameId())))
                .collect(Collectors.toList());
        IntStream.range(0, externalProviders.size())
                .forEach(value -> externalProviders.get(value).setAssertionConsumerServiceIndex(value));
        return externalProviders;
    }

    private RotatingKeys rotatingKeys() {
        return new RotatingKeys().setActive(activeKey())
                .setStandBy(standbyKeys());
    }

    private List<SimpleKey> standbyKeys() {
        return Collections.emptyList();
    }

    private SimpleKey activeKey() {
        X509Certificate certificate = CertificationUtil.getCertificateByName(keyAlias, keyStore, keyStorePassword);
        PrivateKey privateKey = CertificationUtil.getPrivateKey(keyAlias, keyPassword, keyStore, keyStorePassword);

        try {
            return new SimpleKey().setCertificate(getEncoder().encodeToString(certificate.getEncoded()))
                    .setPassphrase(keyPassword)
                    .setPrivateKey(getEncoder().encodeToString(privateKey.getEncoded()))
                    .setName("sp-signing-key");
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return new SimpleKey();
    }

}