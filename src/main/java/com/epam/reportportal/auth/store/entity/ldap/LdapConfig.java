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

import com.epam.reportportal.auth.validation.LdapSequenceProvider;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.persistence.*;

/**
 * LDAP auth config
 *
 * @author Andrei Varabyeu
 */

@GroupSequenceProvider(LdapSequenceProvider.class)
@Entity
@Table(name = "ldap_config", schema = "public")
public class LdapConfig extends AbstractLdapIntegration {

	@Column(name = "user_dn_pattern", length = 256)
	private String userDnPattern;

	@Column(name = "user_search_filter", length = 256)
	private String userSearchFilter;

	@Column(name = "group_search_base", length = 256)
	private String groupSearchBase;

	@Column(name = "group_search_filter", length = 256)
	private String groupSearchFilter;

	@Enumerated(EnumType.STRING)
	private PasswordEncoderType passwordEncoderType;

	@Column(name = "password_attributes", length = 256)
	private String passwordAttribute;

	@Column(name = "manager_dn", length = 256)
	private String managerDn;

	@Column(name = "manager_password", length = 256)
	private String managerPassword;

	public String getUserDnPattern() {
		return userDnPattern;
	}

	public void setUserDnPattern(String userDnPattern) {
		this.userDnPattern = userDnPattern;
	}

	public String getGroupSearchBase() {
		return groupSearchBase;
	}

	public void setGroupSearchBase(String groupSearchBase) {
		this.groupSearchBase = groupSearchBase;
	}

	public PasswordEncoderType getPasswordEncoderType() {
		return passwordEncoderType;
	}

	public void setPasswordEncoderType(PasswordEncoderType passwordEncoderType) {
		this.passwordEncoderType = passwordEncoderType;
	}

	public String getPasswordAttribute() {
		return passwordAttribute;
	}

	public void setPasswordAttribute(String passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}

	public String getManagerDn() {
		return managerDn;
	}

	public void setManagerDn(String managerDn) {
		this.managerDn = managerDn;
	}

	public String getManagerPassword() {
		return managerPassword;
	}

	public void setManagerPassword(String managerPassword) {
		this.managerPassword = managerPassword;
	}

	public String getUserSearchFilter() {
		return userSearchFilter;
	}

	public void setUserSearchFilter(String userSearchFilter) {
		this.userSearchFilter = userSearchFilter;
	}

	public String getGroupSearchFilter() {
		return groupSearchFilter;
	}

	public void setGroupSearchFilter(String groupSearchFilter) {
		this.groupSearchFilter = groupSearchFilter;
	}

}
