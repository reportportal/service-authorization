/*
 * Copyright 2016 EPAM Systems
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
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.OAuthSecurityConfig;
import com.epam.reportportal.auth.oauth.OAuthProvider;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

/**
 * Shows list of supported authentication providers
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class AuthProvidersInfoContributor implements InfoContributor {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthProvidersInfoContributor.class);

    private final ServerSettingsRepository settingsRepository;
    private final Map<String, OAuthProvider> providersMap;

    @Autowired
    public AuthProvidersInfoContributor(ServerSettingsRepository settingsRepository,
                                        Map<String, OAuthProvider> providersMap) {
        this.settingsRepository = settingsRepository;
        this.providersMap = providersMap;
    }

    @Override
    public void contribute(Info.Builder builder) {
        ServerSettings settings = settingsRepository.findOne("default");

        final Map<String, OAuth2LoginDetails> oauth2Details = settings.getoAuth2LoginDetails();

        final Map<String, AuthProviderInfo> providers = providersMap.values()
                .stream()
                .filter(p -> !p.isConfigDynamic() || (null != oauth2Details && oauth2Details.containsKey(p.getName())))
                .collect(Collectors
                        .toMap(OAuthProvider::getName,
                                p -> new AuthProviderInfo(p.getButton(), p.buildPath(getAuthBasePath()))));

        Map<String, AuthProviderInfo> samlProviders = locateSamlProviders(settings);

        providers.putAll(samlProviders);

        builder.withDetail("auth_extensions", providers);
        //@formatter:on
    }

    private Map<String, AuthProviderInfo> locateSamlProviders(ServerSettings settings) {
        Map<String, SamlProviderDetails> samlDetails = Optional.ofNullable(settings.getSamlProviderDetails())
                .orElseGet(HashMap::new);

        Map<String, AuthProviderInfo> samlProviders = new HashMap<>();
        for (Map.Entry<String, SamlProviderDetails> entry : samlDetails.entrySet()) {
            SamlProviderDetails samlProvider = entry.getValue();
            if (!samlProvider.isEnabled()) {
                continue;
            }

            AuthProviderInfo provider = populateProviderInfo(samlProvider);
            if (provider != null) {
                samlProviders.put(entry.getKey(), provider);
            }

        }
        return samlProviders;
    }

    private AuthProviderInfo populateProviderInfo(SamlProviderDetails samlProviderDetails) {
        try {
            return new AuthProviderInfo(buildButton(samlProviderDetails.getIdpName()), buildPath(samlProviderDetails.getIdpUrl()));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unable to populate provider information for SAML provider: {}", samlProviderDetails.getIdpName(), e);
            return null;
        }
    }

    private String buildButton(String providerName) {
        return String.format("<span>Login with %s</span>", providerName);
    }

    private String buildPath(String providerUrl) throws UnsupportedEncodingException {
        return fromCurrentContextPath()
                .path(String.format("/saml/sp/discovery?idp=%s", UriUtils.encode(providerUrl, UTF_8.toString())))
                .build().getPath();
    }

    private String getAuthBasePath() {
        return fromCurrentContextPath().path(OAuthSecurityConfig.SSO_LOGIN_PATH).build().getPath();
    }

    public static class AuthProviderInfo {
        private String button;
        private String path;

        public AuthProviderInfo(String button, String path) {
            this.button = button;
            this.path = path;
        }

        public String getButton() {
            return button;
        }

        public void setButton(String button) {
            this.button = button;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

}
