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
package com.epam.reportportal.auth.store.entity;

import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;

import javax.persistence.*;

/**
 * @author Andrei Varabyeu
 */
@Entity
@Table(name = "auth_config")
public class AuthConfig {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "ldap_config_id")
	private LdapConfig ldap;

	@ManyToOne
	@JoinColumn(name = "active_directory_config_id")
	private ActiveDirectoryConfig activeDirectory;

	public AuthConfig() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LdapConfig getLdap() {
		return ldap;
	}

	public void setLdap(LdapConfig ldap) {
		this.ldap = ldap;
	}

	public ActiveDirectoryConfig getActiveDirectory() {
		return activeDirectory;
	}

	public void setActiveDirectory(ActiveDirectoryConfig activeDirectory) {
		this.activeDirectory = activeDirectory;
	}

	@Override
	public String toString() {
		return "AuthConfig{" + "id='" + id + '\'' + ", ldap=" + ldap + ", activeDirectory=" + activeDirectory + '}';
	}
}
