package com.epam.reportportal.auth.integration.converter;

import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapAttributes;

import java.util.function.Function;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class ActiveDirectoryConverter {

	private ActiveDirectoryConverter() {
		//static only
	}

	public static final Function<ActiveDirectoryConfig, ActiveDirectoryResource> TO_RESOURCE = activeDirectoryConfig -> {

		LdapAttributes attributes = LdapConverter.LDAP_ATTRIBUTES_TO_RESOURCE.apply(activeDirectoryConfig);

		ActiveDirectoryResource resource = new ActiveDirectoryResource();
		resource.setLdapAttributes(attributes);
		resource.setDomain(activeDirectoryConfig.getDomain());

		return resource;
	};
}
