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
package com.epam.reportportal.auth.store.events;

import com.epam.reportportal.auth.FakeMongoConfig;
import com.epam.reportportal.auth.store.AuthConfigRepository;
import com.epam.reportportal.auth.store.entity.AuthConfigEntity;
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;
import com.epam.reportportal.auth.util.Encryptor;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Andrei Varabyeu
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FakeMongoConfig.class)
@ActiveProfiles("unittest")
public class AuthAttributesEventListenerTest {

	@Autowired
	private AuthConfigRepository repository;
	@Autowired
	private Encryptor encryptor;

	@Test
	public void testPassEncryption() {

		LdapConfig ldapConfig = new LdapConfig();
		ldapConfig.setManagerPassword(encryptor.encrypt("managerPassword"));
		ldapConfig.setUserDnPattern("userDnPattern");
		repository.updateLdap(ldapConfig);
		AuthConfigEntity entity = new AuthConfigEntity();
		entity.setLdap(ldapConfig);

		AuthConfigEntity dbEntity = repository.findDefault();
		Assert.assertThat(dbEntity.getLdap().getManagerPassword(), Matchers.is("managerPassword"));
	}

}