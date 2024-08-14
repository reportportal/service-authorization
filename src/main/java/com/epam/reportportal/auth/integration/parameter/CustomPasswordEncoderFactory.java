/*
 * Copyright 2024 EPAM Systems
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
package com.epam.reportportal.auth.integration.parameter;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class CustomPasswordEncoderFactory {

  public static PasswordEncoder createDelegatingPasswordEncoder() {
    String encodingId = "bcrypt";
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(encodingId, new BCryptPasswordEncoder());
    encoders.put("ldap", new org.springframework.security.crypto.password.LdapShaPasswordEncoder());
    encoders.put("MD4", new org.springframework.security.crypto.password.Md4PasswordEncoder());
    encoders.put("MD5", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("MD5"));
    encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
    encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
    Pbkdf2PasswordEncoder PBKDF2_SHA256 = new Pbkdf2PasswordEncoder();
    PBKDF2_SHA256.setAlgorithm(SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    encoders.put("PBKDF2_SHA256", PBKDF2_SHA256);
    Pbkdf2PasswordEncoder PBKDF2_SHA512 = new Pbkdf2PasswordEncoder();
    PBKDF2_SHA512.setAlgorithm(SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
    encoders.put("PBKDF2_SHA512", new Pbkdf2PasswordEncoder());
    encoders.put("scrypt", new SCryptPasswordEncoder());
    encoders.put("SHA-1", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-1"));
    encoders.put("SHA-256",
        new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-256"));
    encoders.put("sha256", new org.springframework.security.crypto.password.StandardPasswordEncoder());
    encoders.put("argon2", new Argon2PasswordEncoder());
    return new DelegatingPasswordEncoder(encodingId, encoders);
  }

}
