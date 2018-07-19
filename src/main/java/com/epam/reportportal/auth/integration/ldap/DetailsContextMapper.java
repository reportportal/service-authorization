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

import com.epam.reportportal.auth.ReportPortalUser;
import com.epam.reportportal.auth.util.AuthUtils;
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

		org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(
				user.getLogin(),
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
				user.getRole(), optionalProjectUser.orElse(Collections.emptySet())
						.stream()
						.collect(Collectors.toMap(p -> p.getProject().getName(),
								p -> new ReportPortalUser.ProjectDetails(p.getProject().getId(), p.getRole())
						))
		);
	}
}
