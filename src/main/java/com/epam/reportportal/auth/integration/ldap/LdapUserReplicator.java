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

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.ldap.SynchronizationAttributes;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.filesystem.DataEncoder;
import com.epam.ta.reportportal.filesystem.DataStore;
import com.epam.ta.reportportal.util.PersonalProjectService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;

/**
 * LDAP replicator
 *
 * @author Andrei Varabyeu
 */
@Component
public class LdapUserReplicator extends AbstractUserReplicator {

	@Autowired
	public LdapUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
			PersonalProjectService personalProjectService, DataStore dataStorage, DataEncoder encoder) {
		super(userRepository, projectRepository, personalProjectService, dataStorage, encoder);
	}

	/**
	 * Replicates LDAP user to internal database (if does NOT exist). Creates personal project for that user
	 *
	 * @param name       Username
	 * @param ctx        LDAP context
	 * @param attributes Synchronization Attributes
	 * @return Internal User representation
	 */
	@Transactional
	public User replicateUser(String name, DirContextOperations ctx, SynchronizationAttributes attributes) {
		String email = (String) ctx.getObjectAttribute(attributes.getEmail());
		if (isNullOrEmpty(email)) {
			throw new UserSynchronizationException("Email not provided");
		}
		email = normalizeId(email);

		String login = normalizeId(StringUtils.substringBefore(name, "@"));
		Optional<User> userOptional = userRepository.findByLogin(login);
		if (!userOptional.isPresent()) {
			User newUser = new User();
			newUser.setLogin(login);

			ofNullable(attributes.getFullName()).flatMap(it -> ofNullable(ctx.getStringAttribute(it))).ifPresent(newUser::setFullName);

			ofNullable(attributes.getPhoto()).flatMap(it -> ofNullable(ctx.getObjectAttribute(it)))
					.filter(photo -> photo instanceof byte[])
					.map(photo -> (byte[]) photo)
					.ifPresent(photo -> newUser.setAttachment(uploadPhoto(login, photo)));

			checkEmail(email);
			newUser.setEmail(email);
			newUser.setMetadata(defaultMetaData());

			newUser.setUserType(UserType.LDAP);
			newUser.setRole(UserRole.USER);
			newUser.setExpired(false);

			final Project project = generatePersonalProject(newUser);
			newUser.getProjects().add(project.getUsers().iterator().next());
			userRepository.save(newUser);

			return newUser;

		} else if (!UserType.LDAP.equals(userOptional.get().getUserType())) {
			//if user with such login exists, but it's not GitHub user than throw an exception
			throw new UserSynchronizationException("User with login '" + userOptional.get().getId() + "' already exists");
		}
		return userOptional.get();
	}

}
