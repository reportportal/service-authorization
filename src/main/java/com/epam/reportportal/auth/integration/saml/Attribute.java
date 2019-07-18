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

import org.springframework.security.saml.saml2.attribute.AttributeNameFormat;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Represents attributes extracted from SAML response message
 *
 * @author Yevgeniy Svalukhin
 */
public class Attribute implements Serializable {
	private static final long serialVersionUID = -182983902349882L;

	private String name;
	private String friendlyName;
	private List<Object> values = new LinkedList<>();
	private String nameFormat = AttributeNameFormat.UNSPECIFIED.toString();
	private boolean required;

	public String getName() {
		return name;
	}

	public Attribute setName(String name) {
		this.name = name;
		return this;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public Attribute setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
		return this;
	}

	public List<Object> getValues() {
		return values;
	}

	public Attribute setValues(List<Object> values) {
		this.values.addAll(values);
		return this;
	}

	public String getNameFormat() {
		return nameFormat;
	}

	public Attribute setNameFormat(String nameFormat) {
		this.nameFormat = nameFormat;
		return this;
	}

	public boolean isRequired() {
		return required;
	}

	public Attribute setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Attribute attribute = (Attribute) o;
		return required == attribute.required && Objects.equals(name, attribute.name) && Objects.equals(nameFormat, attribute.nameFormat);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, nameFormat, required);
	}
}
