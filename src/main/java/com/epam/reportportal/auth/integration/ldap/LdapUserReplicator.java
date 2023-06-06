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
package com.epam.reportportal.auth.integration.ldap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;
import static com.epam.reportportal.auth.util.AuthUtils.CROP_DOMAIN;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.reportportal.commons.ContentTypeResolver;
import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.util.PersonalProjectService;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LDAP replicator
 *
 * @author Andrei Varabyeu
 */
@Component
public class LdapUserReplicator extends AbstractUserReplicator {

	private static final String EMAIL_NOT_PROVIDED_MSG = "Email not provided";
	private static final String USER_ALREADY_EXISTS_MSG = "User with login '%s' already exists";
	private static final String EMAIL_ATTRIBUTE_NOT_PROVIDED_MSG = "Email attribute not provided";

  @Autowired
	public LdapUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
			PersonalProjectService personalProjectService, UserBinaryDataService userBinaryDataService,
			ContentTypeResolver contentTypeResolver) {
		super(userRepository, projectRepository, personalProjectService, userBinaryDataService, contentTypeResolver);
	}

  /**
   * Replicates LDAP user to internal database (if does NOT exist). Creates personal project for
   * that user
   *
   * @param name      Username
   * @param ctx       LDAP context
   * @param syncAttrs Synchronization Attributes
   * @return Internal User representation
   */
	@Transactional
	public User replicateUser(String name, DirContextOperations ctx, Map<String, String> syncAttrs) {
		String emailAttribute = ofNullable(
				syncAttrs.get(LdapParameter.EMAIL_ATTRIBUTE.getParameterName()))
				.orElseThrow(() -> new UserSynchronizationException(EMAIL_ATTRIBUTE_NOT_PROVIDED_MSG));

		String emailFromContext = (String) ctx.getObjectAttribute(emailAttribute);
		String email = validateEmail(emailFromContext);
		String login = CROP_DOMAIN.apply(name);

		Optional<User> userOptional = userRepository.findByLogin(login);

		if (userOptional.isEmpty()) {
			return createNewUser(ctx, syncAttrs, email, login);
		}

		User user = userOptional.get();
		checkUserType(user);
		updateEmailIfNeeded(email, user);

		return user;
	}

	private String validateEmail(String email) {
		if (isNullOrEmpty(email)) {
			throw new UserSynchronizationException(EMAIL_NOT_PROVIDED_MSG);
		}
		return email.toLowerCase();
	}

	private User createNewUser(DirContextOperations ctx, Map<String, String> syncAttributes,
			String email,	String login) {
		User newUser = new User();
		newUser.setLogin(login);

		ofNullable(syncAttributes.get(LdapParameter.FULL_NAME_ATTRIBUTE.getParameterName()))
				.filter(StringUtils::isNotBlank)
				.flatMap(it -> ofNullable(ctx.getStringAttribute(it)))
				.ifPresent(newUser::setFullName);

		checkEmail(email);
		newUser.setEmail(email);
		newUser.setMetadata(defaultMetaData());
		newUser.setUserType(UserType.LDAP);
		newUser.setRole(UserRole.USER);
		newUser.setExpired(false);

		final Project project = generatePersonalProject(newUser);
		newUser.getProjects().add(project.getUsers().iterator().next());

		return userRepository.save(newUser);
	}

  private void checkUserType(User user) {
    if (!UserType.LDAP.equals(user.getUserType())) {
      String login = user.getLogin();
      throw new UserSynchronizationException(String.format(USER_ALREADY_EXISTS_MSG, login));
    }
  }

	private void updateEmailIfNeeded(String email, User user) {
		if (!StringUtils.equals(user.getEmail(), email)) {
			user.setEmail(email);
			userRepository.save(user);
		}
	}

}
