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

import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;

import java.util.function.Function;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class ActiveDirectoryConverter {

	private ActiveDirectoryConverter() {
		//static only
	}

	public static final Function<ActiveDirectoryConfig, ActiveDirectoryResource> TO_RESOURCE = activeDirectoryConfig -> {

		ActiveDirectoryResource resource = new ActiveDirectoryResource();
		resource.setLdapAttributes(LdapConverter.LDAP_ATTRIBUTES_TO_RESOURCE.apply(activeDirectoryConfig));
		resource.setId(activeDirectoryConfig.getId());
		resource.setDomain(activeDirectoryConfig.getDomain());

		return resource;
	};
}
