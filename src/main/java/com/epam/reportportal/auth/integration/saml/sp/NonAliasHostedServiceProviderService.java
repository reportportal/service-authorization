//package com.epam.reportportal.auth.integration.saml.sp;
//
//import java.net.URI;
//import java.util.UUID;
//import org.joda.time.DateTime;
//import org.springframework.security.saml.SamlMetadataCache;
//import org.springframework.security.saml.SamlProviderNotFoundException;
//import org.springframework.security.saml.SamlTransformer;
//import org.springframework.security.saml.SamlValidator;
//import org.springframework.security.saml.provider.service.AuthenticationRequestEnhancer;
//import org.springframework.security.saml.provider.service.HostedServiceProviderService;
//import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
//import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
//import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
//import org.springframework.security.saml.saml2.authentication.Issuer;
//import org.springframework.security.saml.saml2.metadata.Binding;
//import org.springframework.security.saml.saml2.metadata.Endpoint;
//import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
//import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
//
///**
// * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
// */
//public class NonAliasHostedServiceProviderService extends HostedServiceProviderService {
//
//  private final AuthenticationRequestEnhancer authenticationRequestEnhancer;
//
//  public NonAliasHostedServiceProviderService(LocalServiceProviderConfiguration configuration,
//      ServiceProviderMetadata metadata,
//      SamlTransformer transformer, SamlValidator validator, SamlMetadataCache cache,
//      AuthenticationRequestEnhancer authnRequestEnhancer) {
//    super(configuration, metadata, transformer, validator, cache, authnRequestEnhancer);
//    this.authenticationRequestEnhancer = authnRequestEnhancer;
//  }
//
//  @Override
//  public AuthenticationRequest authenticationRequest(IdentityProviderMetadata idp) {
//    ExternalIdentityProviderConfiguration configuration =
//        getIdentityProviderConfigurationForMetadata(idp);
//    final URI authnBinding = configuration.getAuthenticationRequestBinding();
//    Binding preferredBinding =
//        authnBinding == null ? Binding.REDIRECT : Binding.fromUrn(authnBinding);
//    Endpoint endpoint = getPreferredEndpoint(idp.getIdentityProvider().getSingleSignOnService(),
//        preferredBinding, 0);
//    ServiceProviderMetadata sp = getMetadata();
//    AuthenticationRequest request = new AuthenticationRequest()
//        // Some service providers will not accept first character if 0..9
//        // Azure AD IdP for example.
//        .setId("ARQ" + UUID.randomUUID().toString().substring(1))
//        .setIssueInstant(new DateTime(getClock().millis()))
//        .setForceAuth(Boolean.FALSE)
//        .setPassive(Boolean.FALSE)
//        .setBinding(endpoint.getBinding())
//        .setAssertionConsumerService(
//            getPreferredEndpoint(sp.getServiceProvider().getAssertionConsumerService(), null, -1))
//        .setIssuer(new Issuer().setValue(sp.getEntityId()))
//        .setDestination(endpoint);
//    if (sp.getServiceProvider().isAuthnRequestsSigned()) {
//      request.setSigningKey(sp.getSigningKey(), sp.getAlgorithm(), sp.getDigest());
//    }
//    return authenticationRequestEnhancer.enhance(request);
//  }
//
//  private ExternalIdentityProviderConfiguration getIdentityProviderConfigurationForMetadata(
//      IdentityProviderMetadata idp) {
//    return getConfiguration().getProviders()
//        .stream()
//        .filter(i -> i.getAlias().equals(idp.getEntityAlias()))
//        .findFirst()
//        .orElseThrow(() -> new SamlProviderNotFoundException("alias:" + idp.getEntityAlias()));
//  }
//
//}
