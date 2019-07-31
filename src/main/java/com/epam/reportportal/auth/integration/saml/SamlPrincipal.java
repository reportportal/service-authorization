/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.integration.saml;

import org.springframework.security.saml.saml2.authentication.SubjectPrincipal;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * Represents principal extracted from SAML response and used for authentication
 *
 * @author Yevgeniy Svalukhin
 */
public class SamlPrincipal extends SubjectPrincipal<SamlPrincipal> implements Serializable, Principal {

	private static final long serialVersionUID = -341083232L;

	private String format;
	private String value;

	public String getFormat() {
		return format;
	}

	public SamlPrincipal setFormat(String format) {
		this.format = format;
		return this;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public SamlPrincipal setValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public String getName() {
		return getValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SamlPrincipal samlPrincipal = (SamlPrincipal) o;
		return Objects.equals(format, samlPrincipal.format) && Objects.equals(value, samlPrincipal.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(format, value);
	}
}
