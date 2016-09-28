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

import com.epam.ta.reportportal.database.entity.user.UserRole;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Andrei Varabyeu
 */
public class AuthUtilsTest {

	@Test
	public void testAuthoritiesConverter() {
		Collection<GrantedAuthority> grantedAuthorities = Collections.singletonList(UserRole.USER).stream().map(AuthUtils.AS_AUTHORITIES)
				.collect(Collectors.toList()).get(0);
		Assert.assertThat("Incorrect authority conversion", grantedAuthorities.iterator().next().getAuthority(),
				Matchers.is(UserRole.USER.getAuthority()));

	}
}
