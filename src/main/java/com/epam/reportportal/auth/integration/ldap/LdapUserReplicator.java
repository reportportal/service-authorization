/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.integration.ldap;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.ta.reportportal.commons.EntityUtils;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.database.entity.user.UserType;
import com.epam.ta.reportportal.database.personal.PersonalProjectService;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;

public class LdapUserReplicator extends AbstractUserReplicator {

	@Autowired
	public LdapUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
			PersonalProjectService personalProjectService) {
		super(userRepository, projectRepository, personalProjectService);
	}

	/**
	 * Replicates LDAP user to internal database (if does NOT exist). Creates personal project for that user
	 *
	 * @param name  Username
	 * @param email User's email
	 * @return Internal User representation
	 */
	public User replicateUser(String name, String email) {
		String login = EntityUtils.normalizeId(name);
		if (Strings.isNullOrEmpty(email)) {
			throw new UserSynchronizationException("Email not provided");
		}

		User user = userRepository.findOne(login);
		if (null == user) {
			user = new User();
			user.setLogin(login);

			checkEmail(email);
			user.setEmail(EntityUtils.normalizeId(email));
			user.setMetaInfo(defaultMetaInfo());

			user.setType(UserType.GITHUB);
			user.setRole(UserRole.USER);
			user.setIsExpired(false);

			user.setDefaultProject(generatePersonalProject(user));
			userRepository.save(user);

		} else if (!UserType.LDAP.equals(user.getType())) {
			//if user with such login exists, but it's not GitHub user than throw an exception
			throw new UserSynchronizationException("User with login '" + user.getId() + "' already exists");
		}
		return user;
	}

}
