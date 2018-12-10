package com.epam.reportportal.auth.integration.builder;

import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.entity.ldap.PasswordEncoderType;
import com.epam.ta.reportportal.entity.ldap.SynchronizationAttributes;
import com.epam.ta.reportportal.ws.model.integration.auth.SynchronizationAttributesResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;

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
		ldapConfig.setPasswordEncoderType(PasswordEncoderType.valueOf(updateLdapRQ.getPasswordEncoderType()));
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
