package com.epam.reportportal.auth.integration.builder;

import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.entity.ldap.SynchronizationAttributes;
import com.epam.ta.reportportal.ws.model.integration.auth.SynchronizationAttributesResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateActiveDirectoryRQ;

import javax.validation.constraints.NotNull;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public final class ActiveDirectoryBuilder {

	private final ActiveDirectoryConfig activeDirectoryConfig;

	public ActiveDirectoryBuilder() {
		activeDirectoryConfig = new ActiveDirectoryConfig();
	}

	public ActiveDirectoryBuilder(ActiveDirectoryConfig activeDirectoryConfig) {
		this.activeDirectoryConfig = activeDirectoryConfig;
	}

	public ActiveDirectoryBuilder addUpdateRq(UpdateActiveDirectoryRQ updateActiveDirectoryRQ) {

		SynchronizationAttributes attributes = ofNullable(activeDirectoryConfig.getSynchronizationAttributes()).orElseGet(
				SynchronizationAttributes::new);

		SynchronizationAttributesResource attributesResource = updateActiveDirectoryRQ.getLdapAttributes().getSynchronizationAttributes();
		attributes.setPhoto(attributesResource.getPhoto());
		attributes.setEmail(attributesResource.getEmail());
		attributes.setFullName(attributesResource.getFullName());
		activeDirectoryConfig.setSynchronizationAttributes(attributes);

		activeDirectoryConfig.setEnabled(updateActiveDirectoryRQ.getLdapAttributes().getEnabled());
		activeDirectoryConfig.setUrl(updateActiveDirectoryRQ.getLdapAttributes().getUrl());
		activeDirectoryConfig.setBaseDn(updateActiveDirectoryRQ.getLdapAttributes().getBaseDn());

		activeDirectoryConfig.setDomain(updateActiveDirectoryRQ.getDomain());

		return this;
	}

	public @NotNull ActiveDirectoryConfig build() {
		return activeDirectoryConfig;
	}

}
