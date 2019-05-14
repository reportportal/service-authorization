package com.epam.reportportal.auth.config;

import com.epam.reportportal.auth.integration.saml.ReportPortalSamlAuthenticationManager;
import com.epam.reportportal.auth.integration.saml.SamlAuthenticationSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.SamlKeyException;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.authentication.GenericErrorAuthenticationFailureHandler;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.saml.spi.SamlKeyStoreProvider;
import org.springframework.security.saml.spi.SpringSecuritySaml;
import org.springframework.security.saml.spi.opensaml.OpenSamlImplementation;
import org.springframework.security.saml.util.X509Utilities;

import javax.servlet.Filter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static org.springframework.util.StringUtils.hasText;

@Configuration
public class SamlServiceProviderBeanConfiguration extends SamlServiceProviderServerBeanConfiguration {

    private SamlAuthenticationSuccessHandler samlSuccessHandler;
    private SamlUserReplicator samlUserReplicator;
    private SamlServerConfiguration serviceProviderConfiguration;

    public SamlServiceProviderBeanConfiguration(SamlServerConfiguration spConfiguration, SamlAuthenticationSuccessHandler samlSuccessHandler,
                                                SamlUserReplicator samlUserReplicator) {
        this.serviceProviderConfiguration = spConfiguration;
        this.samlSuccessHandler = samlSuccessHandler;
        this.samlUserReplicator = samlUserReplicator;
    }

    @Override
    protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
        return serviceProviderConfiguration;
    }

    @Override
    public Filter spAuthenticationResponseFilter() {
        SamlAuthenticationResponseFilter authenticationFilter = new SamlAuthenticationResponseFilter(getSamlProvisioning());
        authenticationFilter.setAuthenticationManager(new ReportPortalSamlAuthenticationManager(samlUserReplicator));
        authenticationFilter.setAuthenticationSuccessHandler(samlSuccessHandler);
        authenticationFilter.setAuthenticationFailureHandler(new GenericErrorAuthenticationFailureHandler());
        return authenticationFilter;
    }

    @Override
    public SpringSecuritySaml samlImplementation() {
        OpenSamlImplementation implementation = new OpenSamlImplementation(samlTime()).init();
        implementation.setSamlKeyStoreProvider(samlKeyStoreProvider());
        return implementation;
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

                        RSAPrivateKey privateKey = X509Utilities.getPrivateKey(Base64.getDecoder().decode(key.getPrivateKey()), "RSA");

                        ks.setKeyEntry(key.getName(), privateKey, key.getPassphrase().toCharArray(), new
                                Certificate[]{certificate});
                    }

                    return ks;
                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidKeySpecException | IOException e) {
                    throw new SamlKeyException(e);
                }
            }
        };
    }
}
