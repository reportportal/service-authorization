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

package com.epam.reportportal.auth.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

/**
 * Configuration class for unit tests.
 */
@SpringBootConfiguration
@EnableConfigurationProperties
@EnableAutoConfiguration(exclude = {QuartzAutoConfiguration.class})
@ComponentScan(basePackages = {"com.epam.reportportal"})
public class TestConfig {

  @Bean
  @Profile("unittest")
  public JwtAccessTokenConverter accessTokenConverter(AccessTokenConverter accessTokenConverter) {
    JwtAccessTokenConverter jwtConverter = new JwtAccessTokenConverter();
    jwtConverter.setSigningKey("123");
    jwtConverter.setAccessTokenConverter(accessTokenConverter);
    return jwtConverter;
  }


}
