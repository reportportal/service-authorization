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
package com.epam.reportportal.auth.integration.converter;

import com.epam.ta.reportportal.entity.ldap.AbstractLdapIntegration;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapAttributes;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.SynchronizationAttributesResource;

import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class LdapConverter {

	private LdapConverter() {
		//static only
	}

	public static final Function<? super AbstractLdapIntegration, LdapAttributes> LDAP_ATTRIBUTES_TO_RESOURCE = ldapIntegration -> {

		LdapAttributes ldapAttributes = new LdapAttributes();
		ldapAttributes.setBaseDn(ldapIntegration.getBaseDn());
		ldapAttributes.setEnabled(ldapIntegration.isEnabled());
		ldapAttributes.setUrl(ldapIntegration.getUrl());

		ofNullable(ldapIntegration.getSynchronizationAttributes()).ifPresent(synchronizationAttributes -> {
			SynchronizationAttributesResource attributes = new SynchronizationAttributesResource();
			attributes.setPhoto(synchronizationAttributes.getPhoto());
			attributes.setEmail(synchronizationAttributes.getEmail());
			attributes.setFullName(synchronizationAttributes.getFullName());
			ldapAttributes.setSynchronizationAttributes(attributes);
		});

		return ldapAttributes;
	};

	public static final Function<LdapConfig, LdapResource> TO_RESOURCE = ldapConfig -> {

		LdapResource ldapResource = new LdapResource();
		ldapResource.setId(ldapConfig.getId());
		ldapResource.setLdapAttributes(LDAP_ATTRIBUTES_TO_RESOURCE.apply(ldapConfig));
		ldapResource.setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
		ldapResource.setGroupSearchBase(ldapConfig.getGroupSearchBase());
		ldapResource.setManagerDn(ldapConfig.getManagerDn());
		ldapResource.setManagerPassword(ldapConfig.getManagerPassword());
		ldapResource.setPasswordAttribute(ldapConfig.getPasswordAttribute());
		ofNullable(ldapConfig.getPasswordEncoderType()).ifPresent(type -> ldapResource.setPasswordEncoderType(type.name()));
		ldapResource.setUserDnPattern(ldapConfig.getUserDnPattern());
		ldapResource.setUserSearchFilter(ldapConfig.getUserSearchFilter());

		return ldapResource;
	};
}
