package com.epam.reportportal.auth.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificationUtil {

    public static X509Certificate getCertificateByName(String certificateAlias, String trustStoreName, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            loadKeyStore(keyStore, trustStoreName, password);
            Certificate cert = keyStore.getCertificate(certificateAlias);
            if (cert.getType().equals("X.509")) {
                return (X509Certificate) cert;
            }
            throw new Error("Could not find a suitable x509 certificate for alias " + certificateAlias + " in " + trustStoreName);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new Error("Error opening keystore: " + e.getCause(), e);
        }
    }

    public static PrivateKey getPrivateKey(String keyAlias, String keyPass, String trustStore, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            loadKeyStore(keyStore, trustStore, password);
            Key key = keyStore.getKey(keyAlias, keyPass.toCharArray());
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            }
            throw new Error("Unable to find private key in store: " + trustStore);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException e) {
            throw new Error("Error opening keystore: " + e.getCause(), e);
        }
    }


    private static void loadKeyStore(KeyStore keyStore, String jksPath, String jksPassword)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        char[] password = null;
        if (jksPassword != null) {
            password = jksPassword.toCharArray();
        }
        if (jksPath.startsWith("file://")) {
            keyStore.load(Files.newInputStream(Paths.get(jksPath.replaceFirst("file://", ""))), password);
        } else {
            keyStore.load(ClassLoader.getSystemResourceAsStream(jksPath), password);
        }
    }
}
