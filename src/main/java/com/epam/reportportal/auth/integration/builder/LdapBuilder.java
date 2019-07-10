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
package com.epam.reportportal.auth.integration.builder;

import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.entity.ldap.PasswordEncoderType;
import com.epam.ta.reportportal.entity.ldap.SynchronizationAttributes;
import com.epam.ta.reportportal.ws.model.integration.auth.SynchronizationAttributesResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class LdapBuilder {

	private final LdapConfig ldapConfig;

	public LdapBuilder() {
		ldapConfig = new LdapConfig();
	}

	public LdapBuilder(LdapConfig ldapConfig) {
		this.ldapConfig = ldapConfig;
	}

	public LdapBuilder addUpdateRq(UpdateLdapRQ updateLdapRQ) {
		ldapConfig.setEnabled(updateLdapRQ.getLdapAttributes().getEnabled());
		if (StringUtils.isBlank(updateLdapRQ.getPasswordEncoderType())) {
			ldapConfig.setPasswordEncoderType(null);
		} else {
			PasswordEncoderType.findByType(updateLdapRQ.getPasswordEncoderType()).ifPresent(ldapConfig::setPasswordEncoderType);
		}
		ldapConfig.setManagerPassword(updateLdapRQ.getManagerPassword());
		ldapConfig.setGroupSearchBase(updateLdapRQ.getGroupSearchBase());
		ldapConfig.setGroupSearchFilter(updateLdapRQ.getGroupSearchFilter());
		ldapConfig.setManagerDn(updateLdapRQ.getManagerDn());
		ldapConfig.setPasswordAttribute(updateLdapRQ.getPasswordAttribute());
		ldapConfig.setUserDnPattern(updateLdapRQ.getUserDnPattern());
		ldapConfig.setUserSearchFilter(updateLdapRQ.getUserSearchFilter());

		ldapConfig.setUrl(updateLdapRQ.getLdapAttributes().getUrl());
		ldapConfig.setBaseDn(updateLdapRQ.getLdapAttributes().getBaseDn());

		SynchronizationAttributes attributes = ofNullable(ldapConfig.getSynchronizationAttributes()).orElseGet(SynchronizationAttributes::new);

		SynchronizationAttributesResource attributesResource = updateLdapRQ.getLdapAttributes().getSynchronizationAttributes();
		attributes.setPhoto(attributesResource.getPhoto());
		attributes.setEmail(attributesResource.getEmail());
		attributes.setFullName(attributesResource.getFullName());

		ldapConfig.setSynchronizationAttributes(attributes);
		return this;
	}

	public @NotNull LdapConfig build() {
		return ldapConfig;
	}
}
