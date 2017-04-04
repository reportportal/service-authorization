/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
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
package com.epam.reportportal.auth.oauth;

import com.epam.ta.reportportal.database.entity.settings.OAuth2LoginDetails;

/**
 * @author Andrei Varabyeu
 */
abstract public class OAuthProvider {

	/**
	 * Auth provider name
	 */
	private String name;

	/**
	 * HTML code of button
	 */
	private String button;

	public OAuthProvider(String name, String button) {
		this.name = name;
		this.button = button;
	}

	/**
	 * Applies default settings
	 *
	 * @param details OAuth configuration
	 */
	abstract public void applyDefaults(OAuth2LoginDetails details);

	public String getName() {
		return name;
	}

	public String getButton() {
		return button;
	}

	public String buildPath(String basePath) {
		return basePath + (basePath.endsWith("/") ? "" : "/") + this.name;
	}

}
