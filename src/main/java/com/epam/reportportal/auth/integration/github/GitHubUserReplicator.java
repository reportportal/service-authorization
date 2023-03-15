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
package com.epam.reportportal.auth.integration.github;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.reportportal.commons.ContentTypeResolver;
import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.Metadata;
import com.epam.ta.reportportal.entity.attachment.BinaryData;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.util.PersonalProjectService;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replicates GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class GitHubUserReplicator extends AbstractUserReplicator {

  public GitHubUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
      PersonalProjectService personalProjectService, UserBinaryDataService userBinaryDataService,
      ContentTypeResolver contentTypeResolver) {
    super(userRepository, projectRepository, personalProjectService, userBinaryDataService,
        contentTypeResolver);
  }

  public User synchronizeUser(String accessToken) {
    GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
    UserResource userResource = gitHubClient.getUser();
    Optional<User> userOptional = userRepository.findByLogin(normalizeId(userResource.getLogin()));
    BusinessRule.expect(userOptional, Optional::isPresent)
        .verify(ErrorType.USER_NOT_FOUND, userResource.getLogin());
    User user = userOptional.get();
    BusinessRule.expect(user.getUserType(), userType -> Objects.equals(userType, UserType.GITHUB))
        .verify(ErrorType.INCORRECT_AUTHENTICATION_TYPE,
            "User '" + userResource.getLogin() + "' is not GitHUB user");
    if (StringUtils.isNotBlank(userResource.getName())) {
      user.setFullName(userResource.getName());
    }
    String email = userResource.getEmail();
    if (Strings.isNullOrEmpty(email)) {
      email = retrieveEmail(gitHubClient).orElseThrow(
          () -> new UserSynchronizationException("User 'email' has not been provided"));
    }
    email = normalizeId(email);
    if (!user.getEmail().equals(email)) {
      checkEmail(email);
      user.setEmail(email);
    }
    Metadata metadata = ofNullable(user.getMetadata()).orElse(new Metadata(Maps.newHashMap()));
    metadata.getMetadata()
        .put("synchronizationDate", Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant()));
    user.setMetadata(metadata);

    uploadAvatar(gitHubClient, user, userResource.getAvatarUrl());
    userRepository.save(user);
    return user;
  }

  /**
   * Replicates GitHub user to internal database (if does NOT exist). Updates if exist. Creates
   * personal project for that user
   *
   * @param userResource GitHub user to be replicated
   * @param gitHubClient Configured github client
   * @return Internal User representation
   */
  @Transactional
  public ReportPortalUser replicateUser(UserResource userResource, GitHubClient gitHubClient) {
    String login = normalizeId(userResource.getLogin());
    User user = userRepository.findByLogin(login).map(u -> {
      if (UserType.GITHUB.equals(u.getUserType())) {
        updateUser(u, userResource, gitHubClient);
      } else {
        //if user with such login exists, but it's not GitHub user than throw an exception
        throw new UserSynchronizationException(
            "User with login '" + u.getLogin() + "' already exists");
      }
      return u;
    }).orElseGet(() -> userRepository.save(createUser(userResource, gitHubClient)));

    return ReportPortalUser.userBuilder().fromUser(user);
  }

  private void updateUser(User user, UserResource userResource, GitHubClient gitHubClient) {
    String email = userResource.getEmail();
    if (Strings.isNullOrEmpty(email)) {
      email = retrieveEmail(gitHubClient).orElseThrow(
          () -> new UserSynchronizationException("User 'email' has not been provided"));
    }
    email = normalizeId(email);
    if (StringUtils.isBlank(user.getEmail()) || !user.getEmail().equals(email)) {
      checkEmail(email);
      user.setEmail(email);
    }
    user.setFullName(
        isNullOrEmpty(userResource.getName()) ? user.getLogin() : userResource.getName());
    user.setMetadata(defaultMetaData());
    uploadAvatar(gitHubClient, user, userResource.getAvatarUrl());
  }

  private User createUser(UserResource userResource, GitHubClient gitHubClient) {
    User user = new User();
    String login = normalizeId(userResource.getLogin());
    user.setLogin(login);
    updateUser(user, userResource, gitHubClient);
    user.setUserType(UserType.GITHUB);
    user.setRole(UserRole.USER);
    user.setExpired(false);
    final Project project = generatePersonalProject(user);
    user.getProjects().addAll(project.getUsers());
    return user;
  }

  private void uploadAvatar(GitHubClient gitHubClient, User user, String avatarUrl) {
    if (null != avatarUrl) {
      ResponseEntity<Resource> photoRs = gitHubClient.downloadResource(avatarUrl);
      try (InputStream photoStream = photoRs.getBody().getInputStream()) {
        BinaryData photo = new BinaryData(photoRs.getHeaders().getContentType().toString(),
            photoRs.getBody().contentLength(),
            photoStream
        );
        uploadPhoto(user, photo);
      } catch (IOException e) {
        LOGGER.error("Unable to load photo for user {}", user.getLogin());
      }
    }
  }

  private Optional<String> retrieveEmail(GitHubClient gitHubClient) {
    return gitHubClient.getUserEmails()
        .stream()
        .filter(EmailResource::isVerified)
        .filter(EmailResource::isPrimary)
        .findFirst()
        .map(EmailResource::getEmail);
  }
}
