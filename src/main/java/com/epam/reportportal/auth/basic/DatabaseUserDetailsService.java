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

import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static java.util.Optional.ofNullable;

/**
 * Spring's {@link UserDetailsService} implementation. Uses {@link User} entity
 * from ReportPortal database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class DatabaseUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByLogin(normalizeId(username))
				.orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found."));

		String login = user.getLogin();
		String password = ofNullable(user.getPassword()).orElse("");

		org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(login,
				password,
				true,
				true,
				true,
				true, AuthUtils.AS_AUTHORITIES.apply(user.getRole())
		);

		return new ReportPortalUser(userDetails,
				user.getId(),
				user.getRole(),
				user.getProjects()
						.stream()
						.collect(Collectors.toMap(projectUser -> projectUser.getProject().getName(),
								projectUser -> new ReportPortalUser.ProjectDetails(projectUser.getProject().getId(),
										projectUser.getProject().getName(),
										projectUser.getProjectRole()
								)
						)),
				user.getEmail()
		);
	}
}