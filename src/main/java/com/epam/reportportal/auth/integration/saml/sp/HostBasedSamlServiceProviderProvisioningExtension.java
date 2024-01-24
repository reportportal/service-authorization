package com.epam.reportportal.auth.integration.saml.sp;

import static org.springframework.util.StringUtils.hasText;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.springframework.security.saml.SamlMetadataCache;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.key.KeyType;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.config.SamlConfigurationRepository;
import org.springframework.security.saml.provider.provisioning.HostBasedSamlServiceProviderProvisioning;
import org.springframework.security.saml.provider.service.AuthenticationRequestEnhancer;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class HostBasedSamlServiceProviderProvisioningExtension extends
    HostBasedSamlServiceProviderProvisioning {

  private final AuthenticationRequestEnhancer authenticationRequestEnhancer;

  public HostBasedSamlServiceProviderProvisioningExtension(
      SamlConfigurationRepository configuration, SamlTransformer transformer,
      SamlValidator validator, SamlMetadataCache cache,
      AuthenticationRequestEnhancer authnRequestEnhancer) {
    super(configuration, transformer, validator, cache, authnRequestEnhancer);
    this.authenticationRequestEnhancer = authnRequestEnhancer;
  }

  @Override
  protected ServiceProviderService getHostedServiceProvider(
      LocalServiceProviderConfiguration spConfig) {
    String basePath = spConfig.getBasePath();

    List<SimpleKey> keys = new LinkedList<>();
    SimpleKey activeKey = spConfig.getKeys().getActive();
    keys.add(activeKey);
    keys.add(activeKey.clone(activeKey.getName() + "-encryption", KeyType.ENCRYPTION));
    keys.addAll(spConfig.getKeys().getStandBy());
    SimpleKey signingKey = spConfig.isSignMetadata() ? spConfig.getKeys().getActive() : null;

    String prefix = hasText(spConfig.getPrefix()) ? spConfig.getPrefix() : "saml/sp/";
    String aliasPath = getAliasPath(spConfig);
    ServiceProviderMetadata metadata =
        serviceProviderMetadata(
            basePath,
            signingKey,
            keys,
            prefix,
            aliasPath,
            spConfig.getDefaultSigningAlgorithm(),
            spConfig.getDefaultDigest()
        );
    if (!spConfig.getNameIds().isEmpty()) {
      metadata.getServiceProvider().setNameIds(spConfig.getNameIds());
    }

    if (!spConfig.isSingleLogoutEnabled()) {
      metadata.getServiceProvider().setSingleLogoutService(Collections.emptyList());
    }
    if (hasText(spConfig.getEntityId())) {
      metadata.setEntityId(spConfig.getEntityId());
    }
    if (hasText(spConfig.getAlias())) {
      metadata.setEntityAlias(spConfig.getAlias());
    }
    metadata.getServiceProvider().setWantAssertionsSigned(spConfig.isWantAssertionsSigned());
    metadata.getServiceProvider().setAuthnRequestsSigned(spConfig.isSignRequests());

    return new NonAliasHostedServiceProviderService(
        spConfig,
        metadata,
        getTransformer(),
        getValidator(),
        getCache(),
        authenticationRequestEnhancer
    );
  }
}
