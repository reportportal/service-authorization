package com.epam.reportportal.auth.integration.converter;

import com.epam.ta.reportportal.entity.ldap.AbstractLdapIntegration;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapAttributes;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.SynchronizationAttributesResource;

import java.util.function.Function;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class LdapConverter {

	private LdapConverter() {
		//static only
	}

	public static final Function<? super AbstractLdapIntegration, LdapAttributes> LDAP_ATTRIBUTES_TO_RESOURCE = ldapIntegration -> {

		SynchronizationAttributesResource attributes = new SynchronizationAttributesResource();
		attributes.setPhoto(ldapIntegration.getSynchronizationAttributes().getPhoto());
		attributes.setEmail(ldapIntegration.getSynchronizationAttributes().getEmail());
		attributes.setFullName(ldapIntegration.getSynchronizationAttributes().getFullName());

		LdapAttributes ldapAttributes = new LdapAttributes();
		ldapAttributes.setSynchronizationAttributes(attributes);
		ldapAttributes.setBaseDn(ldapIntegration.getBaseDn());
		ldapAttributes.setEnabled(ldapIntegration.isEnabled());
		ldapAttributes.setUrl(ldapIntegration.getUrl());

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
		ldapResource.setPasswordEncoderType(ldapConfig.getPasswordEncoderType().name());
		ldapResource.setUserDnPattern(ldapConfig.getUserDnPattern());
		ldapResource.setUserSearchFilter(ldapConfig.getUserSearchFilter());

		return ldapResource;
	};
}
