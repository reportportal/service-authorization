/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/commons-dao
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
package com.epam.reportportal.auth.store.entity.ldap;

import com.epam.reportportal.auth.validation.AdSequenceProvider;
import com.epam.reportportal.auth.validation.IfEnabled;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Active Directory auth config
 *
 * @author Andrei Varabyeu
 */
@GroupSequenceProvider(AdSequenceProvider.class)
@Entity
@Table(name = "active_directory_config", schema = "public")
public class ActiveDirectoryConfig extends AbstractLdapIntegration {

	@NotNull(groups = { IfEnabled.class })
	@Column(name = "domain", length = 256)
	private String domain;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
