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

import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.entity.user.User;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Details Context mapper
 */
public class DetailsContextMapper extends LdapUserDetailsMapper {

	private final LdapUserReplicator ldapUserReplicator;
	private final Supplier<Map<String, String>> attributes;

	public DetailsContextMapper(LdapUserReplicator ldapUserReplicator, Supplier<Map<String, String>> attributes) {
		this.ldapUserReplicator = ldapUserReplicator;
		this.attributes = attributes;
	}

	@Override
	@Transactional
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
		UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
		User user = ldapUserReplicator.replicateUser(userDetails.getUsername(), ctx, attributes.get());
		return ReportPortalUser.userBuilder().fromUser(user);
	}
}
