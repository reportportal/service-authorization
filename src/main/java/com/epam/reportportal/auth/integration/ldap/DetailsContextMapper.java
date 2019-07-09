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

import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.entity.ldap.SynchronizationAttributes;
import com.epam.ta.reportportal.entity.user.ProjectUser;
import com.epam.ta.reportportal.entity.user.User;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author Details Context mapper
 */
class DetailsContextMapper extends LdapUserDetailsMapper {

	private final LdapUserReplicator ldapUserReplicator;
	private final SynchronizationAttributes attributes;

	DetailsContextMapper(LdapUserReplicator ldapUserReplicator, SynchronizationAttributes attributes) {
		this.ldapUserReplicator = ldapUserReplicator;
		this.attributes = attributes;
	}

	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
		UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);

		User user = ldapUserReplicator.replicateUser(userDetails.getUsername(), ctx, attributes);

		org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(user.getLogin(),
				"",
				true,
				true,
				true,
				true,
				AuthUtils.AS_AUTHORITIES.apply(user.getRole())
		);

		Optional<Set<ProjectUser>> optionalProjectUser = ofNullable(user.getProjects());

		return new ReportPortalUser(u,
				user.getId(),
				user.getRole(),
				optionalProjectUser.map(it -> it.stream().collect(Collectors.toMap(p -> p.getProject().getName(),
						p -> new ReportPortalUser.ProjectDetails(p.getProject().getId(), p.getProject().getName(), p.getProjectRole())
				))).orElseGet(Collections::emptyMap),
				user.getEmail()
		);
	}
}
