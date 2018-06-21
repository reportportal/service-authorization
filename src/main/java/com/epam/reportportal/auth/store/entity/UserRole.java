package com.epam.reportportal.auth.store.entity;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * UserRole representation<br>
 * Role has more rights than the following one. So, Administrator is more
 * privileged than User.
 *
 * @author Andrei Varabyeu
 */
public enum UserRole {

	USER,
	ADMINISTRATOR;

	public static final String ROLE_PREFIX = "ROLE_";

	public static Optional<UserRole> findByName(String name) {
		return Arrays.stream(UserRole.values()).filter(role -> role.name().equals(name)).findAny();
	}

	public static Optional<UserRole> findByAuthority(String name) {
		if (Strings.isNullOrEmpty(name)) {
			return Optional.empty();
		}
		return findByName(StringUtils.substringAfter(name, ROLE_PREFIX));
	}

	public String getAuthority() {
		return "ROLE_" + this.name();
	}

}