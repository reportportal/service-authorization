/*
 * Copyright 2025 EPAM Systems
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

package com.epam.reportportal.auth.endpoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.reportportal.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for OAuthConfigurationEndpoint. Tests the security configuration of the GET /settings/oauth endpoint.
 */
public class OAuthConfigurationEndpointTest extends BaseTest {

  public static final String SETTING_OAUTH_URL = "/settings/oauth";
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void testGetOAuthSettingsWithoutAuthentication() throws Exception {
    mockMvc.perform(get(SETTING_OAUTH_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetOAuthSettingsByAdmin() throws Exception {
    mockMvc.perform(get(SETTING_OAUTH_URL)
            .with(token(oAuthHelper.getSuperadminToken())))
        .andExpect(status().isOk());
  }

  @Test
  public void testOAuthEndpointByNonAdmin() throws Exception {
    mockMvc.perform(get(SETTING_OAUTH_URL)
            .with(token(oAuthHelper.getDefaultToken())))
        .andExpect(status().isUnauthorized());
  }
}
