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
package com.epam.reportportal.auth.basic;

import com.epam.reportportal.auth.ReportPortalUser;
import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.jooq.tables.pojos.Project;
import com.epam.ta.reportportal.jooq.tables.pojos.Users;
import com.epam.ta.reportportal.jooq.tables.pojos.UsersProject;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.ta.reportportal.jooq.Tables.*;

/**
 * Spring's {@link UserDetailsService} implementation. Uses {@link Users} entity
 * from ReportPortal database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class DatabaseUserDetailsService implements UserDetailsService {

	@Autowired
	private DSLContext dsl;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Map<Users, List<FullProject>> userProjects = dsl.select()
				.from(USERS)
				.join(USERS_PROJECT)
				.on(USERS_PROJECT.USER_ID.eq(USERS.ID))
				.join(PROJECT)
				.on(USERS_PROJECT.PROJECT_ID.eq(PROJECT.ID))
				.where(USERS.LOGIN.eq(username))
				.fetchGroups(
						// Map records first into the USER table and then into the key POJO type
						r -> r.into(Users.class),

						// Map records first into the ROLE table and then into the value POJO type
						r -> new FullProject(r.into(Project.class), r.into(UsersProject.class))
				);

		if (userProjects.isEmpty()) {
			throw new UsernameNotFoundException("User not found");
		}
		Map.Entry<Users, List<FullProject>> userProject = userProjects.entrySet().iterator().next();
		Users userEntity = userProject.getKey();

		String login = userEntity.getLogin();
		String password = userEntity.getPassword() == null ? "" : userEntity.getPassword();

		org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(
				login,
				password,
				true,
				true,
				true,
				true,
				AuthUtils.AS_AUTHORITIES.apply(UserRole.findByName(userEntity.getRole().name()).get())
		);
		return new ReportPortalUser(
				u,
				userProject.getValue().stream().collect(Collectors.toMap(p -> p.project.getName(), p -> p.usersProject.getProjectRole()))
		);
	}

	static class FullProject {
		UsersProject usersProject;
		Project project;

		FullProject(Project project, UsersProject usersProject) {
			this.usersProject = usersProject;
			this.project = project;
		}
	}
}