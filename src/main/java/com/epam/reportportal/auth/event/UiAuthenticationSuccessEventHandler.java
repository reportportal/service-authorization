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

package com.epam.reportportal.auth.event;

import com.epam.reportportal.auth.integration.saml.ReportPortalSamlAuthentication;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.util.PersonalProjectService;
import com.epam.reportportal.rules.exception.ErrorType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates Last Login field in database User entity.
 *
 * @author Andrei Varabyeu
 */
@Component
public class UiAuthenticationSuccessEventHandler {

  private UserRepository userRepository;

  private PersonalProjectService personalProjectService;

  @Autowired
  public UiAuthenticationSuccessEventHandler(UserRepository userRepository,
      PersonalProjectService personalProjectService) {
    this.userRepository = userRepository;
    this.personalProjectService = personalProjectService;
  }

  @EventListener
  @Transactional
  public void onApplicationEvent(UiUserSignedInEvent event) {
    String username = event.getAuthentication().getName();
    userRepository.updateLastLoginDate(
        Instant.ofEpochMilli(event.getTimestamp()),
        username);

    if (MapUtils.isEmpty(acquireUser(event.getAuthentication()).getProjectDetails())) {
      User user = userRepository.findByLogin(username)
          .orElseThrow(() -> new ReportPortalException(ErrorType.USER_NOT_FOUND, username));
      Project project = personalProjectService.generatePersonalProject(user);
      user.getProjects().addAll(project.getUsers());
    }
  }

  private ReportPortalUser acquireUser(Authentication authentication) {
    if (authentication instanceof ReportPortalSamlAuthentication rpAuth) {
      return userRepository.findUserDetails(rpAuth.getPrincipal())
          .orElseThrow(() ->
              new ReportPortalException(ErrorType.USER_NOT_FOUND, rpAuth.getPrincipal()));
    } else {
      return (ReportPortalUser) authentication.getPrincipal();
    }
  }
}
