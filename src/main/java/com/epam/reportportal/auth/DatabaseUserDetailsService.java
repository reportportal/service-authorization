/*
 * Copyright 2016 EPAM Systems
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
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.UserRoleDetails;
import com.epam.ta.reportportal.database.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.stream.Collectors;

/**
 * Spring's {@link UserDetailsService} implementation. Uses {@link User} entity
 * from ReportPortal database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
class DatabaseUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserRoleDetails userEntity = userRepository.aggregateUserProjects(username.toLowerCase());
		if (null == userEntity || null == userEntity.getUser()) {
			throw new UsernameNotFoundException("Username '" + username + "' not found");
		}

		String login = userEntity.getUser().getLogin();
		String password = userEntity.getUser().getPassword() == null ? "" : userEntity.getUser().getPassword();


		org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(login, password, true,
				true, true, true, AuthUtils.AS_AUTHORITIES.apply(userEntity.getUser().getRole()));
		return new ReportPortalUser(u, userEntity.getProjects().stream().collect(Collectors.toMap(UserRoleDetails.ProjectDetails::getProject,
				UserRoleDetails.ProjectDetails::getProjectRole)));
	}
}