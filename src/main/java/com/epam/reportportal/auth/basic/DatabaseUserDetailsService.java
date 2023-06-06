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
package com.epam.reportportal.auth.basic;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;

import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring's {@link UserDetailsService} implementation. Uses {@link User} entity from ReportPortal
 * database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class DatabaseUserDetailsService implements UserDetailsService {

  private UserRepository userRepository;

  @Autowired
  public void setUserRepository(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    ReportPortalUser user = userRepository.findUserDetails(normalizeId(username))
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword() == null ? "" : user.getPassword())
        .authorities(AuthUtils.AS_AUTHORITIES.apply(user.getUserRole()))
        .build();

    return ReportPortalUser.userBuilder()
        .withUserDetails(userDetails)
        .withProjectDetails(user.getProjectDetails())
        .withUserId(user.getUserId())
        .withUserRole(user.getUserRole())
        .withEmail(user.getEmail())
        .build();
  }
}