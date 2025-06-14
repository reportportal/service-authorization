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

package com.epam.reportportal.auth.dao;

import com.epam.reportportal.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryCustomImplTest extends BaseTest {

  @Autowired
  UserRepository userRepository;

  @Test
  void findUserDetails() {
    var user = userRepository.findByLogin("admin@reportportal.internal")
        .orElseThrow();
    Assertions.assertEquals("admin@reportportal.internal", user.getLogin());
  }

  @Test
  void findUserDetailsNotFound() {
    var user = userRepository.findByLogin("notfound");
    Assertions.assertTrue(user.isEmpty());
  }

}
