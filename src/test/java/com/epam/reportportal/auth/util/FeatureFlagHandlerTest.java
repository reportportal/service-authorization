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
package com.epam.reportportal.auth.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.reportportal.auth.entity.enums.FeatureFlag;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class FeatureFlagHandlerTest {

  @Test
  void testConstructorWithEmptyFlags() {
    FeatureFlagHandler  featureFlagHandler = new FeatureFlagHandler(Collections.emptySet());
    assertFalse(featureFlagHandler.isEnabled(FeatureFlag.SINGLE_BUCKET));
  }

  @Test
  void testConstructorWithValidFlags() {
    FeatureFlagHandler  featureFlagHandler = new FeatureFlagHandler(Set.of("singleBucket", "defaultLdapEncoder"));
    assertTrue(featureFlagHandler.isEnabled(FeatureFlag.SINGLE_BUCKET));
    assertTrue(featureFlagHandler.isEnabled(FeatureFlag.DEFAULT_LDAP_ENCODER));
  }

}
