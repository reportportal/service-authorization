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
package com.epam.reportportal.auth.integration.ldap;

import com.epam.reportportal.auth.store.AuthConfigRepository;
import com.epam.reportportal.auth.store.entity.ldap.PasswordEncoderType;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrei Varabyeu
 */
public class LdapAuthProviderTest {

	@Test
	public void testEncodingTypes() {
		for (PasswordEncoderType type : PasswordEncoderType.values()) {
			Assert.assertThat("Type " + type + " is not mapped", LdapAuthProvider.ENCODER_MAPPING, Matchers.hasKey(type));
		}
	}

	@Test
	public void isEnabled() throws Exception {
		AuthConfigRepository repoMock = mock(AuthConfigRepository.class);
		when(repoMock.findLdap(true)).thenReturn(Optional.empty());
		Assert.assertFalse(new LdapAuthProvider(repoMock, mock(LdapUserReplicator.class)).isEnabled());
	}
}