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
