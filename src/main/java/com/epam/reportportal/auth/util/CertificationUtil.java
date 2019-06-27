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
package com.epam.reportportal.auth.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utility class for loading certificates from trusted stores
 *
 * @author Yevgeniy Svalukhin
 */
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
