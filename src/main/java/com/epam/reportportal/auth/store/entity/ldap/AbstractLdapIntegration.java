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

import com.epam.reportportal.auth.validation.IfEnabled;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.google.common.base.MoreObjects;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * General properties for both LDAP and AD authentication types
 *
 * @author Andrei Varabyeu
 */

@MappedSuperclass
@PrimaryKeyJoinColumn(name = "id")
public class AbstractLdapIntegration extends Integration {

	@Pattern(regexp = "^ldaps?://.*")
	@NotEmpty(groups = { IfEnabled.class })
	@Column(name = "url", length = 256)
	private String url;

	@NotNull(groups = { IfEnabled.class })
	@Column(name = "base_dn", length = 256)
	private String baseDn;

	@ManyToOne
	@JoinColumn(name = "sync_attributes_id")
	private SynchronizationAttributes synchronizationAttributes;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBaseDn() {
		return baseDn;
	}

	public void setBaseDn(String baseDn) {
		this.baseDn = baseDn;
	}

	public SynchronizationAttributes getSynchronizationAttributes() {
		return synchronizationAttributes;
	}

	public void setSynchronizationAttributes(SynchronizationAttributes synchronizationAttributes) {
		this.synchronizationAttributes = synchronizationAttributes;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("url", url)
				.add("baseDn", baseDn)
				.add("synchronizationAttributes", synchronizationAttributes)
				.add("super1", super.isEnabled())
				.toString();
	}
}
