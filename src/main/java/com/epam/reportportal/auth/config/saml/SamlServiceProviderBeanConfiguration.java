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
package com.epam.reportportal.auth.config.saml;

import static org.springframework.util.StringUtils.hasText;

import com.epam.reportportal.auth.AuthFailureHandler;
import com.epam.reportportal.auth.integration.saml.ReportPortalSamlAuthenticationManager;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import com.epam.reportportal.auth.integration.saml.sp.HostBasedSamlServiceProviderProvisioningExtension;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.Base64;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.SamlKeyException;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.saml.spi.DefaultValidator;
import org.springframework.security.saml.spi.SamlKeyStoreProvider;
import org.springframework.security.saml.spi.SpringSecuritySaml;
import org.springframework.security.saml.spi.opensaml.OpenSamlImplementation;
import org.springframework.security.saml.util.X509Utilities;

/**
 * Bean declarations for service provider part of SAML integration
 *
 * @author Yevgeniy Svalukhin
 */
@Configuration
public class SamlServiceProviderBeanConfiguration extends
    SamlServiceProviderServerBeanConfiguration {

  private final Integer maxSessionLive;

  private SamlAuthSuccessHandler samlSuccessHandler;
  private AuthFailureHandler authFailureHandler;
  private SamlUserReplicator samlUserReplicator;
  private SamlServerConfiguration serviceProviderConfiguration;

  public SamlServiceProviderBeanConfiguration(
      @Value("${rp.auth.saml.session-live}") Integer maxSessionLive,
      SamlAuthSuccessHandler samlSuccessHandler, AuthFailureHandler authFailureHandler,
      SamlUserReplicator samlUserReplicator, SamlServerConfiguration spConfiguration) {
    this.maxSessionLive = maxSessionLive;
    this.samlSuccessHandler = samlSuccessHandler;
    this.authFailureHandler = authFailureHandler;
    this.samlUserReplicator = samlUserReplicator;
    this.serviceProviderConfiguration = spConfiguration;
  }

  @Override
  protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
    return serviceProviderConfiguration;
  }

  @Override
  public SamlProviderProvisioning<ServiceProviderService> getSamlProvisioning() {
    return new HostBasedSamlServiceProviderProvisioningExtension(
        samlConfigurationRepository(),
        samlTransformer(),
        samlValidator(),
        samlMetadataCache(),
        authenticationRequestEnhancer()
    );
  }

  @Override
  public Filter spAuthenticationResponseFilter() {
    SamlAuthenticationResponseFilter authenticationFilter = new SamlAuthenticationResponseFilter(
        getSamlProvisioning());
    authenticationFilter.setAuthenticationManager(
        new ReportPortalSamlAuthenticationManager(samlUserReplicator));
    authenticationFilter.setAuthenticationSuccessHandler(samlSuccessHandler);
    authenticationFilter.setAuthenticationFailureHandler(authFailureHandler);
    return authenticationFilter;
  }

  @Override
  public SpringSecuritySaml samlImplementation() {
    OpenSamlImplementation implementation = new OpenSamlImplementation(samlTime()).init();
    implementation.setSamlKeyStoreProvider(samlKeyStoreProvider());
    return implementation;
  }

  @Override
  public SamlValidator samlValidator() {
    final DefaultValidator defaultValidator = new DefaultValidator(samlImplementation());
    defaultValidator.setMaxAuthenticationAgeMillis(
        Math.toIntExact(Duration.ofMinutes(maxSessionLive).toMillis()));
    return defaultValidator;
  }

  private SamlKeyStoreProvider samlKeyStoreProvider() {
    return new SamlKeyStoreProvider() {
      @Override
      public KeyStore getKeyStore(SimpleKey key) {
        try {
          KeyStore ks = KeyStore.getInstance("JKS");
          ks.load(null, DEFAULT_KS_PASSWD);

          byte[] certbytes = X509Utilities.getDER(key.getCertificate());
          Certificate certificate = X509Utilities.getCertificate(certbytes);
          ks.setCertificateEntry(key.getName(), certificate);

          if (hasText(key.getPrivateKey())) {

            RSAPrivateKey privateKey = X509Utilities.getPrivateKey(
                Base64.getDecoder().decode(key.getPrivateKey()), "RSA");

            ks.setKeyEntry(key.getName(), privateKey, key.getPassphrase().toCharArray(),
                new Certificate[]{certificate});
          }

          return ks;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException |
                 InvalidKeySpecException | IOException e) {
          throw new SamlKeyException(e);
        }
      }
    };
  }
}
